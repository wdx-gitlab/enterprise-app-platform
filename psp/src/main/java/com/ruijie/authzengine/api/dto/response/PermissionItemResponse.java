package com.ruijie.authzengine.api.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 权限项响应。
 */
@Data
@Builder
@Schema(description = "权限项响应")
public class PermissionItemResponse {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键", example = "3001")
    private Long id;

    @Schema(description = "租户标识")
    private String tenantId;

    @Schema(description = "应用标识")
    private String appCode;

    @Schema(description = "权限项编码")
    private String permCode;

    @Schema(description = "资源模型编码")
    private String resourceModel;

    @Schema(description = "资源编码")
    private String resourceCode;

    @Schema(description = "动作编码")
    private String actionCode;

    @Schema(description = "失败策略")
    private String failStrategy;
}
