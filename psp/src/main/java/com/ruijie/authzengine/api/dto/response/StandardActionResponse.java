package com.ruijie.authzengine.api.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 标准动作响应。
 */
@Data
@Builder
@Schema(description = "标准动作响应")
public class StandardActionResponse {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "租户标识")
    private String tenantId;

    @Schema(description = "动作编码")
    private String actCode;

    @Schema(description = "动作名称")
    private String actName;

    @Schema(description = "动作类型")
    private String actType;

    @Schema(description = "资源分类")
    private String resCategory;

    @Schema(description = "风险等级")
    private Integer riskLevel;
}