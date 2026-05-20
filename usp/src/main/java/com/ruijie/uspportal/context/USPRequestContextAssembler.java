package com.ruijie.uspportal.context;

import com.ruijie.uspportal.host.dto.AppContextResponse;
import com.ruijie.uspportal.host.dto.CurrentContextResponse;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * USP 请求上下文装配器。
 *
 * <p>负责将内部统一上下文对象转换为对外接口返回的上下文响应模型。</p>
 */
@Component
public class USPRequestContextAssembler {

    /**
         * 将统一请求上下文转换为当前上下文响应。
         *
         * @param context 统一请求上下文
         * @return 当前上下文响应对象
     */
    public CurrentContextResponse toCurrentContextResponse(USPRequestContext context) {
        CurrentUserSnapshot user = context == null ? null : context.getUser();
        TenantSnapshot tenant = context == null ? null : context.getTenant();
        OrgSnapshot org = context == null ? null : context.getOrg();
        PortalRuntimeContext portal = context == null ? null : context.getPortal();
        return CurrentContextResponse.builder()
                .user(CurrentContextResponse.User.builder()
                        .userId(user == null ? null : user.getUserId())
                        .loginName(user == null ? null : user.getLoginName())
                        .displayName(user == null ? null : user.getDisplayName())
                        .authMode(user == null ? null : user.getAuthMode())
                        .build())
                .tenant(CurrentContextResponse.Tenant.builder()
                        .tenantId(tenant == null ? null : tenant.getTenantId())
                        .tenantCode(tenant == null ? null : tenant.getTenantCode())
                        .tenantName(tenant == null ? null : tenant.getTenantName())
                        .status(tenant == null ? null : tenant.getTenantStatus())
                        .build())
                .org(CurrentContextResponse.Org.builder()
                        .orgCode(org == null ? null : org.getOrgCode())
                        .orgName(org == null ? null : org.getOrgName())
                        .build())
                .portal(CurrentContextResponse.Portal.builder()
                        .homeRoute(portal == null ? null : portal.getHomeRoute())
                        .defaultAppCode(portal == null ? null : portal.getDefaultAppCode())
                        .menuVersion(portal == null ? null : portal.getMenuVersion())
                        .build())
                .build();
    }

    /**
         * 将统一请求上下文转换为应用上下文响应。
         *
         * @param context 统一请求上下文
         * @return 应用上下文响应对象
     */
    public AppContextResponse toAppContextResponse(USPRequestContext context) {
        CurrentUserSnapshot user = context == null ? null : context.getUser();
        TenantSnapshot tenant = context == null ? null : context.getTenant();
        OrgSnapshot org = context == null ? null : context.getOrg();
        AppRuntimeContext app = context == null ? null : context.getApp();
        return AppContextResponse.builder()
                .appId(app == null ? null : app.getAppId())
                .appCode(app == null ? null : app.getAppCode())
                .appName(app == null ? null : app.getAppName())
                .tenantId(tenant == null ? null : tenant.getTenantId())
                .tenantCode(tenant == null ? null : tenant.getTenantCode())
                .orgCode(org == null ? null : org.getOrgCode())
                .userId(user == null ? null : user.getUserId())
                .displayName(user == null ? null : user.getDisplayName())
                .entryUrl(app == null ? null : app.getEntryUrl())
                .routePrefix(app == null ? null : app.getRoutePrefix())
                .accessMode(app == null ? null : app.getAccessMode())
                .openMode(app == null ? null : app.getOpenMode())
                .contextHeaders(app == null || app.getContextHeaders() == null ? Collections.emptyMap() : app.getContextHeaders())
                .build();
    }
}
