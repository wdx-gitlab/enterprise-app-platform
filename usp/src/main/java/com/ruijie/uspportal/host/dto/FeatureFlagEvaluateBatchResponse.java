package com.ruijie.uspportal.host.dto;

import lombok.Builder;

import java.util.List;

@lombok.Data
@Builder
/**
 * FeatureFlagEvaluateBatch 响应对象。
 */
public class FeatureFlagEvaluateBatchResponse {

    private String tenantCode;

    private String userId;

    private List<FlagResult> results;

    @lombok.Data
    @Builder
    /**
     * FlagResult 类。
     */
    public static class FlagResult {

        private String flagKey;

        private Boolean enabled;

        private String matchedRule;

        private String reason;
    }
}
