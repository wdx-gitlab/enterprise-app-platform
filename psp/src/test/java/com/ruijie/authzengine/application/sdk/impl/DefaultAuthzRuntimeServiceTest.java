package com.ruijie.authzengine.application.sdk.impl;

import com.ruijie.authzengine.application.sdk.model.AuthzCheckCommand;
import com.ruijie.authzengine.application.sdk.model.AuthzCheckResult;
import com.ruijie.authzengine.application.sdk.model.AuthzResourceRef;
import com.ruijie.authzengine.application.sdk.model.AuthzSubjectRef;
import com.ruijie.authzengine.application.sdk.model.PermCodeCheckCommand;
import com.ruijie.authzengine.application.service.AuthzDecisionAppService;
import com.ruijie.authzengine.application.service.AuthzFacade;
import com.ruijie.authzengine.domain.model.decision.AuthzDecision;
import com.ruijie.authzengine.domain.model.decision.AuthzRequest;
import com.ruijie.authzengine.domain.model.decision.DecisionType;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultAuthzRuntimeServiceTest {

    @Mock
    private AuthzDecisionAppService authzDecisionAppService;

    @Mock
    private AuthzFacade authzFacade;

    private DefaultAuthzRuntimeService runtimeService;

    @BeforeEach
    void setUp() {
        runtimeService = new DefaultAuthzRuntimeService(authzDecisionAppService, authzFacade);
    }

    @Test
    void shouldDelegateCheckToDecisionAppService() {
        AuthzDecision decision = AuthzDecision.builder()
            .decision(DecisionType.PERMIT)
            .reason("PERMIT")
            .matchedPermissions(Collections.singletonList("PERM_CONTRACT_READ"))
            .matchedAssignmentIds(Collections.singletonList("1001"))
            .matchedDelegateIds(Collections.singletonList("2001"))
            .matchedPolicyTemplateCodes(Collections.singletonList("TPL_READ"))
            .obligations(Collections.singletonMap("failStrategy", "DENY"))
            .auditLogId("9001")
            .build();
        when(authzDecisionAppService.checkWithGovernance(any(AuthzRequest.class))).thenReturn(decision);

        AuthzCheckResult result = runtimeService.check(AuthzCheckCommand.builder()
            .tenantId("T001")
            .appCode("CRM")
            .subject(AuthzSubjectRef.builder().subjectId("U10001").subjectModel("SUB_USER").build())
            .resource(AuthzResourceRef.builder().resourceModel("RES_API").resourceId("API_CONTRACT_LIST").build())
            .action("READ")
            .traceId("TRACE-001")
            .build());

        ArgumentCaptor<AuthzRequest> captor = ArgumentCaptor.forClass(AuthzRequest.class);
        verify(authzDecisionAppService).checkWithGovernance(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo("T001");
        assertThat(captor.getValue().getAppCode()).isEqualTo("CRM");
        assertThat(captor.getValue().getSubject().getId()).isEqualTo("U10001");
        assertThat(captor.getValue().getSubject().getType()).isEqualTo("SUB_USER");
        assertThat(captor.getValue().getResource().getResourceType()).isEqualTo("RES_API");
        assertThat(captor.getValue().getResource().getResId()).isEqualTo("API_CONTRACT_LIST");
        assertThat(captor.getValue().getAction()).isEqualTo("READ");
        assertThat(captor.getValue().getTraceId()).isEqualTo("TRACE-001");

        assertThat(result.getDecision()).isEqualTo("PERMIT");
        assertThat(result.getMatchedPermissionCodes()).containsExactly("PERM_CONTRACT_READ");
        assertThat(result.getAuditLogId()).isEqualTo("9001");
    }

    @Test
    void shouldDelegatePermCodeCheckToFacade() {
        when(authzFacade.checkByPermCode("T001", "CRM", "U10001", "PERM_CONTRACT_APPROVE", "C-001"))
            .thenReturn(AuthzDecision.notPermit("NO_PERMISSION"));

        AuthzCheckResult result = runtimeService.checkByPermissionCode(PermCodeCheckCommand.builder()
            .tenantId("T001")
            .appCode("CRM")
            .subjectId("U10001")
            .permissionCode("PERM_CONTRACT_APPROVE")
            .resourceId("C-001")
            .build());

        verify(authzFacade).checkByPermCode("T001", "CRM", "U10001", "PERM_CONTRACT_APPROVE", "C-001");
        assertThat(result.getDecision()).isEqualTo("NOT_PERMIT");
        assertThat(result.getReason()).isEqualTo("NO_PERMISSION");
    }
}