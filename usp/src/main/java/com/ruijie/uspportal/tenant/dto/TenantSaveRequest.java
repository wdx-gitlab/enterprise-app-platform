package com.ruijie.uspportal.tenant.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class TenantSaveRequest {

    @NotBlank(message = "请输入租户编码")
    private String tenantCode;

    @NotBlank(message = "请输入租户名称")
    private String tenantName;

    @NotBlank(message = "请选择租户类型")
    private String tenantType;

    private String capabilityScope;

    private String remark;
}
