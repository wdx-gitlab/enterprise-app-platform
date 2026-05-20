package com.ruijie.uspportal.auth.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 本地账号保存请求。
 */
@Data
public class LocalAccountSaveRequest {

    @NotBlank(message = "登录账号不能为空")
    @Size(max = 64, message = "登录账号长度不能超过64")
    private String loginName;

    @NotBlank(message = "显示名称不能为空")
    @Size(max = 128, message = "显示名称长度不能超过128")
    private String displayName;

    @Size(max = 128, message = "邮箱长度不能超过128")
    private String email;

    @Size(max = 32, message = "手机号长度不能超过32")
    private String phoneNum;

    @Size(max = 64, message = "租户编码长度不能超过64")
    private String tenantCode;

    @Size(max = 32, message = "状态长度不能超过32")
    private String status;

    private Boolean admin;

    private Boolean forceResetPassword;

    @Size(max = 128, message = "密码长度不能超过128")
    private String password;
}
