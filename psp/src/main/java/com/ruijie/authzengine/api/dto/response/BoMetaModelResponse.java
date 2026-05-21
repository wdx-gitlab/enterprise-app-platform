package com.ruijie.authzengine.api.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 业务对象元模型响应。
 */
@Data
@Builder
@Schema(description = "业务对象元模型响应")
public class BoMetaModelResponse {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "租户标识")
    private String tenantId;

    @Schema(description = "应用标识")
    private String appCode;

    @Schema(description = "业务对象编码")
    private String boCode;

    @Schema(description = "业务对象名称")
    private String boName;

    @Schema(description = "Schema JSON")
    private String schemaJson;

    @Schema(description = "适配器类型")
    private String adapterType;

    @Schema(description = "解析器")
    private String resolver;
}