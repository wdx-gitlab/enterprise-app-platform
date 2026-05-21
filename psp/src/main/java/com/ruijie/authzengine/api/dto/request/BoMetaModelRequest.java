package com.ruijie.authzengine.api.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 业务对象元模型写入请求。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "业务对象元模型写入请求")
public class BoMetaModelRequest {

    @NotBlank(message = "租户标识不能为空")
    @Schema(description = "租户标识", example = "T001")
    private String tenantId;

    @NotBlank(message = "应用标识不能为空")
    @Schema(description = "应用标识", example = "CRM")
    private String appCode;

    @NotBlank(message = "业务对象编码不能为空")
    @Schema(description = "业务对象编码", example = "CONTRACT")
    private String boCode;

    @NotBlank(message = "业务对象名称不能为空")
    @Schema(description = "业务对象名称", example = "合同")
    private String boName;

    @Schema(description = "Schema JSON", example = "{}")
    private String schemaJson;

    @Schema(description = "适配器类型", example = "JAVA_BEAN")
    private String adapterType;

    @Schema(description = "解析器", example = "contractHook")
    private String resolver;
}