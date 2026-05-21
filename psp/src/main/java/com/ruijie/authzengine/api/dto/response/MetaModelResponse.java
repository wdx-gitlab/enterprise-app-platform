package com.ruijie.authzengine.api.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 权限元模型响应。
 */
@Data
@Builder
@Schema(description = "权限元模型响应")
public class MetaModelResponse {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "租户标识")
    private String tenantId;

    @Schema(description = "应用标识")
    private String appCode;

    @Schema(description = "模型编码")
    private String modelCode;

    @Schema(description = "模型名称")
    private String modelName;

    @Schema(description = "模型分类")
    private String category;

    @Schema(description = "适配器类型")
    private String adapterType;

    @Schema(description = "解析器")
    private String resolver;

    @Schema(description = "Schema 视图")
    private String schemaView;
}