package com.ruijie.authzengine.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 审计日志分页响应。
 */
@Data
@Builder
@Schema(description = "审计日志分页响应")
public class AuditLogPageResponse {

    @Schema(description = "分页记录")
    private List<AuditLogItemResponse> records;

    @Schema(description = "页码", example = "1")
    private int pageNo;

    @Schema(description = "分页大小", example = "20")
    private int pageSize;

    @Schema(description = "总记录数", example = "1")
    private long total;
}