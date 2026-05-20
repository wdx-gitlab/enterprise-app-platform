package com.ruijie.uspportal.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
/**
 * LocalAccount 响应对象。
 */
public class LocalAccountResponse {

    private Long id;

    private String loginName;

    private String displayName;

    private String email;

    private String phoneNum;

    private String tenantCode;

    private String status;

    private Boolean admin;

    private Boolean forceResetPassword;

    private String passwordEncodeType;

    private String createdBy;

    private LocalDateTime createdTime;

    private String updatedBy;

    private LocalDateTime updatedTime;

    private LocalDateTime lastLoginTime;
}
