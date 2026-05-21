package com.ruijie.authzengine.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 权限元模型写入请求。
 */
@Data
@Schema(description = "权限元模型写入请求")
public class MetaModelRequest {

    @NotBlank(message = "租户标识不能为空")
    @Schema(description = "租户标识", example = "T001")
    private String tenantId;

    @NotBlank(message = "应用标识不能为空")
    @Schema(description = "应用标识", example = "CRM")
    private String appCode;

    @NotBlank(message = "模型编码不能为空")
    @Schema(description = "模型编码", example = "RES_API")
    private String modelCode;

    @Schema(description = "模型名称", example = "接口资源")
    private String modelName;

    @NotBlank(message = "模型分类不能为空")
    @Schema(description = "模型分类", example = "RESOURCE")
    private String category;

    @Schema(description = "适配器类型", example = "JAVA_BEAN")
    private String adapterType;

    @Schema(description = "解析器", example = "noopHook")
    private String resolver;

    @Schema(description = "Schema 视图", example = "{}")
    private String schemaView;
}