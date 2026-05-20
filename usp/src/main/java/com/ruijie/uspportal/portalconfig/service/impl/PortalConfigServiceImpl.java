package com.ruijie.uspportal.portalconfig.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.ruijie.uspportal.cache.CacheKeys;
import com.ruijie.uspportal.cache.PortalCacheService;
import com.ruijie.uspportal.common.BusinessException;
import com.ruijie.uspportal.eventbus.service.impl.ConfigFlagEventPublisher;
import com.ruijie.uspportal.portalconfig.dto.FeatureFlagEvaluateRequest;
import com.ruijie.uspportal.portalconfig.dto.FeatureFlagRuleSaveRequest;
import com.ruijie.uspportal.portalconfig.dto.FeatureFlagSaveRequest;
import com.ruijie.uspportal.portalconfig.dto.PortalParamSaveRequest;
import com.ruijie.uspportal.portalconfig.entity.FeatureFlagEntity;
import com.ruijie.uspportal.portalconfig.entity.FeatureFlagRuleEntity;
import com.ruijie.uspportal.portalconfig.entity.PortalParamEntity;
import com.ruijie.uspportal.portalconfig.entity.PortalParamHistoryEntity;
import com.ruijie.uspportal.portalconfig.mapper.FeatureFlagMapper;
import com.ruijie.uspportal.portalconfig.mapper.FeatureFlagRuleMapper;
import com.ruijie.uspportal.portalconfig.mapper.PortalConfigMapper;
import com.ruijie.uspportal.portalconfig.mapper.PortalParamHistoryMapper;
import com.ruijie.uspportal.portalconfig.service.PortalConfigService;
import com.ruijie.uspportal.security.CurrentUserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 门户配置服务默认实现。
 *
 * <p>负责门户参数、灰度开关与规则的持久化、缓存刷新以及配置变更事件发布。</p>
 */
@Service
public class PortalConfigServiceImpl implements PortalConfigService {

    private final PortalConfigMapper portalConfigMapper;

    private final PortalParamHistoryMapper portalParamHistoryMapper;

    private final FeatureFlagMapper featureFlagMapper;

    private final FeatureFlagRuleMapper featureFlagRuleMapper;

    private final PortalCacheService portalCacheService;

    private final ConfigFlagEventPublisher configFlagEventPublisher;

    @Autowired
    public PortalConfigServiceImpl(PortalConfigMapper portalConfigMapper,
                                   PortalParamHistoryMapper portalParamHistoryMapper,
                                   FeatureFlagMapper featureFlagMapper,
                                   FeatureFlagRuleMapper featureFlagRuleMapper,
                                   PortalCacheService portalCacheService,
                                   ConfigFlagEventPublisher configFlagEventPublisher) {
        this.portalConfigMapper = portalConfigMapper;
        this.portalParamHistoryMapper = portalParamHistoryMapper;
        this.featureFlagMapper = featureFlagMapper;
        this.featureFlagRuleMapper = featureFlagRuleMapper;
        this.portalCacheService = portalCacheService;
        this.configFlagEventPublisher = configFlagEventPublisher;
    }

    /**
     * 查询门户参数列表。
     *
     * @return 参数集合
     */
    @Override
    public List<PortalParamEntity> listParams() {
        QueryWrapper<PortalParamEntity> wrapper = new QueryWrapper<>();
        wrapper.orderByAsc("param_group", "param_key");
        return portalConfigMapper.selectList(wrapper);
    }

    /**
     * 创建门户参数。
     *
     * @param request 参数保存请求
     * @return 创建后的参数信息
     */
    @Override
    public PortalParamEntity saveParam(PortalParamSaveRequest request) {
        PortalParamEntity entity = new PortalParamEntity();
        apply(entity, request);
        entity.setStatus("ENABLED");
        portalConfigMapper.insert(entity);
        writeHistory(entity.getId(), null, entity.getParamValue(), "CREATE");
        refreshParam(entity.getParamKey(), entity);
        return entity;
    }

    /**
     * 更新指定门户参数。
     *
     * @param paramKey 参数键
     * @param request 参数保存请求
     * @return 更新后的参数信息
     */
    @Override
    public PortalParamEntity updateParam(String paramKey, PortalParamSaveRequest request) {
        PortalParamEntity entity = getParam(paramKey);
        String oldValue = entity.getParamValue();
        apply(entity, request);
        portalConfigMapper.updateById(entity);
        writeHistory(entity.getId(), oldValue, entity.getParamValue(), "UPDATE");
        refreshParam(entity.getParamKey(), entity);
        return getParam(paramKey);
    }

    /**
     * 查询指定参数的变更历史。
     *
     * @param paramKey 参数键
     * @return 参数历史记录
     */
    @Override
    public List<PortalParamHistoryEntity> listHistories(String paramKey) {
        PortalParamEntity entity = getParam(paramKey);
        QueryWrapper<PortalParamHistoryEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("param_id", entity.getId()).orderByDesc("changed_time");
        return portalParamHistoryMapper.selectList(wrapper);
    }

    /**
     * 查询功能开关列表。
     *
     * @return 功能开关集合
     */
    @Override
    public List<FeatureFlagEntity> listFlags() {
        QueryWrapper<FeatureFlagEntity> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("updated_time");
        return featureFlagMapper.selectList(wrapper);
    }

    /**
     * 创建功能开关及其规则。
     *
     * @param request 功能开关保存请求
     * @return 创建后的功能开关信息
     */
    @Override
    public FeatureFlagEntity saveFlag(FeatureFlagSaveRequest request) {
        FeatureFlagEntity entity = new FeatureFlagEntity();
        entity.setFlagKey(request.getFlagKey());
        entity.setFlagName(request.getFlagName());
        entity.setDescription(request.getDescription());
        entity.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "DISABLED");
        featureFlagMapper.insert(entity);
        replaceRules(entity.getId(), request.getRules());
        refreshFlag(entity.getFlagKey(), entity);
        return entity;
    }

    /**
     * 更新功能开关及其规则。
     *
     * @param id 功能开关主键
     * @param request 功能开关保存请求
     * @return 更新后的功能开关信息
     */
    @Override
    public FeatureFlagEntity updateFlag(Long id, FeatureFlagSaveRequest request) {
        FeatureFlagEntity entity = featureFlagMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("功能开关不存在");
        }
        entity.setFlagKey(request.getFlagKey());
        entity.setFlagName(request.getFlagName());
        entity.setDescription(request.getDescription());
        entity.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : entity.getStatus());
        featureFlagMapper.updateById(entity);
        replaceRules(id, request.getRules());
        refreshFlag(entity.getFlagKey(), entity);
        return featureFlagMapper.selectById(id);
    }

    /**
     * 查询指定功能开关的规则列表。
     *
     * @param flagId 功能开关主键
     * @return 灰度规则集合
     */
    @Override
    public List<FeatureFlagRuleEntity> listRules(Long flagId) {
        QueryWrapper<FeatureFlagRuleEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("flag_id", flagId).orderByAsc("priority_no");
        return featureFlagRuleMapper.selectList(wrapper);
    }

    /**
     * 按上下文评估功能开关是否启用。
     *
     * @param request 评估请求
     * @return 评估结果
     */
    @Override
    public Map<String, Object> evaluate(FeatureFlagEvaluateRequest request) {
        QueryWrapper<FeatureFlagEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("flag_key", request.getFlagKey()).last("LIMIT 1");
        FeatureFlagEntity entity = featureFlagMapper.selectOne(wrapper);
        if (entity == null) {
            throw new BusinessException("功能开关不存在");
        }
        boolean enabled = "ENABLED".equalsIgnoreCase(entity.getStatus());
        List<FeatureFlagRuleEntity> rules = listRules(entity.getId());
        if (!rules.isEmpty() && request.getContext() != null) {
            enabled = matchRule(rules, request.getContext());
        }
        Map<String, Object> result = new HashMap<>();
        result.put("flagKey", entity.getFlagKey());
        result.put("enabled", enabled);
        return result;
    }

    /**
     * 按顺序匹配灰度规则。
     *
     * @param rules 灰度规则列表
     * @param context 上下文字段
     * @return 是否命中规则
     */
    private boolean matchRule(List<FeatureFlagRuleEntity> rules, Map<String, String> context) {
        for (FeatureFlagRuleEntity rule : rules) {
            String value = context.get(rule.getRuleType().toLowerCase());
            if (!StringUtils.hasText(value)) {
                continue;
            }
            if ("EQ".equalsIgnoreCase(rule.getRuleOperator()) && value.equalsIgnoreCase(rule.getRuleValue())) {
                return true;
            }
            if ("IN".equalsIgnoreCase(rule.getRuleOperator()) && rule.getRuleValue().contains(value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 查询指定参数实体。
     *
     * @param paramKey 参数键
     * @return 参数实体
     */
    private PortalParamEntity getParam(String paramKey) {
        QueryWrapper<PortalParamEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("param_key", paramKey).last("LIMIT 1");
        PortalParamEntity entity = portalConfigMapper.selectOne(wrapper);
        if (entity == null) {
            throw new BusinessException("门户参数不存在");
        }
        return entity;
    }

    /**
     * 将参数保存请求写入参数实体。
     *
     * @param entity 参数实体
     * @param request 参数保存请求
     */
    private void apply(PortalParamEntity entity, PortalParamSaveRequest request) {
        entity.setParamKey(request.getParamKey());
        entity.setParamName(request.getParamName());
        entity.setParamGroup(request.getParamGroup());
        entity.setValueType(request.getValueType());
        entity.setParamValue(request.getParamValue());
        entity.setDefaultValue(request.getDefaultValue());
        entity.setDescription(request.getDescription());
    }

    /**
     * 写入参数变更历史记录。
     *
     * @param paramId 参数主键
     * @param oldValue 旧值
     * @param newValue 新值
     * @param remark 变更说明
     */
    private void writeHistory(Long paramId, String oldValue, String newValue, String remark) {
        PortalParamHistoryEntity history = new PortalParamHistoryEntity();
        history.setParamId(paramId);
        history.setOldValue(oldValue);
        history.setNewValue(newValue);
        history.setChangedBy(CurrentUserContext.get() == null ? "SYSTEM" : CurrentUserContext.get().getLoginName());
        history.setRemark(remark);
        portalParamHistoryMapper.insert(history);
    }

    /**
     * 全量替换指定功能开关下的规则。
     *
     * @param flagId 功能开关主键
     * @param requests 规则保存请求列表
     */
    private void replaceRules(Long flagId, List<FeatureFlagRuleSaveRequest> requests) {
        UpdateWrapper<FeatureFlagRuleEntity> deleteWrapper = new UpdateWrapper<>();
        deleteWrapper.eq("flag_id", flagId);
        featureFlagRuleMapper.delete(deleteWrapper);
        if (requests == null) {
            return;
        }
        for (FeatureFlagRuleSaveRequest request : requests) {
            FeatureFlagRuleEntity rule = new FeatureFlagRuleEntity();
            rule.setFlagId(flagId);
            rule.setRuleType(request.getRuleType());
            rule.setRuleOperator(request.getRuleOperator());
            rule.setRuleValue(request.getRuleValue());
            rule.setPriorityNo(request.getPriorityNo() == null ? 0 : request.getPriorityNo());
            rule.setStatus("ENABLED");
            featureFlagRuleMapper.insert(rule);
        }
    }

    /**
     * 刷新参数缓存并发布配置变更事件。
     *
     * @param paramKey 参数键
     * @param entity 参数实体
     */
    private void refreshParam(String paramKey, PortalParamEntity entity) {
        portalCacheService.put(CacheKeys.PORTAL_PARAM_PREFIX + paramKey, entity);
        configFlagEventPublisher.publishConfigChanged("config.PARAM_UPDATED", entity);
    }

    /**
     * 刷新功能开关缓存并发布配置变更事件。
     *
     * @param flagKey 功能开关键
     * @param entity 功能开关实体
     */
    private void refreshFlag(String flagKey, FeatureFlagEntity entity) {
        portalCacheService.put(CacheKeys.FEATURE_FLAG_PREFIX + flagKey, entity);
        configFlagEventPublisher.publishConfigChanged("config.FLAG_UPDATED", entity);
    }
}
