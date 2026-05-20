package com.ruijie.uspportal.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CurrentUserResponse {

    private String userId;

    private String loginName;

    private String displayName;

    private String tenantCode;

    private String sessionId;

    private String authMode;

    private Boolean admin;
}
