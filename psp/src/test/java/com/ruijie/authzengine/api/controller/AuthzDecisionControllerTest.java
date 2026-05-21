package com.ruijie.authzengine.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruijie.authzengine.api.assembler.AuthzRequestAssembler;
import com.ruijie.authzengine.application.service.AuthzContractAppService;
import com.ruijie.authzengine.application.service.AuthzDecisionAppService;
import com.ruijie.authzengine.application.service.AuthzFacade;
import com.ruijie.authzengine.domain.model.common.SubjectKey;
import com.ruijie.authzengine.domain.model.decision.AuthzContext;
import com.ruijie.authzengine.domain.model.decision.PermissionGrant;
import com.ruijie.authzengine.domain.repository.AuthorizationPolicyRepository;
import com.ruijie.authzengine.domain.service.PermissionDecisionService;
import com.ruijie.authzengine.domain.service.PolicyInformationPoint;
import com.ruijie.authzengine.domain.service.SubjectExpansionService;
import com.ruijie.authzengine.infrastructure.authz.DefaultPolicyDecisionPoint;
import com.ruijie.authzengine.infrastructure.authz.DefaultPolicyEnforcementPoint;
import com.ruijie.authzengine.infrastructure.authz.DefaultPolicyInformationPoint;
import com.ruijie.authzengine.infrastructure.authz.InMemoryAuthorizationPolicyRepository;
import com.ruijie.authzengine.shared.exception.AuthzIntegrationException;
import com.ruijie.authzengine.shared.exception.GlobalExceptionHandler;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthzDecisionControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = buildMockMvc(buildController(new DefaultPolicyInformationPoint(new SubjectExpansionService())));
    }

    @Test
    void shouldWrapDecisionInApiResponse() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("tenantId", "T001");
        payload.put("appCode", "CRM");
        payload.put("action", "APPROVE");
        payload.put("subject", buildSubject("demo-user", "SUB_USER"));
        payload.put("resource", buildResource("RES_DATA_BO", "101"));

        mockMvc.perform(post("/authz-engine/api/v1/authz/check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.message").value("成功"))
            .andExpect(jsonPath("$.data.decision").value("PERMIT"))
                .andExpect(jsonPath("$.data.reason").value("PERMIT"))
                .andExpect(jsonPath("$.data.matchedPermissionCodes[0]").value("CONTRACT_APPROVE"))
                .andExpect(jsonPath("$.data.obligations").isMap());
    }

    @Test
    void shouldReturnValidationErrorWhenRequestInvalid() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("tenantId", "");
        payload.put("appCode", "CRM");
        payload.put("action", "APPROVE");
        payload.put("subject", buildSubject("demo-user", "SUB_USER"));
        payload.put("resource", buildResource("RES_DATA_BO", "101"));

        mockMvc.perform(post("/authz-engine/api/v1/authz/check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("AUTHZ-400"))
            .andExpect(jsonPath("$.message", containsString("租户标识不能为空")));
    }

    @Test
    void shouldReturnSystemErrorWhenPepHitsUnexpectedException() throws Exception {
        PolicyInformationPoint brokenPip = request -> {
            throw new IllegalStateException("boom");
        };
        MockMvc brokenMockMvc = buildMockMvc(buildController(brokenPip));

        Map<String, Object> payload = new HashMap<>();
        payload.put("tenantId", "T001");
        payload.put("appCode", "CRM");
        payload.put("action", "APPROVE");
        payload.put("subject", buildSubject("demo-user", "SUB_USER"));
        payload.put("resource", buildResource("RES_DATA_BO", "101"));

        brokenMockMvc.perform(post("/authz-engine/api/v1/authz/check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("AUTHZ-500"))
            .andExpect(jsonPath("$.message").value("鉴权执行失败"));
    }

            @Test
            void shouldReturnObligationsContractForDataAndFieldPolicies() throws Exception {
            MockMvc obligationMockMvc = buildMockMvc(buildController(buildObligationPip(), buildObligationRepository()));

            Map<String, Object> payload = new HashMap<>();
            payload.put("tenantId", "T001");
            payload.put("appCode", "CRM");
            payload.put("action", "READ");
            payload.put("subject", buildSubject("demo-user", "SUB_USER"));
            payload.put("resource", buildResource("RES_DATA_BO", "101"));

            obligationMockMvc.perform(post("/authz-engine/api/v1/authz/check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.decision").value("PERMIT"))
                .andExpect(jsonPath("$.data.obligations.rowFilter.whereClause").value("(biz_customer.owner_id = ?)"))
                .andExpect(jsonPath("$.data.obligations.rowFilter.params[0]").value("demo-user"))
                .andExpect(jsonPath("$.data.obligations.fieldControls[0].action").value("MASK"))
                .andExpect(jsonPath("$.data.obligations.fieldControls[0].policyCode").value("FIELD_MASK_MOBILE"));
            }

            @Test
            void shouldAcceptOpenApiFieldNamesForSingleCheck() throws Exception {
            Map<String, Object> payload = new HashMap<>();
            payload.put("tenantId", "T001");
            payload.put("appCode", "CRM");
            payload.put("action", "APPROVE");
            payload.put("traceId", "TRACE-TEST-001");
            payload.put("subject", buildSubject("demo-user", "SUB_USER"));
            payload.put("resource", buildResource("RES_DATA_BO", "101"));

            mockMvc.perform(post("/authz-engine/api/v1/authz/check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.decision").value("PERMIT"));
            }

            @Test
            void shouldReturnContractOnlyForBatchCheck() throws Exception {
            Map<String, Object> single = new HashMap<>();
            single.put("tenantId", "T001");
            single.put("appCode", "CRM");
            single.put("action", "APPROVE");
            single.put("subject", buildSubject("demo-user", "SUB_USER"));
            single.put("resource", buildResource("RES_DATA_BO", "101"));

            Map<String, Object> payload = new HashMap<>();
            payload.put("requests", java.util.Collections.singletonList(single));
            payload.put("aggregationMode", "INDEPENDENT");

            mockMvc.perform(post("/authz-engine/api/v1/authz/batch-check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.capabilityStatus").value("CONTRACT_ONLY"));
            }

            @Test
            void shouldReturnContractOnlyForDataScopeResolve() throws Exception {
            Map<String, Object> payload = new HashMap<>();
            payload.put("tenantId", "T001");
            payload.put("appCode", "CRM");
            payload.put("policyTemplateCode", "DATA_SCOPE_DEPT");
            payload.put("subject", buildSubject("demo-user", "SUB_USER"));
            payload.put("resource", buildResource("RES_DATA_BO", "101"));

            mockMvc.perform(post("/authz-engine/api/v1/authz/data-scope/resolve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.capabilityStatus").value("CONTRACT_ONLY"));
            }

    private MockMvc buildMockMvc(AuthzDecisionController controller) {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        return MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setValidator(validator)
            .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
            .build();
    }

    private AuthzDecisionController buildController(PolicyInformationPoint policyInformationPoint) {
        return buildController(policyInformationPoint, new InMemoryAuthorizationPolicyRepository());
    }

    private AuthzDecisionController buildController(
        PolicyInformationPoint policyInformationPoint,
        AuthorizationPolicyRepository repository
    ) {
        DefaultPolicyDecisionPoint pdp = new DefaultPolicyDecisionPoint(policyInformationPoint, repository, new PermissionDecisionService(null, new com.fasterxml.jackson.databind.ObjectMapper()));
        DefaultPolicyEnforcementPoint pep = new DefaultPolicyEnforcementPoint(pdp);
        AuthzDecisionAppService appService = new AuthzDecisionAppService(new AuthzFacade(pep, null));
        return new AuthzDecisionController(appService, new AuthzContractAppService(), new AuthzRequestAssembler());
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
                        .build(),
                    PermissionGrant.builder()
                        .assignmentId(32L)
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

    private Map<String, Object> buildSubject(String id, String type) {
        Map<String, Object> subject = new HashMap<>();
        subject.put("subjectId", id);
        subject.put("subjectModel", type);
        return subject;
    }

    private Map<String, Object> buildResource(String type, String resourceId) {
        Map<String, Object> resource = new HashMap<>();
        resource.put("resourceModel", type);
        resource.put("resourceId", resourceId);
        return resource;
    }

    // ---- T018：Hook 集成错误仍保持统一 ApiResponse<T>/错误码语义 ----

    @Test
    void shouldReturnIndeterminateNotSystemErrorWhenPipThrowsAuthzIntegrationException() throws Exception {
        // AuthzIntegrationException 被 PEP 捕获并降级为 INDETERMINATE，而非 AUTHZ-500 系统错误
        PolicyInformationPoint hookErrorPip = request -> {
            throw new AuthzIntegrationException("未找到业务对象 Hook Bean: missingProvider");
        };
        MockMvc hookErrorMockMvc = buildMockMvc(buildController(hookErrorPip));

        Map<String, Object> payload = new HashMap<>();
        payload.put("tenantId", "T001");
        payload.put("appCode", "CRM");
        payload.put("action", "APPROVE");
        payload.put("subject", buildSubject("demo-user", "SUB_USER"));
        payload.put("resource", buildResource("RES_DATA_BO", "101"));

        hookErrorMockMvc.perform(post("/authz-engine/api/v1/authz/check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.decision").value("INDETERMINATE"));
    }

    @Test
    void shouldDistinguishIntegrationErrorFromSystemErrorInApiResponse() throws Exception {
        // AuthzIntegrationException -> INDETERMINATE（code=0，内部降级），而非普通运行时异常 AUTHZ-500
        PolicyInformationPoint integrationErrorPip = request -> {
            throw new AuthzIntegrationException("Hook 集成错误");
        };
        PolicyInformationPoint runtimeErrorPip = request -> {
            throw new IllegalStateException("unexpected runtime failure");
        };
        MockMvc integrationMockMvc = buildMockMvc(buildController(integrationErrorPip));
        MockMvc runtimeMockMvc = buildMockMvc(buildController(runtimeErrorPip));

        Map<String, Object> payload = new HashMap<>();
        payload.put("tenantId", "T001");
        payload.put("appCode", "CRM");
        payload.put("action", "APPROVE");
        payload.put("subject", buildSubject("demo-user", "SUB_USER"));
        payload.put("resource", buildResource("RES_DATA_BO", "101"));

        // Hook 集成错误：AuthzIntegrationException 被 PEP 捕获，返回 INDETERMINATE（不是 500）
        integrationMockMvc.perform(post("/authz-engine/api/v1/authz/check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.decision").value("INDETERMINATE"));

        // 非预期运行时异常：由 GlobalExceptionHandler 捕获，返回 AUTHZ-500
        runtimeMockMvc.perform(post("/authz-engine/api/v1/authz/check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("AUTHZ-500"));
    }
}