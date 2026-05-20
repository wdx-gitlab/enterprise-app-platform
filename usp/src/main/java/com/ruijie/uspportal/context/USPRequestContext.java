package com.ruijie.uspportal.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * USP 统一请求上下文。
 *
 * <p>聚合当前用户、租户、组织、门户运行态和应用运行态等信息，供宿主接口统一消费。</p>
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class USPRequestContext {

    private CurrentUserSnapshot user;

    private TenantSnapshot tenant;

    private OrgSnapshot org;

    private PortalRuntimeContext portal;

    private AppRuntimeContext app;
}
