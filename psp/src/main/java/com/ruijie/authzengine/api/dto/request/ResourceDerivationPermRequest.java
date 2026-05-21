package com.ruijie.authzengine.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * 派生权限关联写入请求。
 */
@Data
@Schema(description = "派生权限关联写入请求")
public class ResourceDerivationPermRequest {

    @NotBlank(message = "租户标识不能为空")
    @Schema(description = "租户标识", example = "T001")
    private String tenantId;

    @NotBlank(message = "应用标识不能为空")
    @Schema(description = "应用标识", example = "CRM")
    private String appCode;

    @NotBlank(message = "资源类型不能为空")
    @Schema(description = "资源类型：RES_UI_PAGE / RES_UI_COMPONENT / RES_API", example = "RES_UI_PAGE")
    private String resType;

    @NotNull(message = "资源主键不能为空")
    @Schema(description = "资源主键 ID（指向对应 usp_* 表）", example = "100001")
    private Long resId;

    @NotNull(message = "关联权限项 ID 不能为空")
    @Schema(description = "派生来源权限项 ID", example = "200001")
    private Long permItemId;

    @Schema(description = "同一资源下的展示顺序，默认 0", example = "0")
    private Integer sortOrder;
}
