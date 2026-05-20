package com.ruijie.uspportal.appregistry.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class AppSaveRequest {

    @NotBlank(message = "请输入应用编码")
    private String appCode;

    @NotBlank(message = "请输入应用名称")
    private String appName;

    @NotBlank(message = "请输入入口地址")
    private String entryUrl;

    @NotBlank(message = "请选择应用类型")
    private String appType;

    private String routePrefix;

    private String appIcon;

    private String appDesc;
}
