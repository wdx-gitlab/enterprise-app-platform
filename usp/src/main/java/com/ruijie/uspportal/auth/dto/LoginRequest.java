package com.ruijie.uspportal.auth.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class LoginRequest {

    @NotBlank(message = "请输入登录账号")
    private String loginName;

    @NotBlank(message = "请输入登录密码")
    private String password;

    private String tenantCode;
}
