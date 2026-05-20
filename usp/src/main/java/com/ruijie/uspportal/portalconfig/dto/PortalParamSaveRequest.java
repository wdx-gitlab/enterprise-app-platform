package com.ruijie.uspportal.portalconfig.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class PortalParamSaveRequest {

    @NotBlank(message = "请输入参数键")
    private String paramKey;

    @NotBlank(message = "请输入参数名称")
    private String paramName;

    @NotBlank(message = "请输入参数分组")
    private String paramGroup;

    @NotBlank(message = "请选择值类型")
    private String valueType;

    private String paramValue;

    private String defaultValue;

    private String description;
}
