package com.ruijie.authzengine.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 组件资源写入请求。
 */
@Data
@Schema(description = "组件资源写入请求")
public class ComponentResourceRequest {

    @NotBlank(message = "租户标识不能为空")
    @Schema(description = "租户标识", example = "T001")
    private String tenantId;

    @NotBlank(message = "应用标识不能为空")
    @Schema(description = "应用标识", example = "CRM")
    private String appCode;

    @NotBlank(message = "组件编码不能为空")
    @Schema(description = "组件编码", example = "BTN-CONTRACT-EXPORT")
    private String componentCode;

    @NotBlank(message = "组件名称不能为空")
    @Schema(description = "组件名称", example = "导出按钮")
    private String componentName;

    @Schema(description = "页面编码", example = "PAGE-CONTRACT-LIST")
    private String pageCode;

    @Schema(description = "组件类型", example = "BUTTON")
    private String componentType;

    @Schema(description = "状态", example = "ENABLED")
    private String status;
}