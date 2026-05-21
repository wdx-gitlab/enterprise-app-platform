package com.ruijie.authzengine.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * 委托授权撤销请求。
 */
@Data
@Schema(description = "委托授权撤销请求")
public class DelegationRevokeRequest {

    @NotBlank(message = "租户标识不能为空")
    @Schema(description = "租户标识", example = "T001")
    private String tenantId;

    @NotBlank(message = "应用标识不能为空")
    @Schema(description = "应用标识", example = "CRM")
    private String appCode;

    @NotNull(message = "委托记录标识不能为空")
    @Schema(description = "委托记录标识", example = "9000001")
    private Long delegationId;
}