package com.ruijie.authzengine.application.service;

import com.ruijie.authzengine.domain.model.common.SubjectKey;
import com.ruijie.authzengine.domain.model.decision.AuthzContext;
import com.ruijie.authzengine.domain.model.decision.AuthzDecision;
import com.ruijie.authzengine.domain.model.decision.AuthzRequest;
import com.ruijie.authzengine.domain.model.decision.AuthzResource;
import com.ruijie.authzengine.domain.model.decision.AuthzSubject;
import com.ruijie.authzengine.domain.model.decision.PermissionGrant;
import com.ruijie.authzengine.domain.repository.AuthorizationPolicyRepository;
import com.ruijie.authzengine.domain.service.PermissionDecisionService;
import com.ruijie.authzengine.domain.service.PolicyInformationPoint;
import com.ruijie.authzengine.infrastructure.authz.DefaultPolicyDecisionPoint;
import com.ruijie.authzengine.infrastructure.authz.DefaultPolicyEnforcementPoint;
import com.ruijie.authzengine.infrastructure.authz.BoFieldMappingSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

class AuthzDecisionPkColumnObligationTest {

    @Test
    void shouldExposePkColumnsInRowFilterObligation() {
        PolicyInformationPoint pip = request -> {
            Map<String, Object> attributes = new LinkedHashMap<>();
            attributes.put("sub", Collections.singletonMap("userId", "demo-user"));
            Map<String, Object> governanceAttributes = new LinkedHashMap<>();
            governanceAttributes.put("subjectRegistered", true);
            governanceAttributes.put("resourceRegistered", true);
            governanceAttributes.put("actionRegistered", true);
            governanceAttributes.put("normalizedActionCode", "READ");
            governanceAttributes.put("tableName", "biz_customer");
            governanceAttributes.put("attributes", Arrays.asList(buildPkAttribute(), buildOwnerAttribute()));
            return AuthzContext.builder()
                .subjectKeys(Collections.singleton(new SubjectKey("SUB_USER", "demo-user")))
                .attributes(attributes)
                .delegationIds(Collections.emptySet())
                .governanceAttributes(governanceAttributes)
                .build();
        };
        AuthorizationPolicyRepository repository = new AuthorizationPolicyRepository() {
            @Override
            public List<PermissionGrant> findBySubjects(String tenantId, String appCode, Set<SubjectKey> subjectKeys) {
                return Collections.singletonList(PermissionGrant.builder()
                    .assignmentId(31L)
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
                    .build());
            }
        };

        DefaultPolicyDecisionPoint pdp = new DefaultPolicyDecisionPoint(
            pip,
            repository,
            new PermissionDecisionService(null, new com.fasterxml.jackson.databind.ObjectMapper()));
        DefaultPolicyEnforcementPoint pep = new DefaultPolicyEnforcementPoint(pdp);
        AuthzFacade authzFacade = new AuthzFacade(pep, null);
        AuthzDecisionAppService appService = new AuthzDecisionAppService(authzFacade);

        AuthzDecision decision = appService.checkWithGovernance(AuthzRequest.builder()
            .tenantId("T001")
            .appCode("CRM")
            .subject(AuthzSubject.builder().id("demo-user").type("SUB_USER").build())
            .resource(AuthzResource.builder().resourceType("RES_DATA_BO").resId("").build())
            .action("READ")
            .context(Collections.emptyMap())
            .build());

        @SuppressWarnings("unchecked")
        Map<String, Object> rowFilter = (Map<String, Object>) decision.getObligations().get("rowFilter");
        Assertions.assertEquals(Collections.singletonList("id"), rowFilter.get(BoFieldMappingSupport.ROW_FILTER_PK_COLUMNS_KEY));
    }

    private static Map<String, Object> buildPkAttribute() {
        Map<String, Object> attribute = new LinkedHashMap<>();
        attribute.put("code", "id");
        attribute.put("fieldName", "id");
        attribute.put("columnName", "id");
        attribute.put("type", "LONG");
        attribute.put("isPk", true);
        attribute.put("tableName", "biz_customer");
        return attribute;
    }

    private static Map<String, Object> buildOwnerAttribute() {
        Map<String, Object> attribute = new LinkedHashMap<>();
        attribute.put("code", "ownerId");
        attribute.put("fieldName", "ownerId");
        attribute.put("columnName", "owner_id");
        attribute.put("type", "STRING");
        attribute.put("tableName", "biz_customer");
        return attribute;
    }
}