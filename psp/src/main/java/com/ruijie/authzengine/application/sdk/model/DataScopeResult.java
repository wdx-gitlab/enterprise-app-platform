package com.ruijie.authzengine.application.sdk.model;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * data-scope 解析结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataScopeResult {

    private String capabilityStatus;

    private String plannedScope;

    private String translatedSql;

    private List<Map<String, Object>> scopeFragments;
}