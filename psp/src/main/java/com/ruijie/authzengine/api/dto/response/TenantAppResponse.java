package com.ruijie.authzengine.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 租户-应用组合响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "租户-应用组合")
public class TenantAppResponse {

    @Schema(description = "租户标识", example = "T001")
    private String tenantId;

    @Schema(description = "应用编码", example = "CRM")
    private String appCode;
}
