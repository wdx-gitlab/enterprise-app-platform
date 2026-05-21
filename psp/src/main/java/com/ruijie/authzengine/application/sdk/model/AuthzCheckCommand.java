package com.ruijie.authzengine.application.sdk.model;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单次鉴权命令。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthzCheckCommand {

    private String tenantId;

    private String appCode;

    private AuthzSubjectRef subject;

    private AuthzResourceRef resource;

    private String action;

    private Map<String, Object> context;

    private String traceId;
}