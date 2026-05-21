package com.ruijie.authzengine.application.sdk;

import com.ruijie.authzengine.application.sdk.model.AccessibleResourceQuery;
import com.ruijie.authzengine.application.sdk.model.AccessibleResourceResult;
import com.ruijie.authzengine.application.sdk.model.PermissionSnapshotResult;
import com.ruijie.authzengine.application.sdk.model.SubjectScopedQuery;
import com.ruijie.authzengine.application.sdk.model.UiVisibilityQuery;
import com.ruijie.authzengine.application.sdk.model.UiVisibilityResult;
import com.ruijie.authzengine.application.sdk.model.UserContextResult;

/**
 * 面向业务系统的权限查询服务。
 *
 * <p>用于回答“我能做什么 / 能看到什么”，不替代最终执行前的精确鉴权。
 */
public interface AuthzQueryService {

    /**
     * 查询主体权限快照。
     *
     * @param query 主体查询条件
     * @return 权限项快照
     */
    PermissionSnapshotResult queryPermissionSnapshot(SubjectScopedQuery query);

    /**
     * 查询主体对某资源类型的可访问资源列表。
     *
     * @param query 资源查询条件
     * @return 可访问资源结果
     */
    AccessibleResourceResult queryAccessibleResources(AccessibleResourceQuery query);

    /**
     * 批量查询 UI 组件可见性。
     *
     * @param query 组件可见性查询条件
     * @return 可见性结果
     */
    UiVisibilityResult queryUiVisibility(UiVisibilityQuery query);

    /**
     * 查询用户权限上下文合集。
     *
     * @param query 主体查询条件
     * @return 用户权限上下文
     */
    UserContextResult queryUserContext(SubjectScopedQuery query);
}