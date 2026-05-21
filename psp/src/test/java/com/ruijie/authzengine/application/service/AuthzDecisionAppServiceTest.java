package com.ruijie.authzengine.application.service;

import com.ruijie.authzengine.domain.model.common.SubjectKey;
import com.ruijie.authzengine.domain.model.decision.AuthzContext;
import com.ruijie.authzengine.domain.model.decision.AuthzDecision;
import com.ruijie.authzengine.domain.model.decision.AuthzRequest;
import com.ruijie.authzengine.domain.model.decision.AuthzResource;
import com.ruijie.authzengine.domain.model.decision.AuthzSubject;
import com.ruijie.authzengine.domain.model.decision.DecisionType;
import com.ruijie.authzengine.domain.model.decision.PermissionGrant;
import com.ruijie.authzengine.domain.model.ops.AuthzAuditPage;
import com.ruijie.authzengine.domain.model.ops.AuthzAuditQuery;
import com.ruijie.authzengine.domain.model.ops.AuthzAuditRecord;
import com.ruijie.authzengine.domain.repository.AuthzAuditRepository;
import com.ruijie.authzengine.domain.repository.AuthzAuditWriteRepository;
import com.ruijie.authzengine.domain.repository.AuthorizationPolicyRepository;
import com.ruijie.authzengine.domain.service.PermissionDecisionService;
import com.ruijie.authzengine.domain.service.PolicyInformationPoint;
import com.ruijie.authzengine.domain.service.SubjectExpansionService;
import com.ruijie.authzengine.infrastructure.authz.DefaultPolicyDecisionPoint;
import com.ruijie.authzengine.infrastructure.authz.DefaultPolicyEnforcementPoint;
import com.ruijie.authzengine.infrastructure.authz.DefaultPolicyInformationPoint;
import com.ruijie.authzengine.infrastructure.authz.InMemoryAuthorizationPolicyRepository;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AuthzDecisionAppServiceTest {

    @Test
    void shouldPermitWhenUserHasDirectPermission() {
        AuthzDecision decision = buildAppService().checkWithGovernance(buildRequest(Collections.emptyMap(), "APPROVE"));

        Assertions.assertEquals(DecisionType.PERMIT, decision.getDecision());
        Assertions.assertTrue(decision.getMatchedPermissions().contains("CONTRACT_APPROVE"));
    }

    @Test
    void shouldPermitWhenRolePermissionMatches() {
        Map<String, Object> context = new HashMap<>();
        context.put("roles", Collections.singletonList("contract-admin"));

        AuthzDecision decision = buildAppService().checkWithGovernance(buildRequest(context, "READ"));

        Assertions.assertEquals(DecisionType.PERMIT, decision.getDecision());
        Assertions.assertTrue(decision.getMatchedPermissions().contains("CONTRACT_READ"));
    }

    @Test
    void shouldPermitWhenDelegatedPermissionMatches() {
        Map<String, Object> context = new HashMap<>();
        context.put("roles", Collections.singletonList("contract-delegate"));

        AuthzDecision decision = buildAppService(buildDelegationRepository()).checkWithGovernance(buildRequest(context, "APPROVE"));

        Assertions.assertEquals(DecisionType.PERMIT, decision.getDecision());
        Assertions.assertTrue(decision.getMatchedPermissions().contains("CONTRACT_APPROVE_DELEGATED"));
        Assertions.assertTrue(decision.getMatchedDelegateIds().contains("DELEGATE-001"));
    }

    @Test
    void shouldReturnNotPermitWhenNoMatchingPermission() {
        AuthzDecision decision = buildAppService().checkWithGovernance(buildRequest(Collections.emptyMap(), "DELETE"));

        Assertions.assertEquals(DecisionType.NOT_PERMIT, decision.getDecision());
        Assertions.assertEquals("NO_PERMISSION_ITEM", decision.getReason());
    }

    @Test
    void shouldReturnNotPermitWhenSubjectNotRegistered() {
        AuthzRequest request = AuthzRequest.builder()
            .tenantId("T001")
            .appCode("CRM")
            .subject(AuthzSubject.builder().id("ghost-user").type("SUB_USER").build())
            .resource(AuthzResource.builder().resourceType("RES_DATA_BO").resId("").build())
            .action("APPROVE")
            .context(Collections.emptyMap())
            .build();

        AuthzDecision decision = buildAppService().checkWithGovernance(request);

        Assertions.assertEquals(DecisionType.NOT_PERMIT, decision.getDecision());
        Assertions.assertEquals("NO_ASSIGNMENT", decision.getReason());
    }

    @Test
    void shouldReturnNotPermitWhenResourceTypeDoesNotMatch() {
        AuthzRequest request = AuthzRequest.builder()
            .tenantId("T001")
            .appCode("CRM")
            .subject(AuthzSubject.builder().id("demo-user").type("SUB_USER").build())
            .resource(AuthzResource.builder().resourceType("RES_API").resId("9001").build())
            .action("APPROVE")
            .context(Collections.emptyMap())
            .build();

        AuthzDecision decision = buildAppService().checkWithGovernance(request);

        Assertions.assertEquals(DecisionType.NOT_PERMIT, decision.getDecision());
        Assertions.assertEquals("NO_PERMISSION_ITEM", decision.getReason());
    }

    @Test
    void shouldPermitWhenBoInstanceIdMatchesPermissionItemResId() {
        AuthorizationPolicyRepository repository = new AuthorizationPolicyRepository() {
            @Override
            public List<PermissionGrant> findBySubjects(String tenantId, String appCode, Set<SubjectKey> subjectKeys) {
                return Collections.singletonList(PermissionGrant.builder()
                    .assignmentId(11L)
                    .tenantId(tenantId)
                    .appCode(appCode)
                    .subjectType("SUB_USER")
                    .subjectId("demo-user")
                    .resourceType("RES_DATA_BO")
                    .resId("C-20260402-0001")
                    .action("READ")
                    .permissionCode("CONTRACT_INSTANCE_READ")
                    .build());
            }
        };
        AuthzRequest request = AuthzRequest.builder()
            .tenantId("T001")
            .appCode("CRM")
            .subject(AuthzSubject.builder().id("demo-user").type("SUB_USER").build())
            .resource(AuthzResource.builder()
                .resourceType("RES_DATA_BO")
                .resId("C-20260402-0001")
                .build())
            .action("READ")
            .context(Collections.emptyMap())
            .build();

        AuthzDecision decision = buildAppService(repository).checkWithGovernance(request);

        Assertions.assertEquals(DecisionType.PERMIT, decision.getDecision());
        Assertions.assertTrue(decision.getMatchedPermissions().contains("CONTRACT_INSTANCE_READ"));
    }

    @Test
    void shouldReturnIndeterminateWhenPipFails() {
        Map<String, Object> context = new HashMap<>();
        context.put("simulateHookError", true);

        AuthzDecision decision = buildAppService().checkWithGovernance(buildRequest(context, "APPROVE"));

        Assertions.assertEquals(DecisionType.INDETERMINATE, decision.getDecision());
        Assertions.assertEquals("HOOK_ERROR", decision.getReason());
    }

    @Test
    void shouldRecordNormalizedActionCodeIntoAuditLog() {
        CapturingAuditWriteRepository auditWriteRepository = new CapturingAuditWriteRepository();
        AuthzAuditAppService auditAppService = new AuthzAuditAppService(new NoopAuthzAuditRepository(), auditWriteRepository);
        PolicyInformationPoint pip = request -> AuthzContext.builder()
            .subjectKeys(Collections.singleton(new SubjectKey("SUB_USER", "demo-user")))
            .attributes(Collections.emptyMap())
            .delegationIds(Collections.emptySet())
            .governanceAttributes(Collections.singletonMap("normalizedActionCode", "READ"))
            .build();
        AuthorizationPolicyRepository repository = new AuthorizationPolicyRepository() {
            @Override
            public List<PermissionGrant> findBySubjects(String tenantId, String appCode, Set<SubjectKey> subjectKeys) {
                return Collections.singletonList(PermissionGrant.builder()
                    .assignmentId(1L)
                    .tenantId(tenantId)
                    .appCode(appCode)
                    .subjectType("SUB_USER")
                    .subjectId("demo-user")
                    .resourceType("RES_DATA_BO")
                    .resId("")
                    .action("READ")
                    .permissionCode("CONTRACT_READ")
                    .build());
            }
        };
        DefaultPolicyDecisionPoint pdp = new DefaultPolicyDecisionPoint(pip, repository, new PermissionDecisionService(null, new com.fasterxml.jackson.databind.ObjectMapper()));
        DefaultPolicyEnforcementPoint pep = new DefaultPolicyEnforcementPoint(pdp);
        AuthzFacade authzFacade = new AuthzFacade(pep, null);
        AuthzDecisionAppService appService = new AuthzDecisionAppService(authzFacade, auditAppService);

        AuthzDecision decision = appService.checkWithGovernance(buildRequest(new HashMap<>(), "query"));

        Assertions.assertEquals(DecisionType.PERMIT, decision.getDecision());
        Assertions.assertNotNull(auditWriteRepository.getLastSaved());
        Assertions.assertEquals("READ", auditWriteRepository.getLastSaved().getActionCode());
    }

    @Test
    void shouldExposeRowFilterAndFieldControlsInDecision() {
        AuthzDecision decision = buildAppService(buildObligationRepository(), buildObligationPip())
            .checkWithGovernance(buildRequest(new HashMap<>(), "READ"));

        Assertions.assertEquals(DecisionType.PERMIT, decision.getDecision());
        @SuppressWarnings("unchecked")
        Map<String, Object> rowFilter = (Map<String, Object>) decision.getObligations().get("rowFilter");
        Assertions.assertEquals("(biz_customer.owner_id = ?)", rowFilter.get("whereClause"));
        Assertions.assertEquals(Collections.singletonList("demo-user"), rowFilter.get("params"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fieldControls = (List<Map<String, Object>>) decision.getObligations().get("fieldControls");
        Assertions.assertEquals(1, fieldControls.size());
        Assertions.assertEquals("MASK", fieldControls.get(0).get("action"));
        Assertions.assertEquals("FIELD_MASK_MOBILE", fieldControls.get(0).get("policyCode"));
    }

    private AuthzDecisionAppService buildAppService() {
        return buildAppService(new InMemoryAuthorizationPolicyRepository());
    }

    private AuthzDecisionAppService buildAppService(AuthorizationPolicyRepository repository) {
        return buildAppService(repository, new DefaultPolicyInformationPoint(new SubjectExpansionService()));
    }

    private AuthzDecisionAppService buildAppService(AuthorizationPolicyRepository repository, PolicyInformationPoint pip) {
        DefaultPolicyDecisionPoint pdp = new DefaultPolicyDecisionPoint(pip, repository, new PermissionDecisionService(null, new com.fasterxml.jackson.databind.ObjectMapper()));
        DefaultPolicyEnforcementPoint pep = new DefaultPolicyEnforcementPoint(pdp);
        AuthzFacade authzFacade = new AuthzFacade(pep, null);
        return new AuthzDecisionAppService(authzFacade);
    }

    private PolicyInformationPoint buildObligationPip() {
        return request -> {
            Map<String, Object> attributes = new LinkedHashMap<>();
            attributes.put("sub", Collections.singletonMap("userId", "demo-user"));
            Map<String, Object> governanceAttributes = new LinkedHashMap<>();
            governanceAttributes.put("subjectRegistered", true);
            governanceAttributes.put("resourceRegistered", true);
            governanceAttributes.put("actionRegistered", true);
            governanceAttributes.put("normalizedActionCode", "READ");
            governanceAttributes.put("tableName", "biz_customer");
            governanceAttributes.put("attributes", Collections.singletonList(buildFieldAttribute()));
            return AuthzContext.builder()
                .subjectKeys(Collections.singleton(new SubjectKey("SUB_USER", "demo-user")))
                .attributes(attributes)
                .delegationIds(Collections.emptySet())
                .governanceAttributes(governanceAttributes)
                .build();
        };
    }

    private AuthorizationPolicyRepository buildObligationRepository() {
        return new AuthorizationPolicyRepository() {
            @Override
            public List<PermissionGrant> findBySubjects(String tenantId, String appCode, Set<SubjectKey> subjectKeys) {
                return Arrays.asList(
                    PermissionGrant.builder()
                        .assignmentId(21L)
                        .tenantId(tenantId)
                        .appCode(appCode)
                        .subjectType("SUB_USER")
                        .subjectId("demo-user")
                        .resourceType("RES_DATA_BO")
                        .resId("")
                        .action("READ")
                        .permissionCode("CONTRACT_READ")
                        .policyTemplateCode("DATA_OWNER_ONLY")
                        .policyTemplateType("DATA")
                        .policyTemplateStatus("ENABLED")
                        .expressionScript("#tableName + '.owner_id = ' + param(#sub['userId'])")
                        .failStrategy("DENY")
                        .build(),
                    PermissionGrant.builder()
                        .assignmentId(22L)
                        .tenantId(tenantId)
                        .appCode(appCode)
                        .subjectType("SUB_USER")
                        .subjectId("demo-user")
                        .resourceType("RES_DATA_BO")
                        .resId("")
                        .action("READ")
                        .permissionCode("CONTRACT_READ")
                        .policyTemplateCode("FIELD_MASK_MOBILE")
                        .policyTemplateType("FIELD")
                        .policyTemplateStatus("ENABLED")
                        .paramSchema("{\"action\":\"MASK\"}")
                        .policyParams("{\"targetField\":\"mobile\"}")
                        .build()
                );
            }
        };
    }

    private Map<String, Object> buildFieldAttribute() {
        Map<String, Object> attribute = new LinkedHashMap<>();
        attribute.put("code", "mobile");
        attribute.put("fieldName", "mobile");
        attribute.put("columnName", "mobile");
        attribute.put("type", "STRING");
        attribute.put("fieldControl", true);
        return attribute;
    }

    private AuthzRequest buildRequest(Map<String, Object> context, String action) {
        return AuthzRequest.builder()
            .tenantId("T001")
            .appCode("CRM")
            .subject(AuthzSubject.builder().id("demo-user").type("SUB_USER").build())
            .resource(AuthzResource.builder().resourceType("RES_DATA_BO").resId("").build())
            .action(action)
            .context(context)
            .build();
    }

    private AuthorizationPolicyRepository buildDelegationRepository() {
        return new AuthorizationPolicyRepository() {
            @Override
            public List<PermissionGrant> findBySubjects(String tenantId, String appCode, Set<com.ruijie.authzengine.domain.model.common.SubjectKey> subjectKeys) {
                boolean matched = subjectKeys.stream().anyMatch(subjectKey ->
                    "SUB_ROLE".equals(subjectKey.getSubjectType()) && "contract-delegate".equals(subjectKey.getSubjectId())
                );
                if (!matched) {
                    return Collections.emptyList();
                }
                return Collections.singletonList(PermissionGrant.builder()
                    .assignmentId(9L)
                    .tenantId(tenantId)
                    .appCode(appCode)
                    .subjectType("SUB_ROLE")
                    .subjectId("contract-delegate")
                    .resourceType("RES_DATA_BO")
                    .resId("")
                    .action("APPROVE")
                    .permissionCode("CONTRACT_APPROVE_DELEGATED")
                    .delegateId("DELEGATE-001")
                    .build());
            }
        };
    }

    private static final class NoopAuthzAuditRepository implements AuthzAuditRepository {

        @Override
        public AuthzAuditPage query(AuthzAuditQuery query) {
            return AuthzAuditPage.builder()
                .records(Collections.emptyList())
                .pageNo(query.getPageNo())
                .pageSize(query.getPageSize())
                .total(0)
                .build();
        }

        @Override
        public AuthzAuditRecord findById(String tenantId, String appCode, Long auditLogId) {
            return null;
        }
    }

    private static final class CapturingAuditWriteRepository implements AuthzAuditWriteRepository {

        private AuthzAuditRecord lastSaved;

        @Override
        public AuthzAuditRecord save(AuthzAuditRecord authzAuditRecord) {
            this.lastSaved = authzAuditRecord;
            return authzAuditRecord;
        }

        private AuthzAuditRecord getLastSaved() {
            return lastSaved;
        }
    }
}