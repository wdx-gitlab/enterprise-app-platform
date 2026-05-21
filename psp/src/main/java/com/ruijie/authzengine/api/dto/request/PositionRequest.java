package com.ruijie.authzengine.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 岗位目录写入请求。
 */
@Data
@Schema(description = "岗位目录写入请求")
public class PositionRequest {

    @NotBlank(message = "租户标识不能为空")
    @Schema(description = "租户标识", example = "T001")
    private String tenantId;

    @NotBlank(message = "应用标识不能为空")
    @Schema(description = "应用标识", example = "CRM")
    private String appCode;

    @NotBlank(message = "岗位编码不能为空")
    @Schema(description = "岗位编码", example = "POS-MANAGER")
    private String positionCode;

    @NotBlank(message = "岗位名称不能为空")
    @Schema(description = "岗位名称", example = "销售经理")
    private String positionName;

    @Schema(description = "组织编码", example = "ORG-SALES")
    private String orgCode;

    @Schema(description = "状态", example = "ENABLED")
    private String status;
}