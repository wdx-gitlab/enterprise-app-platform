package com.ruijie.authzengine.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import lombok.Data;

/**
 * 权限项写入请求。
 */
@Data
@Schema(description = "权限项写入请求")
public class PermissionItemRequest {

    @NotBlank(message = "租户标识不能为空")
    @Schema(description = "租户标识", example = "T001")
    private String tenantId;

    @NotBlank(message = "应用标识不能为空")
    @Schema(description = "应用标识", example = "CRM")
    private String appCode;

    @Schema(description = "权限项编码，只读预览字段，服务端会根据资源与动作自动生成", example = "CRM:api:contract.query:READ")
    private String permCode;

    @NotBlank(message = "资源模型编码不能为空")
    @Pattern(regexp = "RES_DATA_BO|RES_API", message = "资源模型编码只允许 RES_DATA_BO 或 RES_API")
    @Schema(description = "资源模型编码，只允许 RES_DATA_BO 或 RES_API", example = "RES_API", allowableValues = {"RES_DATA_BO", "RES_API"})
    private String resourceModel;

    @NotBlank(message = "资源编码不能为空")
    @Schema(description = "资源编码", example = "API-CONTRACT-QUERY")
    private String resourceCode;

    @NotBlank(message = "动作编码不能为空")
    @Schema(description = "动作编码", example = "READ")
    private String actionCode;

    @Schema(description = "失败策略", example = "DENY")
    private String failStrategy;
}
