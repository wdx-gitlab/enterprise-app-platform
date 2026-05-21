package com.ruijie.authzengine.infrastructure.persistence;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StreamUtils;
import org.springframework.test.context.ActiveProfiles;

/**
 * 核心治理骨架 Flyway 迁移验证。
 */
@SpringBootTest
@ActiveProfiles("test")
class FlywayCoreSchemaMigrationTest {

    private static final List<String> REQUIRED_TABLES = Arrays.asList(
        "AUTHZ_META_MODEL",
        "AUTHZ_PERMISSION_ITEM",
        "AUTHZ_ASSIGNMENT",
        "AUTHZ_BO_META_MODEL",
        "AUTHZ_STD_ACT_DICT",
        "AUTHZ_STD_POL_TEMPLATE",
        "DAP_SYS_ORG",
        "AUTHZ_SUBJECT_RELATION",
        "DAP_SYS_USER",
        "AUTHZ_USERGROUP",
        "AUTHZ_POSITION",
        "AUTHZ_ROLE",
        "USP_MENU_ITEM",
        "USP_PAGE",
        "USP_COMPONENT",
        "USP_API",
        "AUTHZ_RES_DERIVATION_PERM",
        "AUTHZ_ASSIGNMENT_DELEGATE",
        "AUTHZ_AUDIT_LOG"
    );

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldLoadAllCoreTables() {
        for (String tableName : REQUIRED_TABLES) {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = ?",
                Integer.class,
                tableName
            );
            Assertions.assertEquals(1, count, "缺少 Flyway 表: " + tableName);
        }
    }

    @Test
    void shouldPreserveV1BaselineAndAddColumns() {
        Integer schemaViewCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = 'AUTHZ_META_MODEL' AND COLUMN_NAME = 'SCHEMA_VIEW'",
            Integer.class
        );
        Integer actionSeedCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM authz_std_act_dict WHERE tenant_id = '__GLOBAL__'",
            Integer.class
        );
        Integer actionAliasColumnCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = 'AUTHZ_STD_ACT_DICT' AND COLUMN_NAME = 'ACT_ALIASES'",
            Integer.class
        );
        Integer templateSeedCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM authz_std_pol_template WHERE tenant_id = '__GLOBAL__'",
            Integer.class
        );
        String approveAliases = jdbcTemplate.queryForObject(
            "SELECT act_aliases FROM authz_std_act_dict WHERE tenant_id = '__GLOBAL__' AND act_code = 'APPROVE'",
            String.class
        );
        Integer expressionColumnCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = 'AUTHZ_STD_POL_TEMPLATE' AND COLUMN_NAME = 'EXPRESSION_SCRIPT'",
            Integer.class
        );
        Integer policyTypeColumnCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = 'AUTHZ_STD_POL_TEMPLATE' AND COLUMN_NAME = 'POL_TYPE'",
            Integer.class
        );

        Assertions.assertEquals(1, schemaViewCount);
        Assertions.assertTrue(actionSeedCount >= 4, "应保留全局标准动作基线数据");
        Assertions.assertEquals(1, actionAliasColumnCount);
        Assertions.assertTrue(templateSeedCount >= 3, "应保留全局策略模板基线数据");
        Assertions.assertTrue(approveAliases.contains("APPROVE_REQUEST"));
        Assertions.assertEquals(1, expressionColumnCount);
        Assertions.assertEquals(1, policyTypeColumnCount);
    }

    @Test
    void shouldKeepAuditHookTraceColumnsInBaselineSchema() {
        Integer hookStatusColumnCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = 'AUTHZ_AUDIT_LOG' AND COLUMN_NAME = 'HOOK_STATUS'",
            Integer.class
        );
        Integer hookCostMsColumnCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = 'AUTHZ_AUDIT_LOG' AND COLUMN_NAME = 'HOOK_COST_MS'",
            Integer.class
        );
        Integer attributeSnapshotColumnCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = 'AUTHZ_AUDIT_LOG' AND COLUMN_NAME = 'ATTRIBUTE_SNAPSHOT'",
            Integer.class
        );
        Assertions.assertEquals(1, hookStatusColumnCount, "基线表结构应包含 hook_status 字段");
        Assertions.assertEquals(1, hookCostMsColumnCount, "基线表结构应包含 hook_cost_ms 字段");
        Assertions.assertEquals(1, attributeSnapshotColumnCount, "基线表结构应包含 attribute_snapshot 字段");
    }

    @Test
    void shouldKeepFlywayScriptMysqlFriendly() throws Exception {
        String migrationScript = StreamUtils.copyToString(
            new ClassPathResource("db/authz-migration/V2__authz_engine_baseline_data.sql").getInputStream(),
            StandardCharsets.UTF_8
        );

        Assertions.assertFalse(migrationScript.contains("ADD COLUMN IF NOT EXISTS"));
        Assertions.assertFalse(migrationScript.contains("CREATE INDEX IF NOT EXISTS"));
    }
}