package com.ruijie.uspportal.host.dto;

import lombok.Data;

@Data
/**
 * NavigationResolveTarget 请求对象。
 */
public class NavigationResolveTargetRequest {

    private String menuId;

    private String tenantCode;

    private String appCode;
}
