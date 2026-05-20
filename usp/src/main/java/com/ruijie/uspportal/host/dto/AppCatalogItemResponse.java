package com.ruijie.uspportal.host.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
/**
 * AppCatalogItem 响应对象。
 */
public class AppCatalogItemResponse {

    private String appCode;

    private String appName;

    private String icon;

    private String entryUrl;

    private String routePrefix;

    private String appType;

    private String openMode;

    private Boolean visible;

    private String publishStatus;
}
