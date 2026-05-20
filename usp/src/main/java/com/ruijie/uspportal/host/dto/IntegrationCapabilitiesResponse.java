package com.ruijie.uspportal.host.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
/**
 * IntegrationCapabilities 响应对象。
 */
public class IntegrationCapabilitiesResponse {

    private String module;

    private String version;

    private List<String> authModes;

    private Boolean supportsFeatureFlags;

    private Boolean supportsNavigationTree;

    private Boolean supportsAppCatalog;

    private Boolean supportsContextSnapshot;

    private Boolean supportsEventReplay;
}
