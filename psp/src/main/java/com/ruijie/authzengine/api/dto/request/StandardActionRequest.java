package com.ruijie.authzengine.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * 标准动作写入请求。
 */
@Data
@Schema(description = "标准动作写入请求")
public class StandardActionRequest {

    @NotBlank(message = "租户标识不能为空")
    @Schema(description = "租户标识", example = "__GLOBAL__")
    private String tenantId;

    @NotBlank(message = "动作编码不能为空")
    @Schema(description = "动作编码", example = "READ")
    private String actCode;

    @NotBlank(message = "动作名称不能为空")
    @Schema(description = "动作名称", example = "查看")
    private String actName;

    @NotBlank(message = "动作类型不能为空")
    @Schema(description = "动作类型", example = "STANDARD")
    private String actType;

    @Schema(description = "资源分类", example = "API")
    private String resCategory;

    @NotNull(message = "风险等级不能为空")
    @Schema(description = "风险等级", example = "1")
    private Integer riskLevel;
}