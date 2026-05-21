package com.ruijie.authzengine.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 操作受理响应。
 */
@Data
@Builder
@Schema(description = "操作受理响应")
public class OperationAckResponse {

    @Schema(description = "是否已受理", example = "true")
    private boolean accepted;

    @Schema(description = "业务标识", example = "RES_API")
    private String businessId;

    @Schema(description = "附加说明", example = "请求已受理")
    private String note;
}