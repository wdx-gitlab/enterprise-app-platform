package com.ruijie.authzengine.api.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 标准策略模板响应。
 */
@Data
@Builder
@Schema(description = "标准策略模板响应")
public class StandardPolicyTemplateResponse {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "租户标识")
    private String tenantId;

    @Schema(description = "模板编码")
    private String templateCode;

    @Schema(description = "模板名称")
    private String templateName;

    @Schema(description = "策略类型")
    private String polType;

    @Schema(description = "表达式脚本")
    private String expressionScript;

    @Schema(description = "参数结构")
    private String paramSchema;

    @Schema(description = "模板状态")
    private String status;
}