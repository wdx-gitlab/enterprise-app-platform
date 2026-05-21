package com.ruijie.authzengine.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * 验证新分层 Flyway 路径的边界：authz 基线与 demo 样例数据解耦。
 */
class FlywaySplitBootstrapMigrationTest {

    @Test
    void shouldBootstrapAuthzBaselineWithoutTenantDemoSeeds() {
        JdbcTemplate jdbcTemplate = migrate(
            "jdbc:h2:mem:authz_split_baseline;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
            "classpath:db/authz-migration"
        );

        Integer tableCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = 'AUTHZ_META_MODEL'",
            Integer.class
        );
        Integer globalActionCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM authz_std_act_dict WHERE tenant_id = '__GLOBAL__'",
            Integer.class
        );
        Integer globalTemplateCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM authz_std_pol_template WHERE tenant_id = '__GLOBAL__'",
            Integer.class
        );
        Integer tenantMetaModelCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM authz_meta_model WHERE tenant_id = 'T001' AND app_code = 'CRM'",
            Integer.class
        );
        Integer tenantPermissionCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM authz_permission_item WHERE tenant_id = 'T001' AND app_code = 'CRM'",
            Integer.class
        );
        Integer tenantUserCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM dap_sys_user WHERE tenant_id = 'T001' AND app_code = 'CRM'",
            Integer.class
        );

        Assertions.assertEquals(1, tableCount);
        Assertions.assertTrue(globalActionCount >= 4);
        Assertions.assertTrue(globalTemplateCount >= 3);
        Assertions.assertEquals(0, tenantMetaModelCount);
        Assertions.assertEquals(0, tenantPermissionCount);
        Assertions.assertEquals(0, tenantUserCount);
    }

    private JdbcTemplate migrate(String jdbcUrl, String... locations) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl(jdbcUrl);
        dataSource.setUsername("sa");
        dataSource.setPassword("");

        Flyway.configure()
            .dataSource(dataSource)
            .locations(locations)
            .load()
            .migrate();
        return new JdbcTemplate(dataSource);
    }
}
