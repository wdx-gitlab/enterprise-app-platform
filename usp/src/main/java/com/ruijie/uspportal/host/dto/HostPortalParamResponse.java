package com.ruijie.uspportal.host.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
/**
 * HostPortalParam 响应对象。
 */
public class HostPortalParamResponse {

    private Long id;

    private String paramKey;

    private String paramName;

    private String paramValue;

    private String paramGroup;

    private String valueType;

    private Boolean frontendReadable;

    private String defaultValue;

    private String status;

    private String description;
}
