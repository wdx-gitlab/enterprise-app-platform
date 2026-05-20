package com.ruijie.uspportal.host.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
/**
 * NavigationResolveTarget 响应对象。
 */
public class NavigationResolveTargetResponse {

    private String targetType;

    private String targetPath;

    private String openMode;

    private String appCode;

    private String menuId;

    private RouteMeta routeMeta;

    @Data
    @Builder
    /**
     * RouteMeta 类。
     */
    public static class RouteMeta {

        private String title;

        private Boolean keepAlive;
    }
}
