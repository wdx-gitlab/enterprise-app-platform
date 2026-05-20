package com.ruijie.uspportal.portalconfig.dto;

import lombok.Data;

import java.util.Map;

@Data
public class FeatureFlagEvaluateRequest {

    private String flagKey;

    private Map<String, String> context;
}
