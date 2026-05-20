package com.ruijie.uspportal.portalconfig.service;

import com.ruijie.uspportal.portalconfig.dto.FeatureFlagEvaluateRequest;
import com.ruijie.uspportal.portalconfig.dto.FeatureFlagSaveRequest;
import com.ruijie.uspportal.portalconfig.dto.PortalParamSaveRequest;
import com.ruijie.uspportal.portalconfig.entity.FeatureFlagEntity;
import com.ruijie.uspportal.portalconfig.entity.FeatureFlagRuleEntity;
import com.ruijie.uspportal.portalconfig.entity.PortalParamEntity;
import com.ruijie.uspportal.portalconfig.entity.PortalParamHistoryEntity;

import java.util.List;
import java.util.Map;

/**
 * 门户配置服务。
 *
 * <p>定义门户参数、灰度开关、规则与评估能力的统一服务接口。</p>
 */
public interface PortalConfigService {

    /**
     * 查询门户参数列表。
     *
     * @return 参数集合
     */
    List<PortalParamEntity> listParams();

    /**
     * 创建门户参数。
     *
     * @param request 参数保存请求
     * @return 创建后的参数信息
     */
    PortalParamEntity saveParam(PortalParamSaveRequest request);

    /**
     * 更新指定门户参数。
     *
     * @param paramKey 参数键
     * @param request 参数保存请求
     * @return 更新后的参数信息
     */
    PortalParamEntity updateParam(String paramKey, PortalParamSaveRequest request);

    /**
     * 查询指定门户参数的变更历史。
     *
     * @param paramKey 参数键
     * @return 参数历史列表
     */
    List<PortalParamHistoryEntity> listHistories(String paramKey);

    /**
     * 查询功能开关列表。
     *
     * @return 功能开关集合
     */
    List<FeatureFlagEntity> listFlags();

    /**
     * 创建功能开关。
     *
     * @param request 功能开关保存请求
     * @return 创建后的功能开关信息
     */
    FeatureFlagEntity saveFlag(FeatureFlagSaveRequest request);

    /**
     * 更新指定功能开关。
     *
     * @param id 功能开关主键
     * @param request 功能开关保存请求
     * @return 更新后的功能开关信息
     */
    FeatureFlagEntity updateFlag(Long id, FeatureFlagSaveRequest request);

    /**
     * 查询指定功能开关的规则列表。
     *
     * @param flagId 功能开关主键
     * @return 灰度规则集合
     */
    List<FeatureFlagRuleEntity> listRules(Long flagId);

    /**
     * 评估给定上下文下的功能开关结果。
     *
     * @param request 评估请求
     * @return 评估结果
     */
    Map<String, Object> evaluate(FeatureFlagEvaluateRequest request);
}
