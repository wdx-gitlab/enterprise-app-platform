package com.ruijie.dapengine.autoconfigure;

import com.alibaba.druid.pool.DruidDataSource;
import com.ruijie.dapengine.admin.controller.GlobalExceptionHandler;
import com.ruijie.dapengine.sdk.MasterDataMetaService;
import com.ruijie.dapengine.common.util.AesCipher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.*;

/**
 * DapEngineAutoConfiguration 集成验证测试。
 * 使用 H2 内存数据库替代真实 MySQL，验证自动装配、Flyway 迁移和配置校验。
 */
@RunWith(SpringRunner.class)
public class DapEngineAutoConfigurationTest {

    private static final String VALID_URL =
        "jdbc:h2:mem:dap_test;DB_CLOSE_DELAY=-1;MODE=MySQL";
    private static final String VALID_KEY = "TestEncryptKey32CharactersLong!!";
    /** H2 URL 模板，%s 替换为不同的 DB 名以隔离各测试。 */
    private static final String H2_URL_TPL =
        "jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1;MODE=MySQL";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(JacksonAutoConfiguration.class, DapEngineAutoConfiguration.class);
    // US1: 正常启动验证
    // =========================================================================

    @Test
    public void should_start_successfully_with_valid_config() {
        contextRunner
            .withPropertyValues(
                "dap.engine.tenant-id=test_tenant",
                "dap.engine.app-code=TEST_APP",
                "rj.unify.engine.datasource.url=" + VALID_URL,
                "rj.unify.engine.datasource.username=sa",
                "rj.unify.engine.datasource.password=",
                "dap.engine.security.encrypt-key=" + VALID_KEY
            )
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasSingleBean(DapEngineDataSource.class);
                assertThat(context.getBean(DapEngineDataSource.class).getDataSource())
                    .isInstanceOf(DruidDataSource.class);
                assertThat(context).hasSingleBean(DapEngineJdbcTemplate.class);
                assertThat(context).hasSingleBean(MasterDataMetaService.class);
                assertThat(context).hasSingleBean(AesCipher.class);
                assertThat(context).hasBean("dapCacheManager");
            });
    }

    @Test
    public void should_create_system_tables_via_flyway() {
        String url = String.format(H2_URL_TPL, "dap_tables");
        contextRunner
            .withPropertyValues(
                "dap.engine.tenant-id=test_tenant",
                "dap.engine.app-code=TEST_APP",
                "rj.unify.engine.datasource.url=" + url,
                "rj.unify.engine.datasource.username=sa",
                "rj.unify.engine.datasource.password=",
                "dap.engine.security.encrypt-key=" + VALID_KEY
            )
            .run(context -> {
                assertThat(context).hasNotFailed();
                // 通过 JdbcTemplate 直接查询表行数验证表存在（比 metadata API 更可靠）
                DapEngineJdbcTemplate dapJt = context.getBean(DapEngineJdbcTemplate.class);
                String[] expectedTables = {
                    "dap_sys_subject", "dap_sys_metadata_config",
                    "dap_sys_sync_config", "dap_sys_checkpoint", "dap_sys_sync_log"
                };
                for (String table : expectedTables) {
                    Integer count = dapJt.getJdbcTemplate()
                        .queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
                    assertThat(count).as("系统表 %s 应可查询（SELECT COUNT(*)=0 也表示表存在）", table)
                        .isNotNull();
                }
            });
    }

    @Test
    public void flyway_migration_should_be_idempotent() {
        // 使用同一 URL 两次启动，模拟重启；H2 内存库已存在时 Flyway 应保持幂等
        String url = String.format(H2_URL_TPL, "dap_idempotent");
        ApplicationContextRunner runner = contextRunner
            .withPropertyValues(
                "dap.engine.tenant-id=t",
                "dap.engine.app-code=A",
                "rj.unify.engine.datasource.url=" + url,
                "rj.unify.engine.datasource.username=sa",
                "rj.unify.engine.datasource.password=",
                "dap.engine.security.encrypt-key=" + VALID_KEY
            );
        runner.run(context -> assertThat(context).hasNotFailed());
        runner.run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    public void should_not_register_beans_when_disabled() {
        contextRunner
            .withPropertyValues("dap.engine.enabled=false")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean(DapEngineDataSource.class);
                assertThat(context).doesNotHaveBean(DapEngineJdbcTemplate.class);
            });
    }

    @Test
    public void should_use_caffeine_when_redis_not_configured() {
        String url = String.format(H2_URL_TPL, "dap_cache");
        contextRunner
            .withPropertyValues(
                "dap.engine.tenant-id=t",
                "dap.engine.app-code=A",
                "rj.unify.engine.datasource.url=" + url,
                "rj.unify.engine.datasource.username=sa",
                "rj.unify.engine.datasource.password=",
                "dap.engine.security.encrypt-key=" + VALID_KEY
            )
            .run(context -> {
                assertThat(context).hasNotFailed();
                CacheManager cm = context.getBean("dapCacheManager", CacheManager.class);
                assertThat(cm).isInstanceOf(
                    org.springframework.cache.caffeine.CaffeineCacheManager.class);
            });
    }

    @Test
    public void should_not_conflict_with_existing_global_exception_handler_bean_name() {
        String url = String.format(H2_URL_TPL, "dap_existing_global_exception_handler");
        new ApplicationContextRunner()
            .withUserConfiguration(ExistingGlobalExceptionHandlerConfiguration.class,
                JacksonAutoConfiguration.class, DapEngineAutoConfiguration.class)
            .withPropertyValues(
                "dap.engine.tenant-id=test_tenant",
                "dap.engine.app-code=TEST_APP",
                "rj.unify.engine.datasource.url=" + url,
                "rj.unify.engine.datasource.username=sa",
                "rj.unify.engine.datasource.password=",
                "dap.engine.security.encrypt-key=" + VALID_KEY
            )
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasBean("globalExceptionHandler");
                assertThat(context).hasBean("dapEngineGlobalExceptionHandler");
                assertThat(context).hasSingleBean(GlobalExceptionHandler.class);
            });
    }

    // =========================================================================
    // US2: 配置缺失时快速失败
    // =========================================================================

    @Test
    public void should_fail_when_tenant_id_missing() {
        contextRunner
            .withPropertyValues(
                "dap.engine.app-code=APP",
                "rj.unify.engine.datasource.url=" + VALID_URL,
                "rj.unify.engine.datasource.username=sa",
                "rj.unify.engine.datasource.password=",
                "dap.engine.security.encrypt-key=" + VALID_KEY
            )
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure())
                    .hasMessageContaining("tenant-id");
            });
    }

    @Test
    public void should_fail_when_app_code_missing() {
        contextRunner
            .withPropertyValues(
                "dap.engine.tenant-id=t",
                "rj.unify.engine.datasource.url=" + VALID_URL,
                "rj.unify.engine.datasource.username=sa",
                "rj.unify.engine.datasource.password=",
                "dap.engine.security.encrypt-key=" + VALID_KEY
            )
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure())
                    .hasMessageContaining("app-code");
            });
    }

    @Test
    public void should_fail_when_datasource_url_missing() {
        contextRunner
            .withPropertyValues(
                "dap.engine.tenant-id=t",
                "dap.engine.app-code=A",
                "rj.unify.engine.datasource.username=sa",
                "rj.unify.engine.datasource.password=",
                "dap.engine.security.encrypt-key=" + VALID_KEY
            )
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure())
                    .hasMessageContaining("datasource.url");
            });
    }

    @Test
    public void should_fail_when_encrypt_key_missing() {
        contextRunner
            .withPropertyValues(
                "dap.engine.tenant-id=t",
                "dap.engine.app-code=A",
                "rj.unify.engine.datasource.url=" + VALID_URL,
                "rj.unify.engine.datasource.username=sa",
                "rj.unify.engine.datasource.password="
            )
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure())
                    .hasMessageContaining("encrypt-key");
            });
    }

    @Test
    public void should_fail_when_business_db_mode_without_datasource_bean() {
        contextRunner
            .withPropertyValues(
                "dap.engine.tenant-id=t",
                "dap.engine.app-code=A",
                "rj.unify.engine.datasource.url=" + VALID_URL,
                "rj.unify.engine.datasource.username=sa",
                "rj.unify.engine.datasource.password=",
                "dap.engine.security.encrypt-key=" + VALID_KEY,
                "dap.engine.sync.target-storage=BUSINESS_DB"
            )
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure())
                    .hasMessageContaining("dataSource");
            });
    }

    @Configuration
    static class ExistingGlobalExceptionHandlerConfiguration {

        @Bean(name = "globalExceptionHandler")
        public Object globalExceptionHandler() {
            return new Object();
        }
    }
}
