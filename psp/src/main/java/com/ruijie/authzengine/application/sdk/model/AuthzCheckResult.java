package com.ruijie.authzengine.application.sdk.model;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SDK 鉴权结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthzCheckResult {

    private String decision;

    private String reason;

    private List<String> matchedPermissionCodes;

    private List<String> matchedAssignmentIds;

    private List<String> matchedDelegateIds;

    private List<String> matchedPolicyTemplateCodes;

    private Map<String, Object> obligations;

    private String auditLogId;
}