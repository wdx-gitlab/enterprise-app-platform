package com.ruijie.authzengine.infrastructure.authz;

import com.ruijie.authzengine.domain.model.decision.AuthzRequest;
import com.ruijie.authzengine.domain.model.decision.AuthzSubject;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * RuntimeLookupService 运行时关系展开集成测试。
 */
@SpringBootTest
@ActiveProfiles("test")
class RuntimeLookupServiceIntegrationTest {

    @Autowired
    private RuntimeLookupService runtimeLookupService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldMergeRoleRelationWhenRelationUsesGovernanceSubjectId() {
        jdbcTemplate.update(
            "INSERT INTO dap_sys_user (id, tenant_id, app_code, staff_no, user_id, staff_name, org_id, status, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            95200L, "T001", "CRM-RUNTIME-LOOKUP", "U-WDX-LOOKUP", "wangdaoxin", "王道鑫", null, "ENABLED", "test", "test", 0
        );
        jdbcTemplate.update(
            "INSERT INTO authz_subject_relation (id, tenant_id, app_code, subject_model, subject_id, related_subject_model, related_subject_id, relation_type, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            95201L, "T001", "CRM-RUNTIME-LOOKUP", "SUB_USER", "95200", "SUB_ROLE", "viewer", "HAS_ROLE", "test", "test", 0
        );

        Map<String, Object> attributes = new LinkedHashMap<>();
        runtimeLookupService.mergeRelatedSubjects(AuthzRequest.builder()
            .tenantId("T001")
            .appCode("CRM-RUNTIME-LOOKUP")
            .subject(AuthzSubject.builder().id("wangdaoxin").type("SUB_USER").build())
            .build(), attributes);

        Assertions.assertEquals(Collections.singletonList("viewer"), attributes.get("roles"));
    }

    @Test
    void shouldMergeRoleRelationWhenSubjectUsesUserId() {
        jdbcTemplate.update(
            "INSERT INTO dap_sys_user (id, tenant_id, app_code, staff_no, user_id, staff_name, org_id, status, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            95210L, "T001", "CRM-RUNTIME-LOOKUP", "R13174", "wangdaoxin-userid", "王道鑫", null, "ENABLED", "test", "test", 0
        );
        jdbcTemplate.update(
            "INSERT INTO authz_subject_relation (id, tenant_id, app_code, subject_model, subject_id, related_subject_model, related_subject_id, relation_type, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            95211L, "T001", "CRM-RUNTIME-LOOKUP", "SUB_USER", "95210", "SUB_ROLE", "editor", "HAS_ROLE", "test", "test", 0
        );

        Map<String, Object> attributes = new LinkedHashMap<>();
        runtimeLookupService.mergeRelatedSubjects(AuthzRequest.builder()
            .tenantId("T001")
            .appCode("CRM-RUNTIME-LOOKUP")
            .subject(AuthzSubject.builder().id("wangdaoxin-userid").type("SUB_USER").build())
            .build(), attributes);

        Assertions.assertEquals(Collections.singletonList("editor"), attributes.get("roles"));
    }
}