package com.ruijie.authzengine.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 页面资源写入请求。
 */
@Data
@Schema(description = "页面资源写入请求")
public class PageResourceRequest {

    @NotBlank(message = "租户标识不能为空")
    @Schema(description = "租户标识", example = "T001")
    private String tenantId;

    @NotBlank(message = "应用标识不能为空")
    @Schema(description = "应用标识", example = "CRM")
    private String appCode;

    @NotBlank(message = "页面编码不能为空")
    @Schema(description = "页面编码", example = "PAGE-CONTRACT-LIST")
    private String pageCode;

    @NotBlank(message = "页面名称不能为空")
    @Schema(description = "页面名称", example = "合同列表")
    private String pageName;

    @Schema(description = "菜单编码", example = "MENU-CONTRACT")
    private String menuCode;

    @Schema(description = "页面路径", example = "/contracts/list")
    private String pagePath;

    @Schema(description = "状态", example = "ENABLED")
    private String status;

    @Schema(description = "显示排序号，数值越小越靠前，默认 0", example = "0")
    private Integer sortOrder;

}