package com.ruijie.uspportal.host.dto;

import lombok.Data;

import java.util.List;

@Data
/**
 * FeatureFlagEvaluateBatch 请求对象。
 */
public class FeatureFlagEvaluateBatchRequest {

    private List<String> flagKeys;

    private String tenantCode;

    private String orgCode;

    private String userId;

    private String appCode;
}
