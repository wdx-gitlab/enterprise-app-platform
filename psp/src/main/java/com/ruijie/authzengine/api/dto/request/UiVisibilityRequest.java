package com.ruijie.authzengine.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * UI 元素可见性批量查询请求 DTO（Q3）。
 */
@Data
@Schema(description = "UI 元素可见性批量查询请求")
public class UiVisibilityRequest {

    @NotBlank(message = "租户标识不能为空")
    @Schema(description = "租户标识", example = "T001")
    private String tenantId;

    @NotBlank(message = "应用标识不能为空")
    @Schema(description = "应用标识", example = "CRM")
    private String appCode;

    @NotBlank(message = "主体 ID 不能为空")
    @Schema(description = "主体标识", example = "U10001")
    private String subjectId;

    @Schema(description = "主体类型，默认 SUB_USER", example = "SUB_USER")
    private String subjectModel = "SUB_USER";

    @NotEmpty(message = "组件编码列表不能为空")
    @Schema(description = "要查询可见性的 UI 组件编码列表（对应权限项 resId）",
        example = "[\"BTN_CONTRACT_APPROVE\", \"BTN_CONTRACT_DELETE\", \"BTN_CONTRACT_EXPORT\"]")
    private List<String> componentCodes;
}

