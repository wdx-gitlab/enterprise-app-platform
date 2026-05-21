package com.ruijie.authzengine.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruijie.authzengine.domain.model.common.SubjectKey;
import com.ruijie.authzengine.domain.model.decision.AuthzContext;
import com.ruijie.authzengine.domain.model.decision.AuthzDecision;
import com.ruijie.authzengine.domain.model.decision.AuthzRequest;
import com.ruijie.authzengine.domain.model.decision.AuthzResource;
import com.ruijie.authzengine.domain.model.decision.AuthzSubject;
import com.ruijie.authzengine.domain.model.decision.DataScopeFragment;
import com.ruijie.authzengine.domain.model.decision.DecisionType;
import com.ruijie.authzengine.domain.model.decision.PermissionGrant;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PermissionDecisionServiceTest {

    @Test
    void shouldNotTreatFieldPolicyAsBooleanGate() {
        PermissionDecisionService decisionService = buildDecisionService();

        AuthzRequest request = AuthzRequest.builder()
            .tenantId("T001")
            .appCode("CRM")
            .subject(AuthzSubject.builder().id("U100").type("SUB_USER").build())
            .resource(AuthzResource.builder().resourceType("RES_DATA_BO").resId("1").build())
            .action("READ")
            .context(Collections.emptyMap())
            .build();

        AuthzContext context = AuthzContext.builder()
            .subjectKeys(Collections.singleton(new SubjectKey("SUB_USER", "U100")))
            .attributes(new LinkedHashMap<>())
            .governanceAttributes(new LinkedHashMap<>())
            .build();
        context.getGovernanceAttributes().put("subjectRegistered", true);
        context.getGovernanceAttributes().put("resourceRegistered", true);
        context.getGovernanceAttributes().put("actionRegistered", true);
        context.getGovernanceAttributes().put("attributes", Collections.singletonList(buildAttribute("mobile", "mobile", "mobile", "STRING", true)));

        PermissionGrant fieldGrant = PermissionGrant.builder()
            .assignmentId(1L)
            .tenantId("T001")
            .appCode("CRM")
            .subjectType("SUB_USER")
            .subjectId("U100")
            .action("READ")
            .permItemId(10L)
            .permissionCode("CRM:bo:CONTRACT:READ")
            .resourceType("RES_DATA_BO")
            .resId("1")
            .policyTemplateCode("FIELD_MASK_MOBILE")
            .policyTemplateType("FIELD")
            .policyTemplateStatus("ENABLED")
            .paramSchema("{\"action\":\"MASK\"}")
            .policyParams("{\"targetField\":\"mobile\"}")
            .build();

        AuthzDecision decision = decisionService.evaluate(request, context, Arrays.asList(fieldGrant));

        Assertions.assertEquals(DecisionType.PERMIT, decision.getDecision());
        Assertions.assertEquals(Collections.singletonList("CRM:bo:CONTRACT:READ"), decision.getMatchedPermissions());
        Assertions.assertEquals(Collections.singletonList("FIELD_MASK_MOBILE"), decision.getMatchedPolicyTemplateCodes());
        Assertions.assertTrue(decision.getObligations().containsKey("fieldControls"));
    }

    @Test
    void shouldProduceRowFilterForDataPolicy() {
        PermissionDecisionService decisionService = buildDecisionService(
            fixedDataEvaluator("biz_customer.owner_id = ?", Collections.<Object>singletonList("U100"))
        );
        PermissionGrant dataGrant = PermissionGrant.builder()
            .assignmentId(2L)
            .tenantId("T001")
            .appCode("CRM")
            .subjectType("SUB_USER")
            .subjectId("U100")
            .action("READ")
            .permissionCode("CRM:bo:CUSTOMER:READ")
            .resourceType("RES_DATA_BO")
            .resId("1")
            .policyTemplateCode("DATA_OWNER_ONLY")
            .policyTemplateType("DATA")
            .policyTemplateStatus("ENABLED")
            .expressionScript("#tableName + '.owner_id = ' + param(#sub['userId'])")
            .failStrategy("DENY")
            .build();

        AuthzDecision decision = decisionService.evaluate(buildRequest(), buildContext(), Collections.singletonList(dataGrant));

        Assertions.assertEquals(DecisionType.PERMIT, decision.getDecision());
        @SuppressWarnings("unchecked")
        Map<String, Object> rowFilter = (Map<String, Object>) decision.getObligations().get("rowFilter");
        Assertions.assertEquals("(biz_customer.owner_id = ?)", rowFilter.get("whereClause"));
        Assertions.assertEquals(Collections.singletonList("U100"), rowFilter.get("params"));
    }

    @Test
    void shouldAllowWhenDataPolicyFailsAndFailStrategyIsAllow() {
        PermissionDecisionService decisionService = buildDecisionService(
            throwingDataEvaluator("DATA_SCOPE_TIMEOUT")
        );
        PermissionGrant dataGrant = PermissionGrant.builder()
            .assignmentId(3L)
            .tenantId("T001")
            .appCode("CRM")
            .subjectType("SUB_USER")
            .subjectId("U100")
            .action("READ")
            .permissionCode("CRM:bo:CUSTOMER:READ")
            .resourceType("RES_DATA_BO")
            .resId("1")
            .policyTemplateCode("DATA_OWNER_ONLY")
            .policyTemplateType("DATA")
            .policyTemplateStatus("ENABLED")
            .expressionScript("T(java.lang.Math).abs(-1)")
            .failStrategy("ALLOW")
            .build();

        AuthzDecision decision = decisionService.evaluate(buildRequest(), buildContext(), Collections.singletonList(dataGrant));

        Assertions.assertEquals(DecisionType.PERMIT, decision.getDecision());
        Assertions.assertFalse(decision.getObligations().containsKey("rowFilter"));
    }

    @Test
    void shouldDenyWhenDataPolicyFailsAndFailStrategyIsDeny() {
        PermissionDecisionService decisionService = buildDecisionService(
            throwingDataEvaluator("DATA_SCOPE_TIMEOUT")
        );
        PermissionGrant dataGrant = PermissionGrant.builder()
            .assignmentId(4L)
            .tenantId("T001")
            .appCode("CRM")
            .subjectType("SUB_USER")
            .subjectId("U100")
            .action("READ")
            .permissionCode("CRM:bo:CUSTOMER:READ")
            .resourceType("RES_DATA_BO")
            .resId("1")
            .policyTemplateCode("DATA_OWNER_ONLY")
            .policyTemplateType("DATA")
            .policyTemplateStatus("ENABLED")
            .expressionScript("T(java.lang.Math).abs(-1)")
            .failStrategy("DENY")
            .build();

        AuthzDecision decision = decisionService.evaluate(buildRequest(), buildContext(), Collections.singletonList(dataGrant));

        Assertions.assertEquals(DecisionType.NOT_PERMIT, decision.getDecision());
        Assertions.assertEquals("POLICY_DENIED", decision.getReason());
    }

    @Test
    void shouldDenyWhenGlobalBooleanGateFailsBeforeApplyingDataGrant() {
        PermissionDecisionService decisionService = buildDecisionService();
        PermissionGrant envGrant = PermissionGrant.builder()
            .assignmentId(40L)
            .tenantId("T001")
            .appCode("CRM")
            .subjectType("SUB_USER")
            .subjectId("U100")
            .action("READ")
            .permissionCode("CRM:bo:CUSTOMER:READ")
            .resourceType("RES_DATA_BO")
            .resId("1")
            .policyTemplateCode("ENV_MONDAY_ONLY")
            .policyTemplateType("ENV")
            .policyTemplateStatus("ENABLED")
            .expressionScript("#env['dayOfWeek'] == 1")
            .build();
        PermissionGrant dataGrant = PermissionGrant.builder()
            .assignmentId(41L)
            .tenantId("T001")
            .appCode("CRM")
            .subjectType("SUB_USER")
            .subjectId("U100")
            .action("READ")
            .permissionCode("CRM:bo:CUSTOMER:READ")
            .resourceType("RES_DATA_BO")
            .resId("1")
            .policyTemplateCode("DATA_OWNER_ONLY")
            .policyTemplateType("DATA")
            .policyTemplateStatus("ENABLED")
            .expressionScript("#tableName + '.owner_id = ' + param(#sub['userId'])")
            .failStrategy("DENY")
            .build();

        AuthzDecision decision = decisionService.evaluate(
            buildRequest(),
            buildContext(Collections.singletonMap("dayOfWeek", 2)),
            Arrays.asList(envGrant, dataGrant)
        );

        Assertions.assertEquals(DecisionType.NOT_PERMIT, decision.getDecision());
        Assertions.assertEquals("POLICY_DENIED", decision.getReason());
        Assertions.assertTrue(decision.getObligations().isEmpty());
    }

    @Test
    void shouldDenyWhenGlobalBooleanGateFailsBeforeApplyingFieldGrant() {
        PermissionDecisionService decisionService = buildDecisionService();
        PermissionGrant envGrant = PermissionGrant.builder()
            .assignmentId(42L)
            .tenantId("T001")
            .appCode("CRM")
            .subjectType("SUB_USER")
            .subjectId("U100")
            .action("READ")
            .permissionCode("CRM:bo:CUSTOMER:READ")
            .resourceType("RES_DATA_BO")
            .resId("1")
            .policyTemplateCode("ENV_MONDAY_ONLY")
            .policyTemplateType("ENV")
            .policyTemplateStatus("ENABLED")
            .expressionScript("#env['dayOfWeek'] == 1")
            .build();
        PermissionGrant fieldGrant = PermissionGrant.builder()
            .assignmentId(43L)
            .tenantId("T001")
            .appCode("CRM")
            .subjectType("SUB_USER")
            .subjectId("U100")
            .action("READ")
            .permissionCode("CRM:bo:CUSTOMER:READ")
            .resourceType("RES_DATA_BO")
            .resId("1")
            .policyTemplateCode("FIELD_MASK_MOBILE")
            .policyTemplateType("FIELD")
            .policyTemplateStatus("ENABLED")
            .paramSchema("{\"action\":\"MASK\"}")
            .policyParams("{\"targetField\":\"mobile\"}")
            .build();

        AuthzDecision decision = decisionService.evaluate(
            buildRequest(),
            buildContext(Collections.singletonMap("dayOfWeek", 3)),
            Arrays.asList(envGrant, fieldGrant)
        );

        Assertions.assertEquals(DecisionType.NOT_PERMIT, decision.getDecision());
        Assertions.assertEquals("POLICY_DENIED", decision.getReason());
        Assertions.assertTrue(decision.getObligations().isEmpty());
    }

    @Test
    void shouldReturnIndeterminateWhenGlobalBooleanGateErrors() {
        PermissionDecisionService decisionService = buildDecisionService();
        PermissionGrant plainGrant = PermissionGrant.builder()
            .assignmentId(44L)
            .tenantId("T001")
            .appCode("CRM")
            .subjectType("SUB_USER")
            .subjectId("U100")
            .action("READ")
            .permissionCode("CRM:bo:CUSTOMER:READ")
            .resourceType("RES_DATA_BO")
            .resId("1")
            .build();
        PermissionGrant envGrant = PermissionGrant.builder()
            .assignmentId(45L)
            .tenantId("T001")
            .appCode("CRM")
            .subjectType("SUB_USER")
            .subjectId("U100")
            .action("READ")
            .permissionCode("CRM:bo:CUSTOMER:READ")
            .resourceType("RES_DATA_BO")
            .resId("1")
            .policyTemplateCode("ENV_INVALID")
            .policyTemplateType("ENV")
            .policyTemplateStatus("ENABLED")
            .expressionScript("#env['dayOfWeek'].substring(0, 1) == '1'")
            .build();

        AuthzDecision decision = decisionService.evaluate(
            buildRequest(),
            buildContext(Collections.singletonMap("dayOfWeek", 1)),
            Arrays.asList(plainGrant, envGrant)
        );

        Assertions.assertEquals(DecisionType.INDETERMINATE, decision.getDecision());
        Assertions.assertEquals("POLICY_EVALUATION_ERROR", decision.getReason());
    }

    @Test
    void shouldChooseMostRestrictiveFieldActionWhenMultiplePoliciesHitSameField() {
        PermissionDecisionService decisionService = buildDecisionService();
        PermissionGrant openGrant = PermissionGrant.builder()
            .assignmentId(5L)
            .tenantId("T001")
            .appCode("CRM")
            .subjectType("SUB_USER")
            .subjectId("U100")
            .action("READ")
            .permissionCode("CRM:bo:CUSTOMER:READ")
            .resourceType("RES_DATA_BO")
            .resId("1")
            .policyTemplateCode("FIELD_OPEN_MOBILE")
            .policyTemplateType("FIELD")
            .policyTemplateStatus("ENABLED")
            .paramSchema("{\"action\":\"OPEN\"}")
            .policyParams("{\"targetField\":\"mobile\"}")
            .build();
        PermissionGrant hideGrant = PermissionGrant.builder()
            .assignmentId(6L)
            .tenantId("T001")
            .appCode("CRM")
            .subjectType("SUB_USER")
            .subjectId("U100")
            .action("READ")
            .permissionCode("CRM:bo:CUSTOMER:READ")
            .resourceType("RES_DATA_BO")
            .resId("1")
            .policyTemplateCode("FIELD_HIDE_MOBILE")
            .policyTemplateType("FIELD")
            .policyTemplateStatus("ENABLED")
            .paramSchema("{\"action\":\"HIDE\"}")
            .policyParams("{\"targetField\":\"mobile\"}")
            .build();

        AuthzDecision decision = decisionService.evaluate(buildRequest(), buildContext(), Arrays.asList(openGrant, hideGrant));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fieldControls = (List<Map<String, Object>>) decision.getObligations().get("fieldControls");
        Assertions.assertEquals(1, fieldControls.size());
        Assertions.assertEquals("HIDE", fieldControls.get(0).get("action"));
    }

    @Test
    void shouldRejectWhenFieldPolicyTargetsUnknownField() {
        PermissionDecisionService decisionService = buildDecisionService();
        PermissionGrant fieldGrant = PermissionGrant.builder()
            .assignmentId(7L)
            .tenantId("T001")
            .appCode("CRM")
            .subjectType("SUB_USER")
            .subjectId("U100")
            .action("READ")
            .permissionCode("CRM:bo:CUSTOMER:READ")
            .resourceType("RES_DATA_BO")
            .resId("1")
            .policyTemplateCode("FIELD_HIDE_UNKNOWN")
            .policyTemplateType("FIELD")
            .policyTemplateStatus("ENABLED")
            .paramSchema("{\"action\":\"HIDE\"}")
            .policyParams("{\"targetField\":\"unknownField\"}")
            .build();

        AuthzDecision decision = decisionService.evaluate(buildRequest(), buildContext(), Collections.singletonList(fieldGrant));

        Assertions.assertEquals(DecisionType.NOT_PERMIT, decision.getDecision());
        Assertions.assertEquals("INVALID_FIELD_POLICY", decision.getReason());
    }

    @Test
    void shouldPermitWhenAnyEnvPolicyPasses() {
        PermissionDecisionService decisionService = buildDecisionService();
        PermissionGrant envTuesdayGrant = PermissionGrant.builder()
            .assignmentId(8L)
            .tenantId("T001")
            .appCode("CRM")
            .subjectType("SUB_USER")
            .subjectId("U100")
            .action("READ")
            .permissionCode("CRM:bo:CUSTOMER:READ")
            .resourceType("RES_DATA_BO")
            .resId("1")
            .policyTemplateCode("ENV_WEDNESDAY_ONLY")
            .policyTemplateType("ENV")
            .policyTemplateStatus("ENABLED")
            .expressionScript("#env['dayOfWeek'] == 3")
            .build();
        PermissionGrant envHourGrant = PermissionGrant.builder()
            .assignmentId(9L)
            .tenantId("T001")
            .appCode("CRM")
            .subjectType("SUB_USER")
            .subjectId("U100")
            .action("READ")
            .permissionCode("CRM:bo:CUSTOMER:READ")
            .resourceType("RES_DATA_BO")
            .resId("1")
            .policyTemplateCode("ENV_AFTER_TEN")
            .policyTemplateType("ENV")
            .policyTemplateStatus("ENABLED")
            .expressionScript("#env['hour'] > 10")
            .build();

        AuthzDecision decision = decisionService.evaluate(
            buildRequest(),
            buildContext(buildEnv(2, 11)),
            Arrays.asList(envTuesdayGrant, envHourGrant)
        );

        Assertions.assertEquals(DecisionType.PERMIT, decision.getDecision());
    }

    @Test
    void shouldDenyWhenAllEnvPoliciesFail() {
        PermissionDecisionService decisionService = buildDecisionService();
        PermissionGrant envWednesdayGrant = PermissionGrant.builder()
            .assignmentId(10L)
            .tenantId("T001")
            .appCode("CRM")
            .subjectType("SUB_USER")
            .subjectId("U100")
            .action("READ")
            .permissionCode("CRM:bo:CUSTOMER:READ")
            .resourceType("RES_DATA_BO")
            .resId("1")
            .policyTemplateCode("ENV_WEDNESDAY_ONLY")
            .policyTemplateType("ENV")
            .policyTemplateStatus("ENABLED")
            .expressionScript("#env['dayOfWeek'] == 3")
            .build();
        PermissionGrant envAfterNoonGrant = PermissionGrant.builder()
            .assignmentId(11L)
            .tenantId("T001")
            .appCode("CRM")
            .subjectType("SUB_USER")
            .subjectId("U100")
            .action("READ")
            .permissionCode("CRM:bo:CUSTOMER:READ")
            .resourceType("RES_DATA_BO")
            .resId("1")
            .policyTemplateCode("ENV_AFTER_NOON")
            .policyTemplateType("ENV")
            .policyTemplateStatus("ENABLED")
            .expressionScript("#env['hour'] > 12")
            .build();

        AuthzDecision decision = decisionService.evaluate(
            buildRequest(),
            buildContext(buildEnv(2, 11)),
            Arrays.asList(envWednesdayGrant, envAfterNoonGrant)
        );

        Assertions.assertEquals(DecisionType.NOT_PERMIT, decision.getDecision());
        Assertions.assertEquals("POLICY_DENIED", decision.getReason());
    }

    @Test
    void shouldPermitWhenAnyStatePolicyPassesAfterEnvPasses() {
        PermissionDecisionService decisionService = buildDecisionService();
        PermissionGrant envGrant = PermissionGrant.builder()
            .assignmentId(12L)
            .tenantId("T001")
            .appCode("CRM")
            .subjectType("SUB_USER")
            .subjectId("U100")
            .action("READ")
            .permissionCode("CRM:bo:CUSTOMER:READ")
            .resourceType("RES_DATA_BO")
            .resId("1")
            .policyTemplateCode("ENV_TUESDAY_ONLY")
            .policyTemplateType("ENV")
            .policyTemplateStatus("ENABLED")
            .expressionScript("#env['dayOfWeek'] == 2")
            .build();
        PermissionGrant stateClosedGrant = PermissionGrant.builder()
            .assignmentId(13L)
            .tenantId("T001")
            .appCode("CRM")
            .subjectType("SUB_USER")
            .subjectId("U100")
            .action("READ")
            .permissionCode("CRM:bo:CUSTOMER:READ")
            .resourceType("RES_DATA_BO")
            .resId("1")
            .policyTemplateCode("STATE_AFTER_NOON")
            .policyTemplateType("STATE")
            .policyTemplateStatus("ENABLED")
            .expressionScript("#env['hour'] > 12")
            .build();
        PermissionGrant stateOpenGrant = PermissionGrant.builder()
            .assignmentId(14L)
            .tenantId("T001")
            .appCode("CRM")
            .subjectType("SUB_USER")
            .subjectId("U100")
            .action("READ")
            .permissionCode("CRM:bo:CUSTOMER:READ")
            .resourceType("RES_DATA_BO")
            .resId("1")
            .policyTemplateCode("STATE_AFTER_TEN")
            .policyTemplateType("STATE")
            .policyTemplateStatus("ENABLED")
            .expressionScript("#env['hour'] > 10")
            .build();

        AuthzDecision decision = decisionService.evaluate(
            buildRequest(),
            buildContext(buildEnv(2, 11)),
            Arrays.asList(envGrant, stateClosedGrant, stateOpenGrant)
        );

        Assertions.assertEquals(DecisionType.PERMIT, decision.getDecision());
    }

    @Test
    void shouldJoinMultipleDataPoliciesWithOr() {
        PermissionDecisionService decisionService = buildDecisionService(
            new DataScopeExpressionEvaluator() {
                @Override
                public DataScopeFragment evaluate(String expressionScript, Map<String, Object> sub, String tableName,
                                                  List<Map<String, Object>> attributes, Map<String, Object> param) {
                    if ("DATA_DEPT".equals(expressionScript)) {
                        return new DataScopeFragment("biz_customer.dept_id = ?", Collections.<Object>singletonList("D001"));
                    }
                    if ("DATA_OWNER".equals(expressionScript)) {
                        return new DataScopeFragment("biz_customer.owner_id = ?", Collections.<Object>singletonList("U100"));
                    }
                    return DataScopeFragment.empty();
                }
            }
        );
        PermissionGrant dataDeptGrant = PermissionGrant.builder()
            .assignmentId(15L)
            .tenantId("T001")
            .appCode("CRM")
            .subjectType("SUB_USER")
            .subjectId("U100")
            .action("READ")
            .permissionCode("CRM:bo:CUSTOMER:READ")
            .resourceType("RES_DATA_BO")
            .resId("1")
            .policyTemplateCode("DATA_DEPT")
            .policyTemplateType("DATA")
            .policyTemplateStatus("ENABLED")
            .expressionScript("DATA_DEPT")
            .build();
        PermissionGrant dataOwnerGrant = PermissionGrant.builder()
            .assignmentId(16L)
            .tenantId("T001")
            .appCode("CRM")
            .subjectType("SUB_USER")
            .subjectId("U100")
            .action("READ")
            .permissionCode("CRM:bo:CUSTOMER:READ")
            .resourceType("RES_DATA_BO")
            .resId("1")
            .policyTemplateCode("DATA_OWNER")
            .policyTemplateType("DATA")
            .policyTemplateStatus("ENABLED")
            .expressionScript("DATA_OWNER")
            .build();

        AuthzDecision decision = decisionService.evaluate(
            buildRequest(),
            buildContext(),
            Arrays.asList(dataDeptGrant, dataOwnerGrant)
        );

        Assertions.assertEquals(DecisionType.PERMIT, decision.getDecision());
        @SuppressWarnings("unchecked")
        Map<String, Object> rowFilter = (Map<String, Object>) decision.getObligations().get("rowFilter");
        Assertions.assertEquals("(biz_customer.dept_id = ?) OR (biz_customer.owner_id = ?)", rowFilter.get("whereClause"));
        Assertions.assertEquals(Arrays.asList("D001", "U100"), rowFilter.get("params"));
    }

    private PermissionDecisionService buildDecisionService() {
        return buildDecisionService(new DataScopeExpressionEvaluator());
    }

    private PermissionDecisionService buildDecisionService(DataScopeExpressionEvaluator dataScopeExpressionEvaluator) {
        return new PermissionDecisionService(
            new PolicyExpressionEvaluator(),
            dataScopeExpressionEvaluator,
            new ObjectMapper()
        );
    }

    private DataScopeExpressionEvaluator fixedDataEvaluator(String sql, List<Object> params) {
        return new DataScopeExpressionEvaluator() {
            @Override
            public DataScopeFragment evaluate(String expressionScript, Map<String, Object> sub, String tableName,
                                              List<Map<String, Object>> attributes, Map<String, Object> param) {
                return new DataScopeFragment(sql, params);
            }
        };
    }

    private DataScopeExpressionEvaluator throwingDataEvaluator(String message) {
        return new DataScopeExpressionEvaluator() {
            @Override
            public DataScopeFragment evaluate(String expressionScript, Map<String, Object> sub, String tableName,
                                              List<Map<String, Object>> attributes, Map<String, Object> param) {
                throw new RuntimeException(message);
            }
        };
    }

    private AuthzRequest buildRequest() {
        return AuthzRequest.builder()
            .tenantId("T001")
            .appCode("CRM")
            .subject(AuthzSubject.builder().id("U100").type("SUB_USER").build())
            .resource(AuthzResource.builder().resourceType("RES_DATA_BO").resId("1").build())
            .action("READ")
            .context(Collections.emptyMap())
            .build();
    }

    private AuthzContext buildContext() {
        return buildContext(Collections.emptyMap());
    }

    private AuthzContext buildContext(Map<String, Object> env) {
        LinkedHashMap<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("sub", Collections.singletonMap("userId", "U100"));
        attributes.put("env", env == null ? Collections.emptyMap() : env);
        AuthzContext context = AuthzContext.builder()
            .subjectKeys(Collections.singleton(new SubjectKey("SUB_USER", "U100")))
            .attributes(attributes)
            .governanceAttributes(new LinkedHashMap<>())
            .build();
        context.getGovernanceAttributes().put("subjectRegistered", true);
        context.getGovernanceAttributes().put("resourceRegistered", true);
        context.getGovernanceAttributes().put("actionRegistered", true);
        context.getGovernanceAttributes().put("tableName", "biz_customer");
        context.getGovernanceAttributes().put("attributes", Collections.singletonList(buildAttribute("mobile", "mobile", "mobile", "STRING", true)));
        return context;
    }

    private Map<String, Object> buildEnv(int dayOfWeek, int hour) {
        Map<String, Object> env = new LinkedHashMap<>();
        env.put("dayOfWeek", dayOfWeek);
        env.put("hour", hour);
        return env;
    }

    private Map<String, Object> buildAttribute(String code, String fieldName, String columnName, String type, boolean fieldControl) {
        Map<String, Object> attribute = new LinkedHashMap<>();
        attribute.put("code", code);
        attribute.put("fieldName", fieldName);
        attribute.put("columnName", columnName);
        attribute.put("type", type);
        attribute.put("fieldControl", fieldControl);
        return attribute;
    }
}