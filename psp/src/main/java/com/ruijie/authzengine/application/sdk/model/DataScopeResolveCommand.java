package com.ruijie.authzengine.application.sdk.model;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * data-scope 解析命令。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataScopeResolveCommand {

    private String tenantId;

    private String appCode;

    private String policyTemplateCode;

    private AuthzSubjectRef subject;

    private AuthzResourceRef resource;

    private String semanticCondition;

    private Map<String, Object> context;
}