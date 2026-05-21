package com.ruijie.authzengine.domain.repository;

import com.ruijie.authzengine.domain.model.governance.PageResult;
import com.ruijie.authzengine.domain.model.governance.permission.AuthPermissionItem;
import java.util.Collections;
import java.util.List;

/**
 * 权限项治理仓储骨架。
 */
public interface PermissionRepository {

    AuthPermissionItem savePermissionItem(AuthPermissionItem permissionItem);

    PageResult<AuthPermissionItem> pagePermissionItems(String tenantId, String appCode, String keyword, int pageNo, int pageSize);

    /**
     * 分页查询权限项，支持按资源模型编码和资源标识过滤。
     *
     * @param tenantId 租户标识
     * @param appCode 应用标识
     * @param keyword 关键词，可为空
     * @param resModelCode 资源模型编码，可为空
     * @param resId 资源标识，可为空
     * @param pageNo 页码
     * @param pageSize 每页条数
     * @return 权限项分页结果
     */
    default PageResult<AuthPermissionItem> pagePermissionItems(
            String tenantId,
            String appCode,
            String keyword,
            String resModelCode,
            String resId,
            int pageNo,
            int pageSize) {
        return pagePermissionItems(tenantId, appCode, keyword, pageNo, pageSize);
    }

    AuthPermissionItem findPermissionItem(String tenantId, String appCode, String permCode);

    void deletePermissionItem(String tenantId, String appCode, String permCode);

    boolean hasPermissionItemReference(String tenantId, String appCode, String permCode);

    /**
     * 按主键 ID 列表批量查询权限项。
     *
     * <p>Q1/Q4 在获取授权记录后，通过 permItemId 批量反查权限项，以获取 permCode 列表。
     *
     * @param tenantId    租户标识
     * @param appCode     应用标识
     * @param permItemIds 权限项 ID 列表
     * @return 权限项列表，空时返回空列表
     */
    default List<AuthPermissionItem> findPermissionItemsByIds(
            String tenantId, String appCode, List<Long> permItemIds) {
        return Collections.emptyList();
    }

    /**
     * 按资源模型编码查询权限项列表。
     *
     * <p>Q2 可访问资源列表查询时使用：先按 resModelCode 过滤出该资源类型下所有权限项，
     * 再与授权记录取交集，得到主体有权访问的资源编码列表。
     *
     * @param tenantId     租户标识
     * @param appCode      应用标识
     * @param resModelCode 资源模型编码，如 RES_UI_MENU、RES_API
     * @return 权限项列表，空时返回空列表
     */
    default List<AuthPermissionItem> findPermissionItemsByResModelCode(
            String tenantId, String appCode, String resModelCode) {
        return Collections.emptyList();
    }
}