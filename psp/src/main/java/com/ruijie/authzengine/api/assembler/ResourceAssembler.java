package com.ruijie.authzengine.api.assembler;

import com.ruijie.authzengine.api.dto.request.ApiResourceRequest;
import com.ruijie.authzengine.api.dto.request.ComponentResourceRequest;
import com.ruijie.authzengine.api.dto.request.MenuResourceRequest;
import com.ruijie.authzengine.api.dto.request.PageResourceRequest;
import com.ruijie.authzengine.api.dto.response.ApiResourceResponse;
import com.ruijie.authzengine.api.dto.response.ComponentResourceResponse;
import com.ruijie.authzengine.api.dto.response.PageResponse;
import com.ruijie.authzengine.api.dto.response.MenuResourceResponse;
import com.ruijie.authzengine.api.dto.response.PageResourceResponse;
import com.ruijie.authzengine.domain.model.governance.PageResult;
import com.ruijie.authzengine.domain.model.governance.resource.SysResApi;
import com.ruijie.authzengine.domain.model.governance.resource.SysResComponent;
import com.ruijie.authzengine.domain.model.governance.resource.SysResMenu;
import com.ruijie.authzengine.domain.model.governance.resource.SysResPage;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 资源目录装配器。
 */
@Component
public class ResourceAssembler {

    public SysResMenu toDefinition(MenuResourceRequest request) {
        return SysResMenu.builder()
            .tenantId(request.getTenantId())
            .tenantCode(request.getTenantCode())
            .appCode(request.getAppCode())
            .appId(request.getAppId())
            .menuCode(request.getMenuCode())
            .menuName(request.getMenuName())
            .menuIcon(request.getMenuIcon())
            .menuType(request.getMenuType())
            .routePath(request.getRoutePath())
            .targetUrl(request.getTargetUrl())
            .parentMenuCode(request.getParentMenuCode())
            .sortNo(request.getSortNo())
            .permissionCode(request.getPermissionCode())
            .visibleExpression(request.getVisibleExpression())
            .publishStatus(request.getPublishStatus())
            .status(request.getStatus())
            .build();
    }

    public SysResPage toDefinition(PageResourceRequest request) {
        return SysResPage.builder()
            .tenantId(request.getTenantId())
            .appCode(request.getAppCode())
            .pageCode(request.getPageCode())
            .pageName(request.getPageName())
            .menuCode(request.getMenuCode())
            .pagePath(request.getPagePath())
            .status(request.getStatus())
            .sortOrder(request.getSortOrder())
            .build();
    }

    public SysResComponent toDefinition(ComponentResourceRequest request) {
        return SysResComponent.builder()
            .tenantId(request.getTenantId())
            .appCode(request.getAppCode())
            .componentCode(request.getComponentCode())
            .componentName(request.getComponentName())
            .pageCode(request.getPageCode())
            .componentType(request.getComponentType())
            .status(request.getStatus())
            .build();
    }

    public SysResApi toDefinition(ApiResourceRequest request) {
        return SysResApi.builder()
            .tenantId(request.getTenantId())
            .appCode(request.getAppCode())
            .apiCode(request.getApiCode())
            .apiName(StringUtils.hasText(request.getApiName()) ? request.getApiName() : request.getApiCode())
            .httpMethod(request.getHttpMethod())
            .uriPattern(request.getUriPattern())
            .status(request.getStatus())
            .build();
    }

    public MenuResourceResponse toResponse(SysResMenu definition) {
        return MenuResourceResponse.builder()
            .id(definition.getId())
            .tenantId(definition.getTenantId())
            .tenantCode(definition.getTenantCode())
            .appCode(definition.getAppCode())
            .appId(definition.getAppId())
            .menuCode(definition.getMenuCode())
            .menuName(definition.getMenuName())
            .menuIcon(definition.getMenuIcon())
            .menuType(definition.getMenuType())
            .routePath(definition.getRoutePath())
            .targetUrl(definition.getTargetUrl())
            .parentMenuCode(definition.getParentMenuCode())
            .sortNo(definition.getSortNo())
            .treeLevel(definition.getTreeLevel())
            .treePath(definition.getTreePath())
            .permissionCode(definition.getPermissionCode())
            .visibleExpression(definition.getVisibleExpression())
            .publishStatus(definition.getPublishStatus())
            .status(definition.getStatus())
            .attributes(definition.getAttributes())
            .build();
    }

    public PageResourceResponse toResponse(SysResPage definition) {
        return PageResourceResponse.builder()
            .id(definition.getId())
            .tenantId(definition.getTenantId())
            .appCode(definition.getAppCode())
            .pageCode(definition.getPageCode())
            .pageName(definition.getPageName())
            .menuCode(definition.getMenuCode())
            .pagePath(definition.getPagePath())
            .status(definition.getStatus())
            .sortOrder(definition.getSortOrder())
            .attributes(definition.getAttributes())
            .build();
    }

    public ComponentResourceResponse toResponse(SysResComponent definition) {
        return ComponentResourceResponse.builder()
            .id(definition.getId())
            .tenantId(definition.getTenantId())
            .appCode(definition.getAppCode())
            .componentCode(definition.getComponentCode())
            .componentName(definition.getComponentName())
            .pageCode(definition.getPageCode())
            .componentType(definition.getComponentType())
            .status(definition.getStatus())
            .attributes(definition.getAttributes())
            .build();
    }

    public ApiResourceResponse toResponse(SysResApi definition) {
        return ApiResourceResponse.builder()
            .id(definition.getId())
            .tenantId(definition.getTenantId())
            .appCode(definition.getAppCode())
            .apiCode(definition.getApiCode())
            .apiName(definition.getApiName())
            .httpMethod(definition.getHttpMethod())
            .uriPattern(definition.getUriPattern())
            .status(definition.getStatus())
            .attributes(definition.getAttributes())
            .build();
    }

    /**
     * 转换分页结果。
     *
     * @param pageResult 领域分页结果
     * @param mapper 记录转换函数
     * @param <T> 领域记录类型
     * @param <R> 响应记录类型
     * @return 接口分页响应
     */
    public <T, R> PageResponse<R> toPageResponse(PageResult<T> pageResult, Function<T, R> mapper) {
        List<R> records = pageResult.getRecords().stream().map(mapper).collect(Collectors.toList());
        return PageResponse.<R>builder()
            .pageNo(pageResult.getPageNo())
            .pageSize(pageResult.getPageSize())
            .total(pageResult.getTotal())
            .records(records)
            .build();
    }
}