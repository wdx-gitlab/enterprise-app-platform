package com.ruijie.uspportal.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {

    private String unionSessionTicket;

    private String loginName;

    private String displayName;

    private String tenantCode;

    private Boolean admin;
}
