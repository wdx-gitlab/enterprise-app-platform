package com.ruijie.authzengine.application.sdk.impl;

import com.ruijie.authzengine.application.sdk.AuthzQueryService;
import com.ruijie.authzengine.application.sdk.model.AccessibleResourceQuery;
import com.ruijie.authzengine.application.sdk.model.AccessibleResourceResult;
import com.ruijie.authzengine.application.sdk.model.PermissionSnapshotResult;
import com.ruijie.authzengine.application.sdk.model.SubjectScopedQuery;
import com.ruijie.authzengine.application.sdk.model.UiVisibilityQuery;
import com.ruijie.authzengine.application.sdk.model.UiVisibilityResult;
import com.ruijie.authzengine.application.sdk.model.UserContextResult;
import com.ruijie.authzengine.application.service.AuthzQueryAppService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.Assert;

/**
 * 默认权限查询服务实现。
 */
public class DefaultAuthzQueryService implements AuthzQueryService {

    private final AuthzQueryAppService authzQueryAppService;

    public DefaultAuthzQueryService(AuthzQueryAppService authzQueryAppService) {
        this.authzQueryAppService = authzQueryAppService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PermissionSnapshotResult queryPermissionSnapshot(SubjectScopedQuery query) {
        validateSubjectScopedQuery(query);
        long start = System.currentTimeMillis();
        List<String> permCodes = authzQueryAppService.queryPermissionSnapshot(
            query.getTenantId(),
            query.getAppCode(),
            query.getSubjectId(),
            query.getSubjectModel()
        );
        return PermissionSnapshotResult.builder()
            .permCodes(permCodes == null ? Collections.<String>emptyList() : permCodes)
            .evalTimeMs(System.currentTimeMillis() - start)
            .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AccessibleResourceResult queryAccessibleResources(AccessibleResourceQuery query) {
        validateAccessibleResourceQuery(query);
        long start = System.currentTimeMillis();
        List<String> resourceCodes = authzQueryAppService.queryAccessibleResources(
            query.getTenantId(),
            query.getAppCode(),
            query.getSubjectId(),
            query.getSubjectModel(),
            query.getResourceType()
        );
        return AccessibleResourceResult.builder()
            .resourceType(query.getResourceType())
            .resourceCodes(resourceCodes == null ? Collections.<String>emptyList() : resourceCodes)
            .evalTimeMs(System.currentTimeMillis() - start)
            .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UiVisibilityResult queryUiVisibility(UiVisibilityQuery query) {
        validateUiVisibilityQuery(query);
        long start = System.currentTimeMillis();
        Map<String, Boolean> visibleMap = authzQueryAppService.queryUiVisibility(
            query.getTenantId(),
            query.getAppCode(),
            query.getSubjectId(),
            query.getSubjectModel(),
            query.getComponentCodes()
        );
        return UiVisibilityResult.builder()
            .visibility(toVisibilityItems(visibleMap))
            .evalTimeMs(System.currentTimeMillis() - start)
            .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserContextResult queryUserContext(SubjectScopedQuery query) {
        validateSubjectScopedQuery(query);
        AuthzQueryAppService.UserContextResult result = authzQueryAppService.queryUserContext(
            query.getTenantId(),
            query.getAppCode(),
            query.getSubjectId(),
            query.getSubjectModel()
        );
        return UserContextResult.builder()
            .subjectId(query.getSubjectId())
            .subjectModel(query.getSubjectModel())
            .permCodes(result == null || result.permCodes == null ? Collections.<String>emptyList() : result.permCodes)
            .accessibleResources(result == null || result.accessibleResources == null
                ? Collections.<String, List<String>>emptyMap() : result.accessibleResources)
            .menuTree(result == null ? Collections.<UserContextResult.MenuTreeNode>emptyList() : toMenuTree(result.menuTree))
            .visibility(result == null ? Collections.<String, UiVisibilityResult.VisibilityItem>emptyMap() : toVisibilityItems(result.visibility))
            .evalTimeMs(result == null ? 0L : result.evalTimeMs)
            .build();
    }

    private void validateSubjectScopedQuery(SubjectScopedQuery query) {
        Assert.notNull(query, "query 不能为空");
        Assert.hasText(query.getTenantId(), "tenantId 不能为空");
        Assert.hasText(query.getAppCode(), "appCode 不能为空");
        Assert.hasText(query.getSubjectId(), "subjectId 不能为空");
        Assert.hasText(query.getSubjectModel(), "subjectModel 不能为空");
    }

    private void validateAccessibleResourceQuery(AccessibleResourceQuery query) {
        Assert.notNull(query, "query 不能为空");
        Assert.hasText(query.getTenantId(), "tenantId 不能为空");
        Assert.hasText(query.getAppCode(), "appCode 不能为空");
        Assert.hasText(query.getSubjectId(), "subjectId 不能为空");
        Assert.hasText(query.getSubjectModel(), "subjectModel 不能为空");
        Assert.hasText(query.getResourceType(), "resourceType 不能为空");
    }

    private void validateUiVisibilityQuery(UiVisibilityQuery query) {
        Assert.notNull(query, "query 不能为空");
        Assert.hasText(query.getTenantId(), "tenantId 不能为空");
        Assert.hasText(query.getAppCode(), "appCode 不能为空");
        Assert.hasText(query.getSubjectId(), "subjectId 不能为空");
        Assert.hasText(query.getSubjectModel(), "subjectModel 不能为空");
        Assert.isTrue(query.getComponentCodes() != null && !query.getComponentCodes().isEmpty(),
            "componentCodes 不能为空");
    }

    private Map<String, UiVisibilityResult.VisibilityItem> toVisibilityItems(Map<String, Boolean> visibleMap) {
        if (visibleMap == null || visibleMap.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, UiVisibilityResult.VisibilityItem> visibility = new LinkedHashMap<>();
        visibleMap.forEach((code, visible) -> visibility.put(code, UiVisibilityResult.VisibilityItem.builder()
            .visible(Boolean.TRUE.equals(visible))
            .disabled(false)
            .readonly(false)
            .build()));
        return visibility;
    }

    private List<UserContextResult.MenuTreeNode> toMenuTree(List<AuthzQueryAppService.MenuTreeNodeResult> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return Collections.emptyList();
        }
        List<UserContextResult.MenuTreeNode> result = new ArrayList<>();
        for (AuthzQueryAppService.MenuTreeNodeResult node : nodes) {
            result.add(UserContextResult.MenuTreeNode.builder()
                .menuCode(node.menuCode)
                .menuName(node.menuName)
                .routePath(node.routePath)
                .children(toMenuTree(node.children))
                .build());
        }
        return result;
    }
}