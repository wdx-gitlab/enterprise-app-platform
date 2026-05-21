package com.ruijie.authzengine.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;
import lombok.Data;

/**
 * API 资源写入请求。
 */
@Data
@Schema(description = "API 资源写入请求")
public class ApiResourceRequest {

    @NotBlank(message = "租户标识不能为空")
    @Schema(description = "租户标识", example = "T001")
    private String tenantId;

    @NotBlank(message = "应用标识不能为空")
    @Schema(description = "应用标识", example = "CRM")
    private String appCode;

    @NotBlank(message = "API 编码不能为空")
    @Schema(description = "API 编码", example = "API-CONTRACT-QUERY")
    private String apiCode;

    @Schema(description = "API 名称", example = "合同查询接口")
    private String apiName;

    @NotBlank(message = "HTTP 方法不能为空")
    @Schema(description = "HTTP 方法", example = "GET")
    private String httpMethod;

    @NotBlank(message = "URI 模式不能为空")
    @Schema(description = "URI 模式", example = "/api/contracts/query")
    private String uriPattern;

    @Schema(description = "状态", example = "ENABLED")
    private String status;
}