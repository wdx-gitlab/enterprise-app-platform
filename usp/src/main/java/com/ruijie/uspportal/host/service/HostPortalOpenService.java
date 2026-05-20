package com.ruijie.uspportal.host.service;

import com.ruijie.uspportal.host.dto.AppCatalogItemResponse;
import com.ruijie.uspportal.host.dto.AppContextResponse;
import com.ruijie.uspportal.host.dto.CurrentContextResponse;
import com.ruijie.uspportal.host.dto.FeatureFlagEvaluateBatchRequest;
import com.ruijie.uspportal.host.dto.FeatureFlagEvaluateBatchResponse;
import com.ruijie.uspportal.host.dto.HostMenuItemResponse;
import com.ruijie.uspportal.host.dto.HostMenuQueryRequest;
import com.ruijie.uspportal.host.dto.HostPortalParamResponse;
import com.ruijie.uspportal.host.dto.IntegrationCapabilitiesResponse;
import com.ruijie.uspportal.host.dto.NavigationResolveTargetRequest;
import com.ruijie.uspportal.host.dto.NavigationResolveTargetResponse;
import com.ruijie.uspportal.navigation.dto.NavigationNode;
import com.ruijie.uspportal.navigation.entity.MenuItemEntity;
import com.ruijie.uspportal.navigation.service.NavigationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 宿主服务直接注入使用的 Portal 对外能力入口。
 *
 * <p>当 {@code usp-portal} 作为模块被其他宿主后端引入时，宿主业务代码可以直接注入该类，
 * 通过方法调用拿到 Portal 侧信息，而不需要再回环走 HTTP 接口。</p>
 */
@Service
public class HostPortalOpenService {

    private static final String NOT_IMPLEMENTED_MESSAGE = "该宿主直连能力暂未实现，请后续按文档能力逐步放开";

    private final NavigationService navigationService;

    @Autowired
    public HostPortalOpenService(NavigationService navigationService) {
        this.navigationService = navigationService;
    }

    /**
     * 查询 Portal 当前配置的菜单列表。
     *
     * <p>这是当前优先开放给宿主直接复用的能力。宿主可以按租户、应用编码和发布状态做本地过滤。</p>
     *
     * @param request 菜单查询条件
     * @return 过滤后的菜单列表
     */
    public List<HostMenuItemResponse> listAllMenus(HostMenuQueryRequest request) {
        HostMenuQueryRequest actualRequest = request == null ? HostMenuQueryRequest.builder().build() : request;
        return navigationService.listMenus().stream()
                .filter(item -> matchTenantCode(item, actualRequest.getTenantCode()))
                .filter(item -> matchAppCode(item, actualRequest.getAppCode()))
                .filter(item -> matchPublishStatus(item, actualRequest.getPublishedOnly()))
                .sorted(Comparator
                        .comparing(HostPortalOpenService::safeTenantCode)
                        .thenComparing(item -> item.getSortNo() == null ? Integer.MAX_VALUE : item.getSortNo())
                        .thenComparing(item -> item.getId() == null ? Long.MAX_VALUE : item.getId()))
                .map(this::toHostMenuItem)
                .collect(Collectors.toList());
    }

    /**
     * 占位：宿主侧直接获取可见导航树。
     */
    public List<NavigationNode> navigationTree(String tenantCode, String appCode) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MESSAGE);
    }

    /**
     * 占位：宿主侧直接获取应用目录。
     */
    public List<AppCatalogItemResponse> appCatalog(String tenantCode) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MESSAGE);
    }

    /**
     * 占位：宿主侧直接解析菜单跳转目标。
     */
    public NavigationResolveTargetResponse resolveTarget(NavigationResolveTargetRequest request) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MESSAGE);
    }

    /**
     * 占位：宿主侧直接获取门户参数。
     */
    public List<HostPortalParamResponse> listPortalParams(String group, String scope) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MESSAGE);
    }

    /**
     * 占位：宿主侧直接评估功能开关。
     */
    public FeatureFlagEvaluateBatchResponse evaluateFlags(FeatureFlagEvaluateBatchRequest request) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MESSAGE);
    }

    /**
     * 占位：宿主侧直接获取应用上下文。
     */
    public AppContextResponse appContext(String appCode, String tenantCode, String unionSessionTicket) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MESSAGE);
    }

    /**
     * 占位：宿主侧直接获取当前上下文。
     */
    public CurrentContextResponse currentContext() {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MESSAGE);
    }

    /**
     * 占位：宿主侧直接获取模块能力说明。
     */
    public IntegrationCapabilitiesResponse capabilities() {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MESSAGE);
    }

    private HostMenuItemResponse toHostMenuItem(MenuItemEntity item) {
        return HostMenuItemResponse.builder()
                .id(item.getId())
                .tenantId(item.getTenantId())
                .tenantCode(item.getTenantCode())
                .menuCode(item.getMenuCode())
                .menuName(item.getMenuName())
                .menuIcon(item.getMenuIcon())
                .menuType(item.getMenuType())
                .appCode(item.getAppCode())
                .appId(item.getAppId())
                .routePath(item.getRoutePath())
                .targetUrl(item.getTargetUrl())
                .parentId(item.getParentId())
                .sortNo(item.getSortNo())
                .treeLevel(item.getTreeLevel())
                .treePath(item.getTreePath())
                .permissionCode(item.getPermissionCode())
                .visibleExpression(item.getVisibleExpression())
                .publishStatus(item.getPublishStatus())
                .status(item.getStatus())
                .createdBy(item.getCreatedBy())
                .createdTime(item.getCreatedTime())
                .updatedBy(item.getUpdatedBy())
                .updatedTime(item.getUpdatedTime())
                .build();
    }

    private boolean matchTenantCode(MenuItemEntity item, String tenantCode) {
        return !StringUtils.hasText(tenantCode) || tenantCode.trim().equalsIgnoreCase(item.getTenantCode());
    }

    private boolean matchAppCode(MenuItemEntity item, String appCode) {
        return !StringUtils.hasText(appCode) || appCode.trim().equalsIgnoreCase(item.getAppCode());
    }

    private boolean matchPublishStatus(MenuItemEntity item, Boolean publishedOnly) {
        return !Boolean.TRUE.equals(publishedOnly) || "PUBLISHED".equalsIgnoreCase(item.getPublishStatus());
    }

    private static String safeTenantCode(MenuItemEntity item) {
        return item.getTenantCode() == null ? "" : item.getTenantCode();
    }
}