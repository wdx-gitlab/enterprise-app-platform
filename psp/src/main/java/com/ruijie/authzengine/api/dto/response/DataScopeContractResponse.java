package com.ruijie.authzengine.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

/**
 * data-scope 占位响应。
 */
@Data
@Builder
@Schema(description = "data-scope 占位响应")
public class DataScopeContractResponse {

    @Schema(description = "能力状态", example = "CONTRACT_ONLY")
    private String capabilityStatus;

    @Schema(description = "规划说明", example = "本轮不实现数据权限具体计算与 SQL 下推，只预留接口与结构")
    private String plannedScope;

    @Schema(description = "宿主 Hook 翻译后的物理 SQL 片段，仅首版单条语义条件翻译成功时返回", example = "stage = 'APPROVING'")
    private String translatedSql;

    @Schema(description = "数据范围片段占位")
    private List<Map<String, Object>> scopeFragments;
}