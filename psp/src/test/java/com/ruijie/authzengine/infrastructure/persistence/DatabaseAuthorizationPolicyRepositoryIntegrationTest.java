package com.ruijie.authzengine.infrastructure.persistence;

import com.ruijie.authzengine.domain.model.common.SubjectKey;
import com.ruijie.authzengine.domain.model.decision.AuthzContext;
import com.ruijie.authzengine.domain.model.decision.PermissionGrant;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * 数据库授权仓储集成测试，验证模板主键到模板编码的最小解析链路。
 */
@SpringBootTest
@ActiveProfiles("test")
class DatabaseAuthorizationPolicyRepositoryIntegrationTest {

    @Autowired
    private DatabaseAuthorizationPolicyRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldResolvePolicyTemplateCodeFromAssignment() {
        jdbcTemplate.update(
            "INSERT INTO authz_permission_item (id, tenant_id, app_code, perm_code, res_model_code, res_id, act_code, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            19001L, "T001", "CRM", "INVOICE_APPROVE", "INVOICE", "", "APPROVE", "test", "test", 0
        );
        jdbcTemplate.update(
            "INSERT INTO authz_std_pol_template (id, tenant_id, template_code, template_name, pol_type, expression_script, param_schema, status, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            19301L, "__GLOBAL__", "WORK_HOUR_ONLY", "工作时间限制", "ENV", "env.hour >= 9 && env.hour <= 18", "{\"type\":\"object\"}", "ENABLED", "test", "test", 0
        );
        jdbcTemplate.update(
            "INSERT INTO authz_assignment (id, tenant_id, app_code, subject_id, subject_model, perm_item_id, policy_tpl_id, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            19201L, "T001", "CRM", "templated-user", "SUB_USER", 19001L, 19301L, "test", "test", 0
        );

        List<PermissionGrant> grants = repository.findBySubjects(
            "T001",
            "CRM",
            Collections.singleton(new SubjectKey("SUB_USER", "templated-user"))
        );

        Assertions.assertEquals(1, grants.size());
        Assertions.assertEquals("WORK_HOUR_ONLY", grants.get(0).getPolicyTemplateCode());
    }

    @Test
    void shouldMergeDelegationGrantWhenDelegationIsActive() {
        jdbcTemplate.update(
            "INSERT INTO authz_permission_item (id, tenant_id, app_code, perm_code, res_model_code, res_id, act_code, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            19011L, "T001", "CRM", "CONTRACT_APPROVE_DELEGATE", "DELEGATION_CASE", "", "APPROVE", "test", "test", 0
        );
        jdbcTemplate.update(
            "INSERT INTO authz_assignment_delegate (id, tenant_id, app_code, grantor_subject_model, grantor_subject_id, delegate_subject_model, delegate_subject_id, perm_item_id, start_time, end_time, status, reason, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, DATEADD('DAY', 1, CURRENT_TIMESTAMP), ?, ?, ?, ?, ?)",
            19111L, "T001", "CRM", "SUB_USER", "grantor-user", "SUB_USER", "delegate-user", 19011L, "ACTIVE", "请假期间委托", "test", "test", 0
        );

        List<PermissionGrant> grants = repository.findBySubjects(
            "T001",
            "CRM",
            Collections.singleton(new SubjectKey("SUB_USER", "delegate-user")),
            AuthzContext.builder()
                .delegationIds(Collections.singleton("19111"))
                .build()
        );

        Assertions.assertEquals(1, grants.size());
        Assertions.assertEquals("CONTRACT_APPROVE_DELEGATE", grants.get(0).getPermissionCode());
        Assertions.assertEquals("19111", grants.get(0).getDelegateId());
    }
}