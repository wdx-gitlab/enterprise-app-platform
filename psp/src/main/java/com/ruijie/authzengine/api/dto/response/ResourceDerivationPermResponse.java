package com.ruijie.authzengine.api.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 派生权限关联响应。
 */
@Data
@Builder
@Schema(description = "派生权限关联响应")
public class ResourceDerivationPermResponse {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键", example = "600001")
    private Long id;

    @Schema(description = "租户标识")
    private String tenantId;

    @Schema(description = "应用标识")
    private String appCode;

    @Schema(description = "资源类型：RES_UI_PAGE / RES_UI_COMPONENT / RES_API")
    private String resType;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "资源主键 ID")
    private Long resId;

    @Schema(description = "资源编码（从对应 usp_* 表反查，仅用于展示）")
    private String resCode;

    @Schema(description = "资源名称（仅用于展示）")
    private String resName;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "派生来源权限项 ID")
    private Long permItemId;

    @Schema(description = "权限项编码（仅用于展示）")
    private String permCode;

    @Schema(description = "展示顺序")
    private Integer sortOrder;
}
