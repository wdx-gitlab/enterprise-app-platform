package com.ruijie.authzengine.application.sdk.impl;

import com.ruijie.authzengine.application.sdk.AuthzRuntimeService;
import com.ruijie.authzengine.application.sdk.model.AuthzCheckCommand;
import com.ruijie.authzengine.application.sdk.model.AuthzCheckResult;
import com.ruijie.authzengine.application.sdk.model.PermCodeCheckCommand;
import com.ruijie.authzengine.application.service.AuthzDecisionAppService;
import com.ruijie.authzengine.application.service.AuthzFacade;
import com.ruijie.authzengine.domain.model.decision.AuthzDecision;
import com.ruijie.authzengine.domain.model.decision.AuthzRequest;
import com.ruijie.authzengine.domain.model.decision.AuthzResource;
import com.ruijie.authzengine.domain.model.decision.AuthzSubject;
import java.util.Collections;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 默认运行时鉴权服务实现。
 */
public class DefaultAuthzRuntimeService implements AuthzRuntimeService {

    private final AuthzDecisionAppService authzDecisionAppService;

    private final AuthzFacade authzFacade;

    public DefaultAuthzRuntimeService(AuthzDecisionAppService authzDecisionAppService,
                                      AuthzFacade authzFacade) {
        this.authzDecisionAppService = authzDecisionAppService;
        this.authzFacade = authzFacade;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AuthzCheckResult check(AuthzCheckCommand command) {
        validateCheckCommand(command);
        AuthzDecision decision = authzDecisionAppService.checkWithGovernance(toDomainRequest(command));
        return toResult(decision);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AuthzCheckResult checkByPermissionCode(PermCodeCheckCommand command) {
        validatePermCodeCommand(command);
        AuthzDecision decision = authzFacade.checkByPermCode(
            command.getTenantId(),
            command.getAppCode(),
            command.getSubjectId(),
            command.getPermissionCode(),
            command.getResourceId()
        );
        return toResult(decision);
    }

    private void validateCheckCommand(AuthzCheckCommand command) {
        Assert.notNull(command, "command 不能为空");
        Assert.hasText(command.getTenantId(), "tenantId 不能为空");
        Assert.hasText(command.getAppCode(), "appCode 不能为空");
        Assert.hasText(command.getAction(), "action 不能为空");
        Assert.notNull(command.getSubject(), "subject 不能为空");
        Assert.notNull(command.getResource(), "resource 不能为空");
        Assert.hasText(command.getSubject().getSubjectId(), "subject.subjectId 不能为空");
        Assert.hasText(command.getSubject().getSubjectModel(), "subject.subjectModel 不能为空");
        Assert.hasText(command.getResource().getResourceModel(), "resource.resourceModel 不能为空");
        Assert.hasText(command.getResource().getResourceId(), "resource.resourceId 不能为空");
    }

    private void validatePermCodeCommand(PermCodeCheckCommand command) {
        Assert.notNull(command, "command 不能为空");
        Assert.hasText(command.getTenantId(), "tenantId 不能为空");
        Assert.hasText(command.getAppCode(), "appCode 不能为空");
        Assert.hasText(command.getSubjectId(), "subjectId 不能为空");
        Assert.hasText(command.getPermissionCode(), "permissionCode 不能为空");
    }

    private AuthzRequest toDomainRequest(AuthzCheckCommand command) {
        return AuthzRequest.builder()
            .tenantId(command.getTenantId())
            .appCode(command.getAppCode())
            .subject(AuthzSubject.builder()
                .id(command.getSubject().getSubjectId())
                .type(command.getSubject().getSubjectModel())
                .build())
            .resource(AuthzResource.builder()
                .resourceType(command.getResource().getResourceModel())
                .resId(command.getResource().getResourceId())
                .build())
            .action(command.getAction())
            .context(command.getContext())
            .traceId(StringUtils.hasText(command.getTraceId()) ? command.getTraceId() : null)
            .build();
    }

    private AuthzCheckResult toResult(AuthzDecision decision) {
        Assert.notNull(decision, "decision 不能为空");
        return AuthzCheckResult.builder()
            .decision(decision.getDecision() == null ? null : decision.getDecision().name())
            .reason(decision.getReason())
            .matchedPermissionCodes(decision.getMatchedPermissions() == null
                ? Collections.<String>emptyList() : decision.getMatchedPermissions())
            .matchedAssignmentIds(decision.getMatchedAssignmentIds() == null
                ? Collections.<String>emptyList() : decision.getMatchedAssignmentIds())
            .matchedDelegateIds(decision.getMatchedDelegateIds() == null
                ? Collections.<String>emptyList() : decision.getMatchedDelegateIds())
            .matchedPolicyTemplateCodes(decision.getMatchedPolicyTemplateCodes() == null
                ? Collections.<String>emptyList() : decision.getMatchedPolicyTemplateCodes())
            .obligations(decision.getObligations() == null
                ? Collections.<String, Object>emptyMap() : decision.getObligations())
            .auditLogId(decision.getAuditLogId())
            .build();
    }
}