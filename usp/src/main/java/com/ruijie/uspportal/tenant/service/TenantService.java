package com.ruijie.uspportal.tenant.service;

import com.ruijie.uspportal.tenant.dto.TenantSaveRequest;
import com.ruijie.uspportal.tenant.entity.TenantEntity;

import java.util.List;

/**
 * 租户服务。
 *
 * <p>定义租户资料维护与租户状态流转的核心操作。</p>
 */
public interface TenantService {

    /**
     * 查询租户列表。
     *
     * @return 租户集合
     */
    List<TenantEntity> list();

    /**
     * 查询指定租户详情。
     *
     * @param id 租户主键
     * @return 租户详情
     */
    TenantEntity get(Long id);

    /**
     * 创建租户。
     *
     * @param request 租户保存请求
     * @return 创建后的租户信息
     */
    TenantEntity create(TenantSaveRequest request);

    /**
     * 更新指定租户。
     *
     * @param id 租户主键
     * @param request 租户保存请求
     * @return 更新后的租户信息
     */
    TenantEntity update(Long id, TenantSaveRequest request);

    /**
     * 激活指定租户。
     *
     * @param id 租户主键
     */
    void activate(Long id);

    /**
     * 暂停指定租户。
     *
     * @param id 租户主键
     */
    void suspend(Long id);

    /**
     * 恢复指定租户。
     *
     * @param id 租户主键
     */
    void resume(Long id);
}
