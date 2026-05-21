package com.ruijie.authzengine.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

/**
 * 批量鉴权占位响应。
 */
@Data
@Builder
@Schema(description = "批量鉴权占位响应")
public class BatchAuthzContractResponse {

    @Schema(description = "能力状态", example = "CONTRACT_ONLY")
    private String capabilityStatus;

    @Schema(description = "规划说明", example = "仅定义批量请求与统一响应结构，不包含执行算法")
    private String plannedScope;

    @Schema(description = "批量结果占位片段")
    private List<Map<String, Object>> results;
}