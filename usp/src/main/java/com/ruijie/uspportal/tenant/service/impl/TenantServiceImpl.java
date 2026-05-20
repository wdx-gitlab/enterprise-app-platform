package com.ruijie.uspportal.tenant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruijie.uspportal.cache.CacheKeys;
import com.ruijie.uspportal.cache.PortalCacheService;
import com.ruijie.uspportal.common.BusinessException;
import com.ruijie.uspportal.eventbus.service.PortalEventPublisher;
import com.ruijie.uspportal.security.LoginSessionService;
import com.ruijie.uspportal.tenant.dto.TenantSaveRequest;
import com.ruijie.uspportal.tenant.entity.TenantEntity;
import com.ruijie.uspportal.tenant.mapper.TenantMapper;
import com.ruijie.uspportal.tenant.service.TenantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 租户服务默认实现。
 *
 * <p>负责租户数据维护、状态缓存刷新、登录会话失效以及租户事件发布。</p>
 */
@Service
public class TenantServiceImpl implements TenantService {

    private final TenantMapper tenantMapper;

    private final PortalCacheService portalCacheService;

    private final PortalEventPublisher portalEventPublisher;

    private final LoginSessionService loginSessionService;

    @Autowired
    public TenantServiceImpl(TenantMapper tenantMapper,
                             PortalCacheService portalCacheService,
                             PortalEventPublisher portalEventPublisher,
                             LoginSessionService loginSessionService) {
        this.tenantMapper = tenantMapper;
        this.portalCacheService = portalCacheService;
        this.portalEventPublisher = portalEventPublisher;
        this.loginSessionService = loginSessionService;
    }

    /**
     * 查询租户列表。
     *
     * @return 租户集合
     */
    @Override
    public List<TenantEntity> list() {
        QueryWrapper<TenantEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("is_deleted", 0).orderByDesc("updated_time");
        return tenantMapper.selectList(wrapper);
    }

    /**
     * 查询指定租户详情。
     *
     * @param id 租户主键
     * @return 租户详情
     */
    @Override
    public TenantEntity get(Long id) {
        TenantEntity tenant = tenantMapper.selectById(id);
        if (tenant == null || tenant.getDeleted() != null && tenant.getDeleted() == 1) {
            throw new BusinessException("租户不存在");
        }
        return tenant;
    }

    /**
     * 创建租户。
     *
     * @param request 租户保存请求
     * @return 创建后的租户信息
     */
    @Override
    public TenantEntity create(TenantSaveRequest request) {
        QueryWrapper<TenantEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("tenant_code", request.getTenantCode()).eq("is_deleted", 0).last("LIMIT 1");
        if (tenantMapper.selectOne(wrapper) != null) {
            throw new BusinessException("租户编码已存在");
        }
        TenantEntity entity = new TenantEntity();
        apply(entity, request);
        entity.setStatus("DRAFT");
        tenantMapper.insert(entity);
        portalEventPublisher.publish("tenant.CREATED", entity);
        return entity;
    }

    /**
     * 更新指定租户。
     *
     * @param id 租户主键
     * @param request 租户保存请求
     * @return 更新后的租户信息
     */
    @Override
    public TenantEntity update(Long id, TenantSaveRequest request) {
        TenantEntity entity = get(id);
        apply(entity, request);
        tenantMapper.updateById(entity);
        evictStatus(entity.getTenantCode());
        return get(id);
    }

    /**
     * 激活指定租户。
     *
     * @param id 租户主键
     */
    @Override
    public void activate(Long id) {
        changeStatus(id, "ACTIVE");
    }

    /**
     * 暂停指定租户并使租户下会话失效。
     *
     * @param id 租户主键
     */
    @Override
    public void suspend(Long id) {
        TenantEntity tenant = changeStatus(id, "SUSPENDED");
        loginSessionService.invalidateByTenantCode(tenant.getTenantCode(), LocalDateTime.now(), "TENANT_SUSPENDED");
    }

    /**
     * 恢复指定租户为激活状态。
     *
     * @param id 租户主键
     */
    @Override
    public void resume(Long id) {
        changeStatus(id, "ACTIVE");
    }

    /**
     * 统一切换租户状态并同步缓存与事件。
     *
     * @param id 租户主键
     * @param status 目标状态
     * @return 更新后的租户实体
     */
    private TenantEntity changeStatus(Long id, String status) {
        TenantEntity entity = get(id);
        entity.setStatus(status);
        if ("ACTIVE".equals(status)) {
            entity.setActivatedTime(LocalDateTime.now());
        }
        tenantMapper.updateById(entity);
        portalCacheService.put(CacheKeys.TENANT_STATUS_PREFIX + entity.getTenantCode(), status);
        portalEventPublisher.publish("tenant." + status, entity);
        return entity;
    }

    /**
     * 将租户保存请求写入租户实体。
     *
     * @param entity 租户实体
     * @param request 租户保存请求
     */
    private void apply(TenantEntity entity, TenantSaveRequest request) {
        entity.setTenantCode(request.getTenantCode());
        entity.setTenantName(request.getTenantName());
        entity.setTenantType(request.getTenantType());
        entity.setCapabilityScope(StringUtils.hasText(request.getCapabilityScope()) ? request.getCapabilityScope().trim() : null);
        entity.setRemark(request.getRemark());
    }

    /**
     * 清理指定租户的状态缓存。
     *
     * @param tenantCode 租户编码
     */
    private void evictStatus(String tenantCode) {
        portalCacheService.evict(CacheKeys.TENANT_STATUS_PREFIX + tenantCode);
    }
}
