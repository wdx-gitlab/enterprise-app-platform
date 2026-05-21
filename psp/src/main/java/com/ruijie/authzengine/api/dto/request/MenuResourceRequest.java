package com.ruijie.authzengine.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 菜单项写入请求。
 */
@Data
@Schema(description = "菜单项写入请求")
public class MenuResourceRequest {

    @NotBlank(message = "租户标识不能为空")
    @Schema(description = "租户标识", example = "T001")
    private String tenantId;

    @Schema(description = "租户编码，可选，缺省时与 tenantId 相同", example = "RUIJIE")
    private String tenantCode;

    @NotBlank(message = "应用标识不能为空")
    @Schema(description = "应用标识", example = "CRM")
    private String appCode;

    @Schema(description = "所属应用 ID，可选")
    private Long appId;

    @NotBlank(message = "菜单编码不能为空")
    @Schema(description = "菜单编码", example = "MENU-CONTRACT")
    private String menuCode;

    @NotBlank(message = "菜单名称不能为空")
    @Schema(description = "菜单名称", example = "合同管理")
    private String menuName;

    @Schema(description = "菜单图标")
    private String menuIcon;

    @Schema(description = "菜单类型：DIRECTORY / MENU / LINK", example = "MENU")
    private String menuType;

    @Schema(description = "路由路径", example = "/contracts")
    private String routePath;

    @Schema(description = "跳转链接（LINK 类型使用）")
    private String targetUrl;

    @Schema(description = "父菜单编码，根菜单留空", example = "MENU-ROOT")
    private String parentMenuCode;

    @Schema(description = "排序号，数值越小越靠前，默认 0", example = "0")
    private Integer sortNo;

    @Schema(description = "PSP 权限编码")
    private String permissionCode;

    @Schema(description = "显示表达式")
    private String visibleExpression;

    @Schema(description = "发布状态：DRAFT / PUBLISHED", example = "DRAFT")
    private String publishStatus;

    @Schema(description = "状态", example = "ENABLED")
    private String status;
}