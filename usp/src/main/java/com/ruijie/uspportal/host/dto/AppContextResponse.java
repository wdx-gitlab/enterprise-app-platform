package com.ruijie.uspportal.host.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
/**
 * AppContext 响应对象。
 */
public class AppContextResponse {

    private Long appId;

    private String appCode;

    private String appName;

    private Long tenantId;

    private String tenantCode;

    private String orgCode;

    private String userId;

    private String displayName;

    private String entryUrl;

    private String routePrefix;

    private String accessMode;

    private String openMode;

    private Map<String, String> contextHeaders;
}
