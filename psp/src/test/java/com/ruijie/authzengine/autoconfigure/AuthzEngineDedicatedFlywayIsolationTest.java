package com.ruijie.authzengine.autoconfigure;

import com.ruijie.authzengine.infrastructure.config.AuditMetaObjectHandler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * US1：宿主业务库迁移隔离测试。
 * <p>
 * 验证启用权限引擎专属数据源后，authz-engine 的 Flyway 迁移脚本
 * 只在专属权限库执行，宿主业务库不会出现权限引擎系统表。
 * </p>
 */
class AuthzEngineDedicatedFlywayIsolationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(AuthzEngineAutoConfiguration.class, AuditMetaObjectHandler.class);

    @Test
    void shouldNotPolluteHostDatabaseWhenAuthzDatasourceConfigured() {
        contextRunner
            .withPropertyValues(
                // 宿主业务库（空 H2，无 authz 系统表）
                "spring.datasource.url=jdbc:h2:mem:host_biz_db_isolation;MODE=MYSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                // 专属权限库（独立 H2，接收 authz-migration）
                "rj.unify.engine.datasource.url=jdbc:h2:mem:authz_engine_isolation;MODE=MYSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
                "rj.unify.engine.datasource.driver-class-name=org.h2.Driver",
                "rj.unify.engine.datasource.username=sa",
                "rj.unify.engine.datasource.password=",
                "authz.engine.flyway.table=authz_flyway_history_isolation"
            )
            .run(context -> {
                // 专属权限库的 Flyway 应执行完毕
                assertThat(context).hasBean("authzDataSource");
                assertThat(context).hasBean("authzFlyway");

                DataSource authzDataSource = context.getBean("authzDataSource", DataSource.class);
                JdbcTemplate authzJdbc = new JdbcTemplate(authzDataSource);

                // 专属权限库应包含权限引擎系统表
                Integer authzHistoryCount = authzJdbc.queryForObject(
                    "SELECT COUNT(*) FROM authz_flyway_history_isolation",
                    Integer.class
                );
                assertThat(authzHistoryCount).isGreaterThan(0);

                // 专属权限库应包含全局标准动作字典（由 authz-migration 初始化）
                Integer globalActionCount = authzJdbc.queryForObject(
                    "SELECT COUNT(*) FROM authz_std_act_dict WHERE tenant_id = '__GLOBAL__'",
                    Integer.class
                );
                assertThat(globalActionCount).isGreaterThanOrEqualTo(4);

                // 专属权限库不应包含租户级 demo 数据（纯基线场景）
                Integer authMetaModelCount = authzJdbc.queryForObject(
                    "SELECT COUNT(*) FROM authz_meta_model WHERE tenant_id = 'T001'",
                    Integer.class
                );
                assertThat(authMetaModelCount).isZero();
            });
    }

    @Test
    void shouldNotCreateAuthzTablesInHostDatabaseSchemaScope() {
        // 用一个独立的裸 H2 作为模拟宿主业务库，验证它不被 authz-migration 污染
        contextRunner
            .withPropertyValues(
                "rj.unify.engine.datasource.url=jdbc:h2:mem:authz_engine_isolation_chk;MODE=MYSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
                "rj.unify.engine.datasource.driver-class-name=org.h2.Driver",
                "rj.unify.engine.datasource.username=sa",
                "rj.unify.engine.datasource.password=",
                "authz.engine.flyway.table=authz_flyway_history_chk"
            )
            .run(context -> {
                DataSource authzDataSource = context.getBean("authzDataSource", DataSource.class);
                JdbcTemplate authzJdbc = new JdbcTemplate(authzDataSource);

                // 标准动作字典应在专属权限库中存在
                Integer globalActionCount = authzJdbc.queryForObject(
                    "SELECT COUNT(*) FROM authz_std_act_dict WHERE tenant_id = '__GLOBAL__'",
                    Integer.class
                );
                assertThat(globalActionCount).isGreaterThanOrEqualTo(4);

                // 专属权限库不应有任何租户级别的 demo 数据（纯基线）
                Integer tenantMetaModelCount = authzJdbc.queryForObject(
                    "SELECT COUNT(*) FROM authz_meta_model WHERE tenant_id = 'T001'",
                    Integer.class
                );
                assertThat(tenantMetaModelCount).isZero();
            });
    }

    @Test
    void shouldSkipAuthzFlywayWhenFlywayDisabled() {
        contextRunner
            .withPropertyValues(
                "rj.unify.engine.datasource.url=jdbc:h2:mem:authz_engine_flyway_disabled;MODE=MYSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
                "rj.unify.engine.datasource.driver-class-name=org.h2.Driver",
                "rj.unify.engine.datasource.username=sa",
                "rj.unify.engine.datasource.password=",
                "authz.engine.flyway.enabled=false"
            )
            .run(context -> {
                assertThat(context).hasBean("authzDataSource");
                assertThat(context).doesNotHaveBean("authzFlyway");
                assertThat(context).doesNotHaveBean("authzFlywayInitializer");
            });
    }
}
