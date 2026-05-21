package com.ruijie.authzengine.api.assembler;

import com.ruijie.authzengine.api.dto.request.AuthzCheckRequest;
import com.ruijie.authzengine.api.dto.response.AuthzCheckResponse;
import com.ruijie.authzengine.domain.model.decision.AuthzDecision;
import com.ruijie.authzengine.domain.model.decision.AuthzRequest;
import com.ruijie.authzengine.domain.model.decision.AuthzResource;
import com.ruijie.authzengine.domain.model.decision.AuthzSubject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 鉴权请求与响应装配器。
 * <p>
 * resourceModel 直接透传为 resourceType（资源大类），不再做 BO 模型反查。
 * resourceId 直接透传为 resId（资源表主键）。
 * </p>
 */
@Slf4j
@Component
public class AuthzRequestAssembler {

    /**
     * 请求 DTO 转换为领域对象。
     *
     * @param request 接口请求
     * @return 领域请求对象
     */
    public AuthzRequest toDomain(AuthzCheckRequest request) {
        String resourceType = StringUtils.hasText(request.getResource().getResourceModel())
            ? request.getResource().getResourceModel().trim() : null;
        String resId = StringUtils.hasText(request.getResource().getResourceId())
            ? request.getResource().getResourceId().trim() : null;
        return AuthzRequest.builder()
            .tenantId(request.getTenantId())
            .appCode(request.getAppCode())
            .subject(AuthzSubject.builder()
                .id(request.getSubject().getSubjectId())
                .type(request.getSubject().getSubjectModel())
                .build())
            .resource(AuthzResource.builder()
                .resourceType(resourceType)
                .resId(resId)
                .build())
            .action(request.getAction())
            .context(request.getContext())
            .traceId(request.getTraceId())
            .build();
    }

    /**
     * 领域决策对象转换为响应 DTO。
     *
     * @param decision 领域决策对象
     * @return 对外响应 DTO
     */
    public AuthzCheckResponse toResponse(AuthzDecision decision) {
        return AuthzCheckResponse.builder()
            .decision(decision.getDecision().name())
            .reason(decision.getReason())
            .matchedPermissionCodes(decision.getMatchedPermissions())
            .matchedAssignmentIds(decision.getMatchedAssignmentIds())
            .matchedDelegateIds(decision.getMatchedDelegateIds())
            .matchedPolicyTemplateCodes(decision.getMatchedPolicyTemplateCodes())
            .obligations(decision.getObligations())
            .auditLogId(decision.getAuditLogId())
            .build();
    }
}