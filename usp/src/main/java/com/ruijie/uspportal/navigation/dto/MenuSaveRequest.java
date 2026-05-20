package com.ruijie.uspportal.navigation.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class MenuSaveRequest {

    @NotBlank(message = "请输入菜单编码")
    private String menuCode;

    @NotBlank(message = "请输入菜单名称")
    private String menuName;

    @NotBlank(message = "请选择菜单类型")
    private String menuType;

    private Long appId;

    private Long parentId;

    private String routePath;

    private String targetUrl;

    private String menuIcon;

    private Integer sortNo;

    private String permissionCode;

    private String tenantCode;
}
