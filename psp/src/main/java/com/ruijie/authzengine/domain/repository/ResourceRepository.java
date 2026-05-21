package com.ruijie.authzengine.domain.repository;

import com.ruijie.authzengine.domain.model.governance.PageResult;
import com.ruijie.authzengine.domain.model.governance.resource.SysResComponent;
import com.ruijie.authzengine.domain.model.governance.resource.SysResApi;
import com.ruijie.authzengine.domain.model.governance.resource.SysResMenu;
import com.ruijie.authzengine.domain.model.governance.resource.SysResPage;
import java.util.Collections;
import java.util.List;

/**
 * 治理资源目录仓储。
 */
public interface ResourceRepository {

    default SysResMenu saveMenu(SysResMenu sysResMenu) {
        throw new UnsupportedOperationException("当前仓储未实现菜单写入能力");
    }

    default PageResult<SysResMenu> pageMenus(String tenantId, String appCode, String keyword, int pageNo, int pageSize) {
        return emptyPage(pageNo, pageSize);
    }

    default SysResMenu findMenu(String tenantId, String appCode, String menuCode) {
        return null;
    }

    /**
     * 查询当前应用下全部菜单定义，供运行时装配菜单树。
     *
     * @param tenantId 租户标识
     * @param appCode 应用标识
     * @return 菜单列表，空时返回空列表
     */
    default List<SysResMenu> listMenus(String tenantId, String appCode) {
        return Collections.emptyList();
    }

    default void deleteMenu(String tenantId, String appCode, String menuCode) {
        throw new UnsupportedOperationException("当前仓储未实现菜单删除能力");
    }

    default boolean hasMenuReference(String tenantId, String appCode, String menuCode) {
        return false;
    }

    default SysResPage savePage(SysResPage sysResPage) {
        throw new UnsupportedOperationException("当前仓储未实现页面写入能力");
    }

    default PageResult<SysResPage> pagePages(String tenantId, String appCode, String keyword, int pageNo, int pageSize) {
        return emptyPage(pageNo, pageSize);
    }

    default SysResPage findPage(String tenantId, String appCode, String pageCode) {
        return null;
    }

    /**
     * 按主键查询页面资源。
     */
    default SysResPage findPageById(String tenantId, String appCode, Long pageId) {
        return null;
    }

    /**
     * 查询当前应用下全部页面定义，供派生菜单链路装配页面 -> 菜单映射。
     */
    default List<SysResPage> listPages(String tenantId, String appCode) {
        return Collections.emptyList();
    }

    default void deletePage(String tenantId, String appCode, String pageCode) {
        throw new UnsupportedOperationException("当前仓储未实现页面删除能力");
    }

    default boolean hasPageReference(String tenantId, String appCode, String pageCode) {
        return false;
    }

    default SysResComponent saveComponent(SysResComponent sysResComponent) {
        throw new UnsupportedOperationException("当前仓储未实现组件写入能力");
    }

    default PageResult<SysResComponent> pageComponents(String tenantId, String appCode, String keyword, int pageNo, int pageSize) {
        return emptyPage(pageNo, pageSize);
    }

    default SysResComponent findComponent(String tenantId, String appCode, String componentCode) {
        return null;
    }

    /**
     * 按主键查询组件资源。
     */
    default SysResComponent findComponentById(String tenantId, String appCode, Long componentId) {
        return null;
    }

    /**
     * 查询当前应用下全部组件定义，供 user-context/Q3 在纯派生模式下装配组件全集。
     */
    default List<SysResComponent> listComponents(String tenantId, String appCode) {
        return Collections.emptyList();
    }

    default void deleteComponent(String tenantId, String appCode, String componentCode) {
        throw new UnsupportedOperationException("当前仓储未实现组件删除能力");
    }

    default boolean hasComponentReference(String tenantId, String appCode, String componentCode) {
        return false;
    }

    /**
     * 保存或更新 API 资源目录。
     *
     * @param sysResApi API 资源定义
     * @return 已保存定义
     */
    default SysResApi saveApi(SysResApi sysResApi) {
        throw new UnsupportedOperationException("当前仓储未实现 API 资源写入能力");
    }

    default PageResult<SysResApi> pageApis(String tenantId, String appCode, String keyword, int pageNo, int pageSize) {
        return pageOf(listApis(tenantId, appCode), pageNo, pageSize);
    }

    default SysResApi findApi(String tenantId, String appCode, String apiCode) {
        return null;
    }

    /**
     * 按主键查询 API 资源。
     */
    default SysResApi findApiById(String tenantId, String appCode, Long apiId) {
        return null;
    }

    default void deleteApi(String tenantId, String appCode, String apiCode) {
        throw new UnsupportedOperationException("当前仓储未实现 API 资源删除能力");
    }

    default boolean hasApiReference(String tenantId, String appCode, String apiCode) {
        return false;
    }

    /**
     * 查询 API 资源目录。
     *
     * @param tenantId 租户标识
     * @param appCode 应用标识
     * @return API 资源列表
     */
    default List<SysResApi> listApis(String tenantId, String appCode) {
        return Collections.emptyList();
    }

    static <T> PageResult<T> emptyPage(int pageNo, int pageSize) {
        return PageResult.<T>builder()
            .pageNo(pageNo)
            .pageSize(pageSize)
            .total(0)
            .records(Collections.emptyList())
            .build();
    }

    static <T> PageResult<T> pageOf(List<T> records, int pageNo, int pageSize) {
        List<T> safeRecords = records == null ? Collections.<T>emptyList() : records;
        return PageResult.<T>builder()
            .pageNo(pageNo)
            .pageSize(pageSize)
            .total(safeRecords.size())
            .records(safeRecords)
            .build();
    }
}