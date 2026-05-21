package com.ruijie.authzengine;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruijie.authzengine.application.service.AuthzFacade;
import com.ruijie.authzengine.application.spi.AuthMetaModelAdapter;
import com.ruijie.authzengine.application.spi.AuthMetaModelRuntimeContext;
import com.ruijie.authzengine.application.spi.AuthMetaModelRuntimeContextHolder;
import com.ruijie.authzengine.application.spi.BoMetaModelAdapter;
import com.ruijie.authzengine.application.spi.BoMetaModelRuntimeContext;
import com.ruijie.authzengine.application.spi.BoMetaModelRuntimeContextHolder;
import com.ruijie.authzengine.application.spi.DataItem;
import com.ruijie.authzengine.application.spi.ModelCode;
import com.ruijie.authzengine.application.spi.SubjectHookResult;
import com.ruijie.authzengine.domain.model.decision.AuthzDecision;
import com.ruijie.authzengine.domain.model.decision.DecisionType;
import com.ruijie.authzengine.infrastructure.config.AuditMetaObjectHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spring 容器级集成测试，验证真实启动链路、Flyway 和 OpenAPI 暴露。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({
    AuthzEngineApplicationIntegrationTest.TestBoHookConfiguration.class,
    AuthzEngineApplicationIntegrationTest.TestSubjectHookConfiguration.class,
    AuthzEngineApplicationIntegrationTest.TestMenuShadowHookConfiguration.class
})
class AuthzEngineApplicationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AuthzFacade authzFacade;

    @Autowired
    private MetaObjectHandler metaObjectHandler;

    @Autowired
    private TestBoHookProbe testBoHookProbe;

    @Autowired
    private TestSubjectHookProbe testSubjectHookProbe;

    @Autowired
    private TestMenuShadowHookProbe testMenuShadowHookProbe;

    @Test
    void shouldLoadFlywaySchemaAndAuditHandler() {
        Integer permissionItemCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM authz_permission_item", Integer.class);

        Assertions.assertNotNull(permissionItemCount);
        Assertions.assertTrue(permissionItemCount >= 2);
        Assertions.assertTrue(metaObjectHandler instanceof AuditMetaObjectHandler);
    }

    @Test
    void shouldExposeOpenApiAndPermitRequestInSpringContext() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("tenantId", "T001");
        payload.put("appCode", "CRM");
        payload.put("action", "APPROVE");
        payload.put("subject", buildSubject("demo-user", "SUB_USER"));
        payload.put("resource", buildResource("RES_DATA_BO", "CONTRACT", "CONTRACT"));

        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paths['/authz-engine/api/v1/authz/check']").exists());

        mockMvc.perform(post("/authz-engine/api/v1/authz/check")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.decision").value("PERMIT"));
    }

    @Test
    void shouldReturnNotPermitInSpringContext() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("tenantId", "T001");
        payload.put("appCode", "CRM");
        payload.put("action", "DELETE");
        payload.put("subject", buildSubject("demo-user", "SUB_USER"));
        payload.put("resource", buildResource("RES_DATA_BO", "CONTRACT", "CONTRACT"));

        mockMvc.perform(post("/authz-engine/api/v1/authz/check")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.decision").value("NOT_PERMIT"))
            .andExpect(jsonPath("$.data.reason").value("NO_PERMISSION_ITEM"));
    }

            @Test
            void shouldPermitViaSubjectExpansionInSpringContext() throws Exception {
            Map<String, Object> payload = new HashMap<>();
            payload.put("tenantId", "T001");
            payload.put("appCode", "CRM");
            payload.put("action", "READ");
            payload.put("subject", buildSubject("demo-user", "SUB_USER"));
            payload.put("resource", buildResource("RES_DATA_BO", "CONTRACT", "CONTRACT"));

            mockMvc.perform(post("/authz-engine/api/v1/authz/check")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.decision").value("PERMIT"))
                .andExpect(jsonPath("$.data.matchedPermissionCodes[0]").value("CONTRACT_READ"));
            }

            @Test
            void shouldReturnSubjectNotRegisteredInSpringContext() throws Exception {
            Map<String, Object> payload = new HashMap<>();
            payload.put("tenantId", "T001");
            payload.put("appCode", "CRM");
            payload.put("action", "APPROVE");
            payload.put("subject", buildSubject("ghost-user", "SUB_USER"));
            payload.put("resource", buildResource("RES_DATA_BO", "CONTRACT", "CONTRACT"));

            mockMvc.perform(post("/authz-engine/api/v1/authz/check")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.decision").value("NOT_PERMIT"))
                .andExpect(jsonPath("$.data.reason").value("SUBJECT_NOT_REGISTERED"));
            }

            @Test
            void shouldReturnResourceNotRegisteredInSpringContext() throws Exception {
            Map<String, Object> payload = new HashMap<>();
            payload.put("tenantId", "T001");
            payload.put("appCode", "CRM");
            payload.put("action", "APPROVE");
            payload.put("subject", buildSubject("demo-user", "SUB_USER"));
            payload.put("resource", buildResource("RES_DATA_BO", "BO_NOT_REGISTERED_CASE", "BO_NOT_REGISTERED_CASE"));

            mockMvc.perform(post("/authz-engine/api/v1/authz/check")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.decision").value("NOT_PERMIT"))
                .andExpect(jsonPath("$.data.reason").value("RESOURCE_NOT_REGISTERED"));
            }

            @Test
            void shouldReturnActionNotRegisteredInSpringContext() throws Exception {
            Map<String, Object> payload = new HashMap<>();
            payload.put("tenantId", "T001");
            payload.put("appCode", "CRM");
            payload.put("action", "EXPORT");
            payload.put("subject", buildSubject("demo-user", "SUB_USER"));
            payload.put("resource", buildResource("RES_DATA_BO", "CONTRACT", "CONTRACT"));

            mockMvc.perform(post("/authz-engine/api/v1/authz/check")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.decision").value("NOT_PERMIT"))
                .andExpect(jsonPath("$.data.reason").value("ACTION_NOT_REGISTERED"));
            }

    @Test
    void shouldPermitWhenActionAliasMapsToStandardActionInSpringContext() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("tenantId", "T001");
        payload.put("appCode", "CRM");
        payload.put("action", "approve_request");
        payload.put("subject", buildSubject("demo-user", "SUB_USER"));
        payload.put("resource", buildResource("RES_DATA_BO", "CONTRACT", "CONTRACT"));

        mockMvc.perform(post("/authz-engine/api/v1/authz/check")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.decision").value("PERMIT"))
            .andExpect(jsonPath("$.data.matchedPermissionCodes[0]").value("CONTRACT_APPROVE"));
    }

    @Test
    void shouldPermitViaParentOrganizationAssignmentInSpringContext() throws Exception {
        jdbcTemplate.update(
            "INSERT INTO authz_bo_meta_model (id, tenant_id, app_code, bo_code, bo_name, schema_json, adapter_type, resolver, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            94000L, "T001", "CRM", "ORG_PARENT_CASE", "父组织继承测试对象", "{}", "JAVA_BEAN", "noopHook", "test", "test", 0
        );
        jdbcTemplate.update(
            "INSERT INTO dap_sys_org (id, tenant_id, app_code, department_code, department_name, parent_org_id, org_path, status, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            94001L, "T001", "CRM", "ORG-PARENT-US2", "父组织", null, "/ORG-PARENT-US2", "ENABLED", "test", "test", 0
        );
        jdbcTemplate.update(
            "INSERT INTO dap_sys_org (id, tenant_id, app_code, department_code, department_name, parent_org_id, org_path, status, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            94002L, "T001", "CRM", "ORG-CHILD-US2", "子组织", 94001L, "/ORG-PARENT-US2/ORG-CHILD-US2", "ENABLED", "test", "test", 0
        );
        jdbcTemplate.update(
            "INSERT INTO dap_sys_user (id, tenant_id, app_code, staff_no, user_id, staff_name, org_id, status, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            94003L, "T001", "CRM", "U-ORG-CHILD", "org-child-user", "组织子用户", 94002L, "ENABLED", "test", "test", 0
        );
        jdbcTemplate.update(
            "INSERT INTO authz_subject_relation (id, tenant_id, app_code, subject_model, subject_id, related_subject_model, related_subject_id, relation_type, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            94004L, "T001", "CRM", "SUB_USER", "org-child-user", "SUB_ORG", "ORG-CHILD-US2", "ORG", "test", "test", 0
        );
        jdbcTemplate.update(
            "INSERT INTO authz_permission_item (id, tenant_id, app_code, perm_code, res_model_code, res_id, act_code, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            94005L, "T001", "CRM", "ORG_PARENT_READ", "ORG_PARENT_CASE", "", "READ", "test", "test", 0
        );
        jdbcTemplate.update(
            "INSERT INTO authz_assignment (id, tenant_id, app_code, subject_id, subject_model, perm_item_id, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
            94006L, "T001", "CRM", "ORG-PARENT-US2", "SUB_ORG", 94005L, "test", "test", 0
        );

        Map<String, Object> payload = new HashMap<>();
        payload.put("tenantId", "T001");
        payload.put("appCode", "CRM");
        payload.put("action", "READ");
        payload.put("subject", buildSubject("org-child-user", "SUB_USER"));
        payload.put("resource", buildResource("RES_DATA_BO", "ORG_PARENT_CASE", "ORG_PARENT_CASE"));

        mockMvc.perform(post("/authz-engine/api/v1/authz/check")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.decision").value("PERMIT"))
            .andExpect(jsonPath("$.data.matchedPermissionCodes[0]").value("ORG_PARENT_READ"));
    }

    @Test
    void shouldReturnIndeterminateWhenHookFailsInSpringContext() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        Map<String, Object> context = new HashMap<>();
        context.put("simulateHookError", true);
        payload.put("tenantId", "T001");
        payload.put("appCode", "CRM");
        payload.put("action", "APPROVE");
        payload.put("subject", buildSubject("demo-user", "SUB_USER"));
        payload.put("resource", buildResource("RES_DATA_BO", "CONTRACT", "CONTRACT"));
        payload.put("context", context);

        mockMvc.perform(post("/authz-engine/api/v1/authz/check")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.decision").value("INDETERMINATE"))
            .andExpect(jsonPath("$.data.reason").value("HOOK_ERROR"));

        Assertions.assertNull(BoMetaModelRuntimeContextHolder.getCurrent());
    }

    @Test
    void shouldPermitViaSubjectShadowHookInSpringContext() throws Exception {
        testSubjectHookProbe.reset();
        jdbcTemplate.update(
            "INSERT INTO authz_meta_model (id, tenant_id, app_code, model_code, model_name, category, adapter_type, resolver, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            96000L,
            "T001",
            "SHADOW_HOOK_APP",
            "SUB_USER",
            "影子用户主体",
            "SUBJECT",
            "JAVA_BEAN",
            "subjectShadowProvider",
            "test",
            "test",
            0
        );
        jdbcTemplate.update(
            "INSERT INTO authz_bo_meta_model (id, tenant_id, app_code, bo_code, bo_name, schema_json, adapter_type, resolver, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            96001L,
            "T001",
            "SHADOW_HOOK_APP",
            "SHADOW_CONTRACT",
            "影子合同对象",
            "{}",
            "JAVA_BEAN",
            "noopHook",
            "test",
            "test",
            0
        );
        jdbcTemplate.update(
            "INSERT INTO authz_permission_item (id, tenant_id, app_code, perm_code, res_model_code, res_id, act_code, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            96002L,
            "T001",
            "SHADOW_HOOK_APP",
            "SHADOW_CONTRACT_READ",
            "RES_DATA_BO",
            "96001",
            "READ",
            "test",
            "test",
            0
        );
        jdbcTemplate.update(
            "INSERT INTO authz_assignment (id, tenant_id, app_code, subject_id, subject_model, perm_item_id, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
            96003L,
            "T001",
            "SHADOW_HOOK_APP",
            "shadow-role",
            "SUB_ROLE",
            96002L,
            "test",
            "test",
            0
        );
        jdbcTemplate.update(
            "INSERT INTO authz_subject_relation (id, tenant_id, app_code, subject_model, subject_id, related_subject_model, related_subject_id, relation_type, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            96004L,
            "T001",
            "SHADOW_HOOK_APP",
            "SUB_USER",
            "shadow-user",
            "SUB_ROLE",
            "shadow-role",
            "ROLE",
            "test",
            "test",
            0
        );

        AuthzDecision decision = authzFacade.checkByPermCode(
            "T001",
            "SHADOW_HOOK_APP",
            "shadow-user",
            "SHADOW_CONTRACT_READ",
            null
        );

        Assertions.assertNotNull(decision);
        Assertions.assertEquals(DecisionType.PERMIT, decision.getDecision());
        Assertions.assertEquals("PERMIT", decision.getReason());
        Assertions.assertTrue(decision.getMatchedPermissions().contains("SHADOW_CONTRACT_READ"));

        Assertions.assertEquals(1, testSubjectHookProbe.getInvocationCount());
        Assertions.assertEquals("T001", testSubjectHookProbe.getLastTenantId());
        Assertions.assertEquals("SHADOW_HOOK_APP", testSubjectHookProbe.getLastAppCode());
        Assertions.assertEquals("SUB_USER", testSubjectHookProbe.getLastModelCode());
        Assertions.assertEquals("shadow-user", testSubjectHookProbe.getLastSubjectId());
        Assertions.assertFalse(testSubjectHookProbe.isLastNativeMode());
        Assertions.assertNull(AuthMetaModelRuntimeContextHolder.getCurrent());
    }

    @Test
    void shouldPermitViaLocalSubjectRelationWhenSubjectShadowHookResolved() {
        testSubjectHookProbe.reset();
        jdbcTemplate.update(
            "INSERT INTO authz_meta_model (id, tenant_id, app_code, model_code, model_name, category, adapter_type, resolver, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            96010L,
            "T001",
            "SHADOW_LOCAL_REL_APP",
            "SUB_USER",
            "影子用户主体",
            "SUBJECT",
            "JAVA_BEAN",
            "subjectShadowProvider",
            "test",
            "test",
            0
        );
        jdbcTemplate.update(
            "INSERT INTO authz_bo_meta_model (id, tenant_id, app_code, bo_code, bo_name, schema_json, adapter_type, resolver, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            96011L,
            "T001",
            "SHADOW_LOCAL_REL_APP",
            "LOCAL_REL_CONTRACT",
            "本地关系补充合同对象",
            "{}",
            "JAVA_BEAN",
            "noopHook",
            "test",
            "test",
            0
        );
        jdbcTemplate.update(
            "INSERT INTO authz_permission_item (id, tenant_id, app_code, perm_code, res_model_code, res_id, act_code, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            96012L,
            "T001",
            "SHADOW_LOCAL_REL_APP",
            "LOCAL_REL_CONTRACT_READ",
            "RES_DATA_BO",
            "96011",
            "READ",
            "test",
            "test",
            0
        );
        jdbcTemplate.update(
            "INSERT INTO authz_assignment (id, tenant_id, app_code, subject_id, subject_model, perm_item_id, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
            96013L,
            "T001",
            "SHADOW_LOCAL_REL_APP",
            "local-role",
            "SUB_ROLE",
            96012L,
            "test",
            "test",
            0
        );
        jdbcTemplate.update(
            "INSERT INTO authz_subject_relation (id, tenant_id, app_code, subject_model, subject_id, related_subject_model, related_subject_id, relation_type, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            96014L,
            "T001",
            "SHADOW_LOCAL_REL_APP",
            "SUB_USER",
            "shadow-user",
            "SUB_ROLE",
            "local-role",
            "ROLE",
            "test",
            "test",
            0
        );

        AuthzDecision decision = authzFacade.checkByPermCode(
            "T001",
            "SHADOW_LOCAL_REL_APP",
            "shadow-user",
            "LOCAL_REL_CONTRACT_READ",
            null
        );

        Assertions.assertNotNull(decision);
        Assertions.assertEquals(DecisionType.PERMIT, decision.getDecision());
        Assertions.assertEquals("PERMIT", decision.getReason());
        Assertions.assertTrue(decision.getMatchedPermissions().contains("LOCAL_REL_CONTRACT_READ"));
        Assertions.assertEquals(1, testSubjectHookProbe.getInvocationCount());
        Assertions.assertEquals("shadow-user", testSubjectHookProbe.getLastSubjectId());
        Assertions.assertEquals("SUB_USER", testSubjectHookProbe.getLastModelCode());
        Assertions.assertFalse(testSubjectHookProbe.isLastNativeMode());
        Assertions.assertNull(AuthMetaModelRuntimeContextHolder.getCurrent());
    }

    @Test
    void shouldReturnIndeterminateWhenSubjectHookBeanMissingInSpringContext() throws Exception {
        jdbcTemplate.update(
            "INSERT INTO authz_meta_model (id, tenant_id, app_code, model_code, model_name, category, adapter_type, resolver, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            96010L,
            "T001",
            "SHADOW_HOOK_MISSING_APP",
            "SUB_USER",
            "影子用户主体缺失 Hook",
            "SUBJECT",
            "JAVA_BEAN",
            "missingSubjectShadowProvider",
            "test",
            "test",
            0
        );
        jdbcTemplate.update(
            "INSERT INTO authz_bo_meta_model (id, tenant_id, app_code, bo_code, bo_name, schema_json, adapter_type, resolver, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            96011L,
            "T001",
            "SHADOW_HOOK_MISSING_APP",
            "SHADOW_CONTRACT_MISSING",
            "影子合同对象缺失 Hook",
            "{}",
            "JAVA_BEAN",
            "noopHook",
            "test",
            "test",
            0
        );

        Map<String, Object> payload = new HashMap<>();
        payload.put("tenantId", "T001");
        payload.put("appCode", "SHADOW_HOOK_MISSING_APP");
        payload.put("action", "READ");
        payload.put("subject", buildSubject("shadow-user-missing", "SUB_USER"));
        payload.put("resource", buildResource("RES_DATA_BO", "SHADOW_CONTRACT_MISSING", "SHADOW_CONTRACT_MISSING"));

        mockMvc.perform(post("/authz-engine/api/v1/authz/check")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.decision").value("INDETERMINATE"))
            .andExpect(jsonPath("$.data.reason").value("HOOK_ERROR"));

        Assertions.assertNull(AuthMetaModelRuntimeContextHolder.getCurrent());
    }

    @Test
    void shouldLoadBoAttributesViaJavaBeanHookInSpringContext() throws Exception {
        testBoHookProbe.reset();
        jdbcTemplate.update(
            "INSERT INTO authz_bo_meta_model (id, tenant_id, app_code, bo_code, bo_name, schema_json, adapter_type, resolver, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            95000L,
            "T001",
            "CRM",
            "OPPORTUNITY_HOOK_CASE",
            "商机 Hook 测试对象",
            "{\"version\":\"2.0\",\"fields\":[{\"code\":\"stage\",\"type\":\"STRING\",\"source\":\"stage\",\"exposedToPolicy\":true}],\"policyContext\":{},\"sqlTranslation\":{}}",
            "JAVA_BEAN",
            "opportunityInfoProvider",
            "test",
            "test",
            0
        );
        jdbcTemplate.update(
            "INSERT INTO authz_permission_item (id, tenant_id, app_code, perm_code, res_model_code, res_id, act_code, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            95001L,
            "T001",
            "CRM",
            "OPPORTUNITY_HOOK_READ",
            "OPPORTUNITY_HOOK_CASE",
            "",
            "READ",
            "test",
            "test",
            0
        );
        jdbcTemplate.update(
            "INSERT INTO authz_assignment (id, tenant_id, app_code, subject_id, subject_model, perm_item_id, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
            95002L,
            "T001",
            "CRM",
            "demo-user",
            "SUB_USER",
            95001L,
            "test",
            "test",
            0
        );

        Map<String, Object> payload = new HashMap<>();
        payload.put("tenantId", "T001");
        payload.put("appCode", "CRM");
        payload.put("action", "READ");
        payload.put("subject", buildSubject("demo-user", "SUB_USER"));
        Map<String, Object> resource = buildResource("RES_DATA_BO", "OPPORTUNITY_HOOK_CASE", "OPPORTUNITY_HOOK_CASE");
        resource.put("resourceId", "OPP-001");
        payload.put("resource", resource);

        mockMvc.perform(post("/authz-engine/api/v1/authz/check")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.decision").value("PERMIT"))
            .andExpect(jsonPath("$.data.matchedPermissionCodes[0]").value("OPPORTUNITY_HOOK_READ"));

        Assertions.assertEquals(1, testBoHookProbe.getInvocationCount());
        Assertions.assertEquals("OPP-001", testBoHookProbe.getLastInstanceId());
        Assertions.assertEquals("OPPORTUNITY_HOOK_CASE", testBoHookProbe.getLastBoCode());
        Assertions.assertEquals("T001", testBoHookProbe.getLastTenantId());
        Assertions.assertEquals("CRM", testBoHookProbe.getLastAppCode());
        Assertions.assertNull(BoMetaModelRuntimeContextHolder.getCurrent());
    }

    @Test
    void shouldTranslateSemanticConditionViaBoHookInSpringContext() throws Exception {
        testBoHookProbe.reset();
        jdbcTemplate.update(
            "INSERT INTO authz_bo_meta_model (id, tenant_id, app_code, bo_code, bo_name, schema_json, adapter_type, resolver, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            95020L,
            "T001",
            "CRM",
            "OPPORTUNITY_SCOPE_CASE",
            "商机数据范围测试对象",
            "{\"version\":\"2.0\",\"fields\":[{\"code\":\"stage\",\"type\":\"STRING\",\"source\":\"stage\",\"exposedToPolicy\":true}],\"policyContext\":{},\"sqlTranslation\":{\"mode\":\"HOOK_SQL\"}}",
            "JAVA_BEAN",
            "opportunityInfoProvider",
            "test",
            "test",
            0
        );

        Map<String, Object> payload = new HashMap<>();
        payload.put("tenantId", "T001");
        payload.put("appCode", "CRM");
        payload.put("policyTemplateCode", "DATA_SCOPE_STAGE");
        payload.put("semanticCondition", "stage = 'APPROVING'");
        payload.put("subject", buildSubject("demo-user", "SUB_USER"));
        Map<String, Object> resource = buildResource("RES_DATA_BO", "OPPORTUNITY_SCOPE_CASE", "OPPORTUNITY_SCOPE_CASE");
        resource.put("resourceId", "OPP-DS-001");
        payload.put("resource", resource);

        mockMvc.perform(post("/authz-engine/api/v1/authz/data-scope/resolve")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.capabilityStatus").value("AVAILABLE"))
            .andExpect(jsonPath("$.data.translatedSql").value("stage = 'APPROVING'"))
            .andExpect(jsonPath("$.data.scopeFragments[0].type").value("SQL_WHERE"))
            .andExpect(jsonPath("$.data.scopeFragments[0].resolver").value("opportunityInfoProvider"));

        Assertions.assertEquals(1, testBoHookProbe.getTranslationInvocationCount());
        Assertions.assertEquals("stage = 'APPROVING'", testBoHookProbe.getLastSemanticCondition());
        Assertions.assertNull(BoMetaModelRuntimeContextHolder.getCurrent());
    }

    @Test
    void shouldBuildDataAndFieldObligationsFromRealPipInSpringContext() throws Exception {
        testBoHookProbe.reset();
        jdbcTemplate.update(
            "INSERT INTO dap_sys_user (id, tenant_id, app_code, staff_no, user_id, staff_name, org_id, status, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            95039L,
            "T001",
            "CRM",
            "obligation-user",
            "obligation-user",
            "义务测试用户",
            null,
            "ENABLED",
            "test",
            "test",
            0
        );
        jdbcTemplate.update(
            "INSERT INTO authz_bo_meta_model (id, tenant_id, app_code, bo_code, bo_name, schema_json, adapter_type, resolver, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            95040L,
            "T001",
            "CRM",
            "OPPORTUNITY_OBLIGATION_CASE",
            "商机义务测试对象",
            "{\"entities\":[{\"code\":\"opportunity\",\"name\":\"商机主表\",\"isPrimary\":true,\"tableName\":\"biz_opportunity\",\"attributes\":[{\"code\":\"id\",\"fieldName\":\"id\",\"columnName\":\"id\",\"name\":\"主键\",\"type\":\"LONG\",\"isPk\":true},{\"code\":\"ownerId\",\"fieldName\":\"ownerId\",\"columnName\":\"owner_id\",\"name\":\"负责人\",\"type\":\"STRING\",\"isPk\":false,\"filterable\":true},{\"code\":\"mobile\",\"fieldName\":\"mobile\",\"columnName\":\"mobile\",\"name\":\"手机号\",\"type\":\"STRING\",\"isPk\":false,\"fieldControl\":true,\"fieldControlStrategy\":\"MASK\"}]}],\"operations\":[{\"code\":\"READ\",\"name\":\"查看\",\"scope\":\"BO\"}]}",
            "JAVA_BEAN",
            "opportunityInfoProvider",
            "test",
            "test",
            0
        );
        jdbcTemplate.update(
            "INSERT INTO authz_permission_item (id, tenant_id, app_code, perm_code, res_model_code, res_id, act_code, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            95041L,
            "T001",
            "CRM",
            "OPPORTUNITY_OBLIGATION_READ",
            "RES_DATA_BO",
            "95040",
            "READ",
            "test",
            "test",
            0
        );
        jdbcTemplate.update(
            "INSERT INTO authz_std_pol_template (id, tenant_id, template_code, template_name, pol_type, expression_script, param_schema, status, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            95042L,
            "T001",
            "DATA_OWNER_ONLY_PIP",
            "仅负责人可见",
            "DATA",
            "#tableName + '.owner_id = ' + param(#sub['id'])",
            "{}",
            "ENABLED",
            "test",
            "test",
            0
        );
        jdbcTemplate.update(
            "INSERT INTO authz_std_pol_template (id, tenant_id, template_code, template_name, pol_type, expression_script, param_schema, status, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            95043L,
            "T001",
            "FIELD_MASK_MOBILE_PIP",
            "手机号脱敏",
            "FIELD",
            "return true;",
            "{\"properties\":{\"action\":{\"const\":\"MASK\"}}}",
            "ENABLED",
            "test",
            "test",
            0
        );
        jdbcTemplate.update(
            "INSERT INTO authz_assignment (id, tenant_id, app_code, subject_id, subject_model, perm_item_id, policy_tpl_id, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            95044L,
            "T001",
            "CRM",
            "obligation-user",
            "SUB_USER",
            95041L,
            95042L,
            "test",
            "test",
            0
        );
        jdbcTemplate.update(
            "INSERT INTO authz_assignment (id, tenant_id, app_code, subject_id, subject_model, perm_item_id, policy_tpl_id, policy_params, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            95045L,
            "T001",
            "CRM",
            "obligation-user",
            "SUB_USER",
            95041L,
            95043L,
            "{\"targetField\":\"mobile\"}",
            "test",
            "test",
            0
        );

        Map<String, Object> payload = new HashMap<>();
        payload.put("tenantId", "T001");
        payload.put("appCode", "CRM");
        payload.put("action", "READ");
        payload.put("subject", buildSubject("obligation-user", "SUB_USER"));
        Map<String, Object> requestContext = new HashMap<>();
        requestContext.put("instanceId", "OPP-OB-001");
        payload.put("context", requestContext);
        Map<String, Object> resource = buildResource("RES_DATA_BO", "OPPORTUNITY_OBLIGATION_CASE", "95040");
        payload.put("resource", resource);

        mockMvc.perform(post("/authz-engine/api/v1/authz/check")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.decision").value("PERMIT"))
            .andExpect(jsonPath("$.data.obligations.rowFilter.whereClause").value("(biz_opportunity.owner_id = ?)"))
            .andExpect(jsonPath("$.data.obligations.rowFilter.params[0]").value("obligation-user"))
            .andExpect(jsonPath("$.data.obligations.fieldControls[0].action").value("MASK"))
            .andExpect(jsonPath("$.data.obligations.fieldControls[0].code").value("mobile"))
            .andExpect(jsonPath("$.data.obligations.fieldControls[0].policyCode").value("FIELD_MASK_MOBILE_PIP"));

        Assertions.assertEquals(1, testBoHookProbe.getInvocationCount());
        Assertions.assertEquals("OPP-OB-001", testBoHookProbe.getLastInstanceId());
    }

    @Test
    void shouldReturnIntegrationErrorWhenDataScopeHookBeanMissingInSpringContext() throws Exception {
        jdbcTemplate.update(
            "INSERT INTO authz_bo_meta_model (id, tenant_id, app_code, bo_code, bo_name, schema_json, adapter_type, resolver, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            95030L,
            "T001",
            "CRM",
            "OPPORTUNITY_SCOPE_MISSING",
            "商机数据范围缺失 Hook 测试对象",
            "{\"version\":\"2.0\",\"fields\":[{\"code\":\"stage\",\"type\":\"STRING\",\"source\":\"stage\",\"exposedToPolicy\":true}],\"policyContext\":{},\"sqlTranslation\":{\"mode\":\"HOOK_SQL\"}}",
            "JAVA_BEAN",
            "missingOpportunityProvider",
            "test",
            "test",
            0
        );

        Map<String, Object> payload = new HashMap<>();
        payload.put("tenantId", "T001");
        payload.put("appCode", "CRM");
        payload.put("policyTemplateCode", "DATA_SCOPE_STAGE");
        payload.put("semanticCondition", "stage = 'APPROVING'");
        payload.put("subject", buildSubject("demo-user", "SUB_USER"));
        payload.put("resource", buildResource("RES_DATA_BO", "OPPORTUNITY_SCOPE_MISSING", "OPPORTUNITY_SCOPE_MISSING"));

        mockMvc.perform(post("/authz-engine/api/v1/authz/data-scope/resolve")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("AUTHZ-503"))
            .andExpect(jsonPath("$.message").value("未找到业务对象 Hook Bean: missingOpportunityProvider"));

        Assertions.assertNull(BoMetaModelRuntimeContextHolder.getCurrent());
    }

    @Test
    void shouldReturnIndeterminateWhenBoHookBeanMissingInSpringContext() throws Exception {
        jdbcTemplate.update(
            "INSERT INTO authz_bo_meta_model (id, tenant_id, app_code, bo_code, bo_name, schema_json, adapter_type, resolver, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            95010L,
            "T001",
            "CRM",
            "OPPORTUNITY_HOOK_MISSING",
            "商机缺失 Hook 测试对象",
            "{\"version\":\"2.0\",\"fields\":[{\"code\":\"stage\",\"type\":\"STRING\"}],\"policyContext\":{},\"sqlTranslation\":{}}",
            "JAVA_BEAN",
            "missingOpportunityProvider",
            "test",
            "test",
            0
        );
        jdbcTemplate.update(
            "INSERT INTO authz_permission_item (id, tenant_id, app_code, perm_code, res_model_code, res_id, act_code, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            95011L,
            "T001",
            "CRM",
            "OPPORTUNITY_HOOK_MISSING_READ",
            "OPPORTUNITY_HOOK_MISSING",
            "",
            "READ",
            "test",
            "test",
            0
        );
        jdbcTemplate.update(
            "INSERT INTO authz_assignment (id, tenant_id, app_code, subject_id, subject_model, perm_item_id, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
            95012L,
            "T001",
            "CRM",
            "demo-user",
            "SUB_USER",
            95011L,
            "test",
            "test",
            0
        );

        Map<String, Object> payload = new HashMap<>();
        payload.put("tenantId", "T001");
        payload.put("appCode", "CRM");
        payload.put("action", "READ");
        payload.put("subject", buildSubject("demo-user", "SUB_USER"));
        Map<String, Object> resource = buildResource("RES_DATA_BO", "OPPORTUNITY_HOOK_MISSING", "OPPORTUNITY_HOOK_MISSING");
        resource.put("resourceId", "OPP-404");
        payload.put("resource", resource);

        mockMvc.perform(post("/authz-engine/api/v1/authz/check")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.decision").value("INDETERMINATE"))
            .andExpect(jsonPath("$.data.reason").value("HOOK_ERROR"));

        Assertions.assertNull(BoMetaModelRuntimeContextHolder.getCurrent());
    }

    @Test
    void shouldPersistAndQueryAuditLogsInSpringContext() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        String traceId = "TRACE-AUDIT-US3-001";
        jdbcTemplate.update("DELETE FROM authz_audit_log WHERE tenant_id = ? AND app_code = ?", "T001", "CRM");
        payload.put("tenantId", "T001");
        payload.put("appCode", "CRM");
        payload.put("action", "APPROVE");
        payload.put("traceId", traceId);
        payload.put("subject", buildSubject("demo-user", "SUB_USER"));
        payload.put("resource", buildResource("RES_DATA_BO", "CONTRACT", "CONTRACT"));

        mockMvc.perform(post("/authz-engine/api/v1/authz/check")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.auditLogId").isNotEmpty());

        Integer auditLogCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM authz_audit_log WHERE tenant_id = ? AND app_code = ? AND request_id = ? AND decision = ?",
            Integer.class,
            "T001", "CRM", traceId, "PERMIT"
        );

        Assertions.assertEquals(1, auditLogCount);

        mockMvc.perform(get("/authz-engine/api/v1/governance/audit-logs")
                .param("tenantId", "T001")
                .param("appCode", "CRM")
                .param("subjectId", "demo-user")
                .param("actionCode", "APPROVE")
                .param("pageNo", "1")
                .param("pageSize", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.records[0].requestId").value(traceId))
            .andExpect(jsonPath("$.data.records[0].decision").value("PERMIT"));
    }

    @Test
    void shouldPersistNormalizedActionCodeInAuditLogWhenAliasMatches() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        String traceId = "TRACE-AUDIT-ALIAS-001";
        jdbcTemplate.update("DELETE FROM authz_audit_log WHERE tenant_id = ? AND app_code = ?", "T001", "CRM");
        payload.put("tenantId", "T001");
        payload.put("appCode", "CRM");
        payload.put("action", "approve_request");
        payload.put("traceId", traceId);
        payload.put("subject", buildSubject("demo-user", "SUB_USER"));
        payload.put("resource", buildResource("RES_DATA_BO", "CONTRACT", "CONTRACT"));

        mockMvc.perform(post("/authz-engine/api/v1/authz/check")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.decision").value("PERMIT"));

        mockMvc.perform(get("/authz-engine/api/v1/governance/audit-logs")
                .param("tenantId", "T001")
                .param("appCode", "CRM")
                .param("actionCode", "APPROVE")
                .param("pageNo", "1")
                .param("pageSize", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.records[0].requestId").value(traceId))
            .andExpect(jsonPath("$.data.records[0].actionCode").value("APPROVE"));
    }

    @Test
    void shouldRejectDelegationWhenGrantorLacksActivePermissionInSpringContext() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("tenantId", "T001");
        payload.put("appCode", "CRM");
        payload.put("grantorSubjectModel", "SUB_USER");
        payload.put("grantorSubjectId", "ghost-user");
        payload.put("delegateSubjectModel", "SUB_USER");
        payload.put("delegateSubjectId", "approver-b");
        payload.put("permissionCode", "CONTRACT_APPROVE");
        payload.put("startTime", "2026-04-02T09:00:00");
        payload.put("endTime", "2026-04-03T09:00:00");

        mockMvc.perform(post("/authz-engine/api/v1/governance/delegations")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("AUTHZ-409"))
            .andExpect(jsonPath("$.message").value("委托人未持有可委托的有效权限"));
    }

    @Test
    void shouldPersistHookStatusInAuditLogWhenSubjectHookSucceedsInSpringContext() throws Exception {
        String traceId = "TRACE-HOOK-AUDIT-001";
        jdbcTemplate.update("DELETE FROM authz_audit_log WHERE request_id = ?", traceId);
        jdbcTemplate.update(
            "INSERT INTO authz_meta_model (id, tenant_id, app_code, model_code, model_name, category, adapter_type, resolver, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            97000L, "T001", "HOOK_AUDIT_APP", "SUB_USER", "Hook 审计测试主体", "SUBJECT", "JAVA_BEAN", "subjectShadowProvider", "test", "test", 0
        );
        jdbcTemplate.update(
            "INSERT INTO authz_bo_meta_model (id, tenant_id, app_code, bo_code, bo_name, schema_json, adapter_type, resolver, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            97001L, "T001", "HOOK_AUDIT_APP", "AUDIT_CONTRACT", "Hook 审计合同对象", "{}", "JAVA_BEAN", "noopHook", "test", "test", 0
        );
        jdbcTemplate.update(
            "INSERT INTO authz_permission_item (id, tenant_id, app_code, perm_code, res_model_code, res_id, act_code, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            97002L, "T001", "HOOK_AUDIT_APP", "AUDIT_CONTRACT_READ", "AUDIT_CONTRACT", "", "READ", "test", "test", 0
        );
        jdbcTemplate.update(
            "INSERT INTO authz_assignment (id, tenant_id, app_code, subject_id, subject_model, perm_item_id, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
            97003L, "T001", "HOOK_AUDIT_APP", "shadow-role", "SUB_ROLE", 97002L, "test", "test", 0
        );

        Map<String, Object> payload = new HashMap<>();
        payload.put("tenantId", "T001");
        payload.put("appCode", "HOOK_AUDIT_APP");
        payload.put("action", "READ");
        payload.put("traceId", traceId);
        payload.put("subject", buildSubject("shadow-audit-user", "SUB_USER"));
        payload.put("resource", buildResource("RES_DATA_BO", "AUDIT_CONTRACT", "AUDIT_CONTRACT"));

        mockMvc.perform(post("/authz-engine/api/v1/authz/check")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.decision").value("PERMIT"));

        String hookStatus = jdbcTemplate.queryForObject(
            "SELECT hook_status FROM authz_audit_log WHERE request_id = ?",
            String.class, traceId
        );
        Long hookCostMs = jdbcTemplate.queryForObject(
            "SELECT hook_cost_ms FROM authz_audit_log WHERE request_id = ?",
            Long.class, traceId
        );
        String attributeSnapshot = jdbcTemplate.queryForObject(
            "SELECT attribute_snapshot FROM authz_audit_log WHERE request_id = ?",
            String.class, traceId
        );
        Assertions.assertEquals("SUCCESS", hookStatus, "Subject Hook 成功时 hook_status 应为 SUCCESS");
        Assertions.assertNotNull(hookCostMs, "hook_cost_ms 应记录 Hook 耗时");
        Assertions.assertTrue(hookCostMs >= 0, "hook_cost_ms 应为非负值");
        Assertions.assertNotNull(attributeSnapshot, "attribute_snapshot 应包含 Hook 返回的属性快照");
        Assertions.assertTrue(attributeSnapshot.contains("deptCode"), "attribute_snapshot 应包含 deptCode 字段");
    }

    // ---- T027：US3 - schema_json V2 Lite 协议运行时拒绝集成测试 ----

    @Test
    void shouldBlockDecisionWhenBoSchemaJsonHasUnsupportedVersionInSpringContext() throws Exception {
        // 直接插入 V3.0 不受支持协议版本的 authz_bo_meta_model，绕过保存时校验
        jdbcTemplate.update(
            "INSERT INTO authz_bo_meta_model (id, tenant_id, app_code, bo_code, bo_name, schema_json, adapter_type, resolver, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            98000L, "T001", "CRM", "SCHEMA_V3_REJECT",
            "不受支持协议版本对象",
            "{\"version\":\"3.0\",\"fields\":[{\"code\":\"stage\",\"type\":\"STRING\",\"source\":\"f_stage\",\"exposedToPolicy\":true}]}",
            "JAVA_BEAN", "noopHook", "test", "test", 0
        );
        jdbcTemplate.update(
            "INSERT INTO authz_permission_item (id, tenant_id, app_code, perm_code, res_model_code, res_id, act_code, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            98001L, "T001", "CRM", "SCHEMA_V3_REJECT_READ", "SCHEMA_V3_REJECT", "", "READ", "test", "test", 0
        );
        jdbcTemplate.update(
            "INSERT INTO authz_assignment (id, tenant_id, app_code, subject_id, subject_model, perm_item_id, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
            98002L, "T001", "CRM", "demo-user", "SUB_USER", 98001L, "test", "test", 0
        );

        Map<String, Object> payload = new HashMap<>();
        payload.put("tenantId", "T001");
        payload.put("appCode", "CRM");
        payload.put("action", "READ");
        payload.put("subject", buildSubject("demo-user", "SUB_USER"));
        Map<String, Object> resource98 = buildResource("RES_DATA_BO", "SCHEMA_V3_REJECT", "SCHEMA_V3_REJECT_CASE");
        resource98.put("instanceId", "V3-INSTANCE-001");
        payload.put("resource", resource98);

        mockMvc.perform(post("/authz-engine/api/v1/authz/check")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("AUTHZ-500"));
    }

    @Test
    void shouldBlockDecisionWhenBoSchemaJsonAllowFieldsContainsUndeclaredFieldInSpringContext() throws Exception {
        // 直接插入 allowFields 引用了未在 fields 列表中声明的字段的 authz_bo_meta_model
        jdbcTemplate.update(
            "INSERT INTO authz_bo_meta_model (id, tenant_id, app_code, bo_code, bo_name, schema_json, adapter_type, resolver, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            98010L, "T001", "CRM", "SCHEMA_BAD_ALLOW",
            "越界 allowFields 对象",
            "{\"version\":\"2.0\",\"fields\":[{\"code\":\"stage\",\"type\":\"STRING\",\"source\":\"f_stage\",\"exposedToPolicy\":true}],"
                + "\"policyContext\":{\"resourcePrefix\":\"res\",\"allowFields\":[\"undeclared_field\"]}}",
            "JAVA_BEAN", "noopHook", "test", "test", 0
        );
        jdbcTemplate.update(
            "INSERT INTO authz_permission_item (id, tenant_id, app_code, perm_code, res_model_code, res_id, act_code, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            98011L, "T001", "CRM", "SCHEMA_BAD_ALLOW_READ", "SCHEMA_BAD_ALLOW", "", "READ", "test", "test", 0
        );
        jdbcTemplate.update(
            "INSERT INTO authz_assignment (id, tenant_id, app_code, subject_id, subject_model, perm_item_id, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
            98012L, "T001", "CRM", "demo-user", "SUB_USER", 98011L, "test", "test", 0
        );

        Map<String, Object> payload = new HashMap<>();
        payload.put("tenantId", "T001");
        payload.put("appCode", "CRM");
        payload.put("action", "READ");
        payload.put("subject", buildSubject("demo-user", "SUB_USER"));
        Map<String, Object> badAllowResource = buildResource("RES_DATA_BO", "SCHEMA_BAD_ALLOW", "SCHEMA_BAD_ALLOW_CASE");
        badAllowResource.put("resourceId", "BAD-ALLOW-INSTANCE-001");
        payload.put("resource", badAllowResource);

        mockMvc.perform(post("/authz-engine/api/v1/authz/check")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("AUTHZ-500"));
    }

    // ---- T012-1：US3 - authz_res_derivation_perm 派生权限链路集成验证 ----

    @Test
    void shouldLoadMenuTreeViaDerivationPermChainInSpringContext() throws Exception {
        // 准备：菜单、页面、用户、权限项、派生绑定、授权
        jdbcTemplate.update(
            "INSERT INTO usp_menu_item (id, tenant_id, tenant_code, app_code, menu_code, menu_name, menu_type, sort_no, status, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            99100L, "T001", "T001", "CRM", "MENU-DERIV-T012", "派生测试菜单", "MENU", 0, "ENABLED", "test", "test", 0
        );
        jdbcTemplate.update(
            "INSERT INTO usp_page (id, tenant_id, app_code, page_code, page_name, menu_id, sort_order, status, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            99101L, "T001", "CRM", "PAGE-DERIV-T012", "派生测试页面", 99100L, 0, "ENABLED", "test", "test", 0
        );
        jdbcTemplate.update(
            "INSERT INTO dap_sys_user (id, tenant_id, app_code, staff_no, user_id, staff_name, org_id, status, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            99102L, "T001", "CRM", "deriv-t012-user", "deriv-t012-user", "派生测试用户", null, "ENABLED", "test", "test", 0
        );
        jdbcTemplate.update(
            "INSERT INTO authz_permission_item (id, tenant_id, app_code, perm_code, res_model_code, res_id, act_code, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            99103L, "T001", "CRM", "DERIV_T012_READ", "RES_DATA_BO", "", "READ", "test", "test", 0
        );
        // authz_res_derivation_perm.res_id = usp_page.id（99101）
        jdbcTemplate.update(
            "INSERT INTO authz_res_derivation_perm (id, tenant_id, app_code, res_type, res_id, perm_item_id, sort_order, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            99104L, "T001", "CRM", "RES_UI_PAGE", 99101L, 99103L, 0, "test", "test", 0
        );
        jdbcTemplate.update(
            "INSERT INTO authz_assignment (id, tenant_id, app_code, subject_id, subject_model, perm_item_id, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
            99105L, "T001", "CRM", "deriv-t012-user", "SUB_USER", 99103L, "test", "test", 0
        );

        mockMvc.perform(get("/authz-engine/api/v1/authz/user-context")
                .param("tenantId", "T001")
                .param("appCode", "CRM")
                .param("subjectId", "deriv-t012-user")
                .param("subjectModel", "SUB_USER"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.permCodes").isArray())
            .andExpect(jsonPath("$.data.permCodes[?(@=='DERIV_T012_READ')]").exists())
            // 派生链路：页面 PAGE-DERIV-T012 → 菜单 MENU-DERIV-T012
            .andExpect(jsonPath("$.data.accessibleResources.RES_UI_MENU[?(@=='MENU-DERIV-T012')]").exists())
            // menuTree 中应含派生菜单节点
            .andExpect(jsonPath("$.data.menuTree[?(@.menuCode=='MENU-DERIV-T012')]").exists());
    }

    // ---- T012-2：US3 - Shadow 模式菜单元数据回调集成验证 ----

    @Test
    void shouldLoadMenuTreeViaShadowBatchResolveItemsInSpringContext() throws Exception {
        testMenuShadowHookProbe.reset();
        // RES_UI_MENU Shadow 适配器注册
        jdbcTemplate.update(
            "INSERT INTO authz_meta_model (id, tenant_id, app_code, model_code, model_name, category, adapter_type, resolver, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            99200L, "T001", "T012_SHADOW_APP", "RES_UI_MENU", "影子菜单适配器", "RESOURCE", "JAVA_BEAN", "menuShadowProvider", "test", "test", 0
        );
        // 引擎内置菜单记录（供页面 menu_id 引用）
        jdbcTemplate.update(
            "INSERT INTO usp_menu_item (id, tenant_id, tenant_code, app_code, menu_code, menu_name, menu_type, sort_no, status, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            99201L, "T001", "T001", "T012_SHADOW_APP", "MENU-SHADOW-T012", "影子菜单（DB）", "MENU", 0, "ENABLED", "test", "test", 0
        );
        jdbcTemplate.update(
            "INSERT INTO usp_page (id, tenant_id, app_code, page_code, page_name, menu_id, sort_order, status, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            99202L, "T001", "T012_SHADOW_APP", "PAGE-SHADOW-T012", "影子页面", 99201L, 0, "ENABLED", "test", "test", 0
        );
        jdbcTemplate.update(
            "INSERT INTO authz_permission_item (id, tenant_id, app_code, perm_code, res_model_code, res_id, act_code, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            99203L, "T001", "T012_SHADOW_APP", "SHADOW_T012_READ", "RES_DATA_BO", "", "READ", "test", "test", 0
        );
        jdbcTemplate.update(
            "INSERT INTO authz_res_derivation_perm (id, tenant_id, app_code, res_type, res_id, perm_item_id, sort_order, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            99204L, "T001", "T012_SHADOW_APP", "RES_UI_PAGE", 99202L, 99203L, 0, "test", "test", 0
        );
        jdbcTemplate.update(
            "INSERT INTO authz_assignment (id, tenant_id, app_code, subject_id, subject_model, perm_item_id, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
            99205L, "T001", "T012_SHADOW_APP", "shadow-menu-user", "SUB_USER", 99203L, "test", "test", 0
        );

        mockMvc.perform(get("/authz-engine/api/v1/authz/user-context")
                .param("tenantId", "T001")
                .param("appCode", "T012_SHADOW_APP")
                .param("subjectId", "shadow-menu-user")
                .param("subjectModel", "SUB_USER"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            // Shadow batchResolveItems(RES_UI_MENU) 的返回数据决定 menuTree 内容
            .andExpect(jsonPath("$.data.menuTree[?(@.menuCode=='MENU-SHADOW-T012')]").exists());

        // 验证 Shadow batchResolveItems 已被调用，且参数含 MENU-SHADOW-T012
        Assertions.assertTrue(testMenuShadowHookProbe.wasInvoked(),
            "menuShadowProvider.batchResolveItems 应在 Shadow 菜单模式下被调用");
        Assertions.assertTrue(testMenuShadowHookProbe.getLastBatchCodes().contains("MENU-SHADOW-T012"),
            "batchResolveItems 应以 MENU-SHADOW-T012 为参数");
    }

    // ---- T012-3：US3 - 引擎主体关系表补全 → UserContext 可见性集成验证 ----

    @Test
    void shouldExpandSubjectKeysViaRelationTableInUserContextInSpringContext() throws Exception {
        testSubjectHookProbe.reset();
        // SUB_USER Shadow 适配器仅补充属性，主体关联仍来自引擎库 authz_subject_relation
        jdbcTemplate.update(
            "INSERT INTO authz_meta_model (id, tenant_id, app_code, model_code, model_name, category, adapter_type, resolver, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            99300L, "T001", "T012_RELATED_SUBJ_APP", "SUB_USER", "影子用户主体", "SUBJECT", "JAVA_BEAN", "relatedSubjectProvider", "test", "test", 0
        );
        // 菜单资源，用于 COMPAT 模式下旧菜单权限项路径
        jdbcTemplate.update(
            "INSERT INTO usp_menu_item (id, tenant_id, tenant_code, app_code, menu_code, menu_name, menu_type, sort_no, status, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            99301L, "T001", "T001", "T012_RELATED_SUBJ_APP", "MENU-RELATED-T012", "关联主体菜单", "MENU", 0, "ENABLED", "test", "test", 0
        );
        // 菜单权限项：分配给 shadow-role（主体关联由 authz_subject_relation 提供）
        jdbcTemplate.update(
            "INSERT INTO authz_permission_item (id, tenant_id, app_code, perm_code, res_model_code, res_id, act_code, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            99302L, "T001", "T012_RELATED_SUBJ_APP", "RELATED_T012_MENU_PERM", "RES_UI_MENU", "MENU-RELATED-T012", "READ", "test", "test", 0
        );
        jdbcTemplate.update(
            "INSERT INTO authz_assignment (id, tenant_id, app_code, subject_id, subject_model, perm_item_id, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
            99303L, "T001", "T012_RELATED_SUBJ_APP", "shadow-role", "SUB_ROLE", 99302L, "test", "test", 0
        );
        jdbcTemplate.update(
            "INSERT INTO authz_subject_relation (id, tenant_id, app_code, subject_model, subject_id, related_subject_model, related_subject_id, relation_type, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            99304L, "T001", "T012_RELATED_SUBJ_APP", "SUB_USER", "related-shadow-user", "SUB_ROLE", "shadow-role", "ROLE", "test", "test", 0
        );

        mockMvc.perform(get("/authz-engine/api/v1/authz/user-context")
                .param("tenantId", "T001")
                .param("appCode", "T012_RELATED_SUBJ_APP")
                .param("subjectId", "related-shadow-user")
                .param("subjectModel", "SUB_USER"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            // 关系表补全后，shadow-role 的菜单权限应体现在 permCodes 与菜单可见性中
            .andExpect(jsonPath("$.data.permCodes[?(@=='RELATED_T012_MENU_PERM')]").exists())
            .andExpect(jsonPath("$.data.accessibleResources.RES_UI_MENU[?(@=='MENU-RELATED-T012')]").exists())
            .andExpect(jsonPath("$.data.menuTree[?(@.menuCode=='MENU-RELATED-T012')]").exists());

        // 断言权限数据说明关系表链路已正确打通（无需 probe，结果即是证据）
    }

    private Map<String, Object> buildSubject(String id, String type) {
        Map<String, Object> subject = new HashMap<>();
        subject.put("id", id);
        subject.put("type", type);
        return subject;
    }

    private Map<String, Object> buildResource(String type, String modelCode, String resourceId) {
        Map<String, Object> resource = new HashMap<>();
        resource.put("type", type);
        resource.put("modelCode", modelCode);
        resource.put("resourceId", resourceId);
        return resource;
    }

    @TestConfiguration
    static class TestBoHookConfiguration {

        @Bean
        TestBoHookProbe testBoHookProbe() {
            return new TestBoHookProbe();
        }

        @Bean("opportunityInfoProvider")
        BoMetaModelAdapter opportunityInfoProvider(TestBoHookProbe probe) {
            return new BoMetaModelAdapter() {
                @Override
                public Map<String, Object> fetchInstanceAttributes(String instanceId, String schemaJson, Map<String, Object> requestContext) {
                    BoMetaModelRuntimeContext runtimeContext = BoMetaModelRuntimeContextHolder.requireCurrent();
                    probe.markInvoked(runtimeContext.getTenantId(), runtimeContext.getAppCode(), runtimeContext.getBoCode(), instanceId);
                    Map<String, Object> attributes = new HashMap<>();
                    attributes.put("stage", "APPROVING");
                    attributes.put("ownerDept", "SALES");
                    return attributes;
                }

                @Override
                public String translateToPhysicalSql(String semanticCondition, String schemaJson) {
                    BoMetaModelRuntimeContext runtimeContext = BoMetaModelRuntimeContextHolder.requireCurrent();
                    probe.markTranslated(runtimeContext.getTenantId(), runtimeContext.getAppCode(), runtimeContext.getBoCode(), semanticCondition);
                    if ("stage = 'APPROVING'".equals(semanticCondition)) {
                        return "stage = 'APPROVING'";
                    }
                    return "1 = 1";
                }
            };
        }
    }

    @TestConfiguration
    static class TestSubjectHookConfiguration {

        @Bean
        TestSubjectHookProbe testSubjectHookProbe() {
            return new TestSubjectHookProbe();
        }

        @Bean("subjectShadowProvider")
        AuthMetaModelAdapter subjectShadowProvider(TestSubjectHookProbe probe) {
            // 新接口参数顺序：fetchInstanceAttributes(ModelCode modelCode, String instanceId, Map requestContext)
            return (modelCode, instanceId, requestContext) -> {
                AuthMetaModelRuntimeContext runtimeContext = AuthMetaModelRuntimeContextHolder.requireCurrent();
                probe.markInvoked(
                    runtimeContext.getTenantId(),
                    runtimeContext.getAppCode(),
                    runtimeContext.getModelCode(),
                    instanceId,
                    runtimeContext.isNativeMode(),
                    runtimeContext.isShadowMode()
                );
                Map<String, Object> attributes = new HashMap<>();
                attributes.put("deptCode", "SHADOW-SALES");
                return SubjectHookResult.builder().attributes(attributes).build();
            };
        }
    }

    static class TestBoHookProbe {

        private final AtomicInteger invocationCount = new AtomicInteger();

        private final AtomicReference<String> lastBoCode = new AtomicReference<>();

        private final AtomicReference<String> lastTenantId = new AtomicReference<>();

        private final AtomicReference<String> lastAppCode = new AtomicReference<>();

        private final AtomicReference<String> lastInstanceId = new AtomicReference<>();

        private final AtomicInteger translationInvocationCount = new AtomicInteger();

        private final AtomicReference<String> lastSemanticCondition = new AtomicReference<>();

        void markInvoked(String tenantId, String appCode, String boCode, String instanceId) {
            invocationCount.incrementAndGet();
            lastTenantId.set(tenantId);
            lastAppCode.set(appCode);
            lastBoCode.set(boCode);
            lastInstanceId.set(instanceId);
        }

        void markTranslated(String tenantId, String appCode, String boCode, String semanticCondition) {
            translationInvocationCount.incrementAndGet();
            lastTenantId.set(tenantId);
            lastAppCode.set(appCode);
            lastBoCode.set(boCode);
            lastSemanticCondition.set(semanticCondition);
        }

        int getInvocationCount() {
            return invocationCount.get();
        }

        String getLastBoCode() {
            return lastBoCode.get();
        }

        String getLastTenantId() {
            return lastTenantId.get();
        }

        String getLastAppCode() {
            return lastAppCode.get();
        }

        String getLastInstanceId() {
            return lastInstanceId.get();
        }

        int getTranslationInvocationCount() {
            return translationInvocationCount.get();
        }

        String getLastSemanticCondition() {
            return lastSemanticCondition.get();
        }

        void reset() {
            invocationCount.set(0);
            translationInvocationCount.set(0);
            lastTenantId.set(null);
            lastAppCode.set(null);
            lastBoCode.set(null);
            lastInstanceId.set(null);
            lastSemanticCondition.set(null);
        }
    }

    static class TestSubjectHookProbe {

        private final AtomicInteger invocationCount = new AtomicInteger();

        private final AtomicReference<String> lastTenantId = new AtomicReference<>();

        private final AtomicReference<String> lastAppCode = new AtomicReference<>();

        private final AtomicReference<String> lastModelCode = new AtomicReference<>();

        private final AtomicReference<String> lastSubjectId = new AtomicReference<>();

        private final AtomicReference<Boolean> lastNativeMode = new AtomicReference<>();

        private final AtomicReference<Boolean> lastShadowMode = new AtomicReference<>();

        void markInvoked(
            String tenantId,
            String appCode,
            String modelCode,
            String subjectId,
            boolean nativeMode,
            boolean shadowMode
        ) {
            invocationCount.incrementAndGet();
            lastTenantId.set(tenantId);
            lastAppCode.set(appCode);
            lastModelCode.set(modelCode);
            lastSubjectId.set(subjectId);
            lastNativeMode.set(nativeMode);
            lastShadowMode.set(shadowMode);
        }

        int getInvocationCount() {
            return invocationCount.get();
        }

        String getLastTenantId() {
            return lastTenantId.get();
        }

        String getLastAppCode() {
            return lastAppCode.get();
        }

        String getLastModelCode() {
            return lastModelCode.get();
        }

        String getLastSubjectId() {
            return lastSubjectId.get();
        }

        boolean isLastNativeMode() {
            return Boolean.TRUE.equals(lastNativeMode.get());
        }

        boolean isLastShadowMode() {
            return Boolean.TRUE.equals(lastShadowMode.get());
        }

        void reset() {
            invocationCount.set(0);
            lastTenantId.set(null);
            lastAppCode.set(null);
            lastModelCode.set(null);
            lastSubjectId.set(null);
            lastNativeMode.set(null);
            lastShadowMode.set(null);
        }
    }

    @TestConfiguration
    static class TestMenuShadowHookConfiguration {

        @Bean
        TestMenuShadowHookProbe testMenuShadowHookProbe() {
            return new TestMenuShadowHookProbe();
        }

        /**
         * 菜单 Shadow 适配器：实现 batchResolveItems，供 T012-2 Shadow 菜单回调集成测试使用。
         */
        @Bean("menuShadowProvider")
        AuthMetaModelAdapter menuShadowProvider(TestMenuShadowHookProbe probe) {
            return new AuthMetaModelAdapter() {
                @Override
                public SubjectHookResult fetchInstanceAttributes(
                        ModelCode modelCode, String instanceId, Map<String, Object> requestContext) {
                    // 菜单适配器无主体属性，直接返回空结果
                    return SubjectHookResult.builder().build();
                }

                @Override
                public List<DataItem> batchResolveItems(ModelCode modelCode, List<String> itemCodes) {
                    probe.markInvoked(modelCode, itemCodes);
                    // 为每个编码构造最小 DataItem：code=menuCode，name=菜单名
                    List<DataItem> result = new ArrayList<>();
                    for (String code : itemCodes) {
                        result.add(DataItem.builder()
                            .code(code)
                            .name("Shadow 菜单 " + code)
                            .status("ENABLED")
                            .build());
                    }
                    return result;
                }
            };
        }

        /**
         * 轻量版主体 Shadow 适配器（不依赖 RuntimeContextHolder），
         * 专用于 T012-3 user-context 集成测试，仅补充主体属性。
         */
        @Bean("relatedSubjectProvider")
        AuthMetaModelAdapter relatedSubjectProvider(TestMenuShadowHookProbe probe) {
            return (modelCode, instanceId, requestContext) -> SubjectHookResult.builder()
                .attributes(Collections.singletonMap("shadowSubjectId", instanceId))
                .build();
        }
    }

    static class TestMenuShadowHookProbe {

        private final AtomicReference<ModelCode> lastModelCode = new AtomicReference<>();

        private final AtomicReference<List<String>> lastBatchCodes = new AtomicReference<>();

        private final AtomicInteger invocationCount = new AtomicInteger();

        void markInvoked(ModelCode modelCode, List<String> itemCodes) {
            invocationCount.incrementAndGet();
            lastModelCode.set(modelCode);
            lastBatchCodes.set(itemCodes == null ? Collections.emptyList() : new ArrayList<>(itemCodes));
        }

        boolean wasInvoked() {
            return invocationCount.get() > 0;
        }

        List<String> getLastBatchCodes() {
            List<String> codes = lastBatchCodes.get();
            return codes != null ? codes : Collections.emptyList();
        }

        void reset() {
            invocationCount.set(0);
            lastModelCode.set(null);
            lastBatchCodes.set(null);
        }
    }
}