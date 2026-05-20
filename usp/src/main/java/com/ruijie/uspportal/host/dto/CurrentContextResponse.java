package com.ruijie.uspportal.host.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
/**
 * CurrentContext 响应对象。
 */
public class CurrentContextResponse {

    private User user;

    private Tenant tenant;

    private Org org;

    private Portal portal;

    @Data
    @Builder
    /**
     * User 类。
     */
    public static class User {

        private String userId;

        private String loginName;

        private String displayName;

        private String authMode;
    }

    @Data
    @Builder
    /**
     * Tenant 类。
     */
    public static class Tenant {

        private Long tenantId;

        private String tenantCode;

        private String tenantName;

        private String status;
    }

    @Data
    @Builder
    /**
     * Org 类。
     */
    public static class Org {

        private String orgCode;

        private String orgName;
    }

    @Data
    @Builder
    /**
     * Portal 类。
     */
    public static class Portal {

        private String homeRoute;

        private String defaultAppCode;

        private String menuVersion;
    }
}
