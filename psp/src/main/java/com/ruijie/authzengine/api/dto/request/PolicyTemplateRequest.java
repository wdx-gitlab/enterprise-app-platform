package com.ruijie.authzengine.api.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 策略模板写入请求。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "策略模板写入请求")
public class PolicyTemplateRequest {

    @NotBlank(message = "租户标识不能为空")
    @Schema(description = "租户标识", example = "__GLOBAL__")
    private String tenantId;

    @NotBlank(message = "模板编码不能为空")
    @Schema(description = "模板编码", example = "ENV_WORK_HOUR")
    private String templateCode;

    @NotBlank(message = "模板名称不能为空")
    @Schema(description = "模板名称", example = "工作时间限制")
    private String templateName;

    @NotBlank(message = "策略类型不能为空")
    @Schema(description = "策略类型", example = "ENV")
    private String polType;

    @Schema(description = "表达式脚本（FIELD 类型的 HIDE/RESTRICTED/OPEN 动作可为空）", example = "return true;")
    private String expressionScript;

    @Schema(description = "参数结构", example = "{}")
    private String paramSchema;

    @Schema(description = "状态", example = "ENABLED")
    private String status;
}