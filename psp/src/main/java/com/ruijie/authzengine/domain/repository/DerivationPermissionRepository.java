package com.ruijie.authzengine.domain.repository;

import com.ruijie.authzengine.domain.model.governance.PageResult;
import com.ruijie.authzengine.domain.model.governance.permission.ResourceDerivationPermission;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 派生权限查询仓储。
 *
 * <p>查询侧只关心"某类资源是否存在派生绑定"以及"当前主体拥有的权限项能派生出哪些资源编码"，
 * 不直接暴露 {@code authz_res_derivation_perm.res_id} 的多态主键细节，便于后续由持久化层自行完成
 * {@code res_id -> resourceCode} 的 join/映射。
 */
public interface DerivationPermissionRepository {

    /**
     * 分页查询派生权限关联，支持按 resType / keyword 过滤。
     *
     * @param tenantId  租户标识
     * @param appCode   应用标识
     * @param resType   资源类型过滤（可为空则查全部）
     * @param keyword   模糊匹配（可为空）
     * @param pageNo    页码
     * @param pageSize  每页大小
     * @return 分页结果
     */
    default PageResult<ResourceDerivationPermission> pageBindings(String tenantId, String appCode,
                                                                   String resType, String keyword,
                                                                   int pageNo, int pageSize) {
        return PageResult.<ResourceDerivationPermission>builder()
            .pageNo(pageNo).pageSize(pageSize).total(0).records(Collections.emptyList()).build();
    }

    /**
     * 保存或更新派生权限关联。
     */
    default ResourceDerivationPermission saveBinding(ResourceDerivationPermission binding) {
        throw new UnsupportedOperationException("当前仓储未实现派生权限写入能力");
    }

    /**
     * 查询单条派生权限关联。
     */
    default ResourceDerivationPermission findBinding(String tenantId, String appCode, Long bindingId) {
        return null;
    }

    /**
     * 查询指定资源下的全部派生绑定。
     */
    default List<ResourceDerivationPermission> listBindingsByResource(String tenantId, String appCode,
                                                                     String resType, Long resId) {
        return Collections.emptyList();
    }

    /**
     * 删除单条派生权限关联。
     */
    default void deleteBinding(String tenantId, String appCode, Long bindingId) {
        throw new UnsupportedOperationException("当前仓储未实现派生权限删除能力");
    }

    /**
     * 按资源删除全部派生权限关联。
     */
    default void deleteBindingsByResource(String tenantId, String appCode, String resType, Long resId) {
    }

    /**
     * 按权限项删除全部派生权限关联。
     */
    default void deleteBindingsByPermissionItemId(String tenantId, String appCode, Long permItemId) {
    }

    /**
     * 判断当前应用在指定资源类型下是否已存在派生绑定记录。
     */
    default boolean hasDerivationBindings(String tenantId, String appCode, String resType) {
        return false;
    }

    /**
     * 查询指定资源类型下全部已配置的资源编码集合。
     */
    default List<String> findAllDerivedResourceCodes(String tenantId, String appCode, String resType) {
        return Collections.emptyList();
    }

    /**
     * 根据主体已拥有的权限项集合，查询可正向展开出的资源编码集合。
     */
    default List<String> findDerivedResourceCodesByPermItemIds(String tenantId, String appCode,
                                                               String resType, Collection<Long> permItemIds) {
        return Collections.emptyList();
    }
}