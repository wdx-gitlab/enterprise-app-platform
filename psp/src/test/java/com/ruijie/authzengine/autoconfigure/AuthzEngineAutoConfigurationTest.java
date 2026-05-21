package com.ruijie.authzengine.autoconfigure;

import com.alibaba.druid.pool.DruidDataSource;
import com.ruijie.authzengine.application.service.AuthzFacade;
import com.ruijie.authzengine.domain.spi.AuthzSubjectProvider;
import com.ruijie.authzengine.infrastructure.config.AuditMetaObjectHandler;
import com.ruijie.authzengine.infrastructure.authz.AuthzHttpPepFilter;
import com.ruijie.authzengine.infrastructure.persistence.mapper.AuthMetaModelMapper;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AuthzEngineAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(AuthzEngineAutoConfiguration.class, AuditMetaObjectHandler.class);


    @Test
    void shouldCreateDedicatedDatasourceAndFlywayWhenConfigured() {
        contextRunner
            .withPropertyValues(
                "rj.unify.engine.datasource.url=jdbc:h2:mem:authz_engine_cfg;MODE=MYSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
                "rj.unify.engine.datasource.driver-class-name=org.h2.Driver",
                "rj.unify.engine.datasource.username=sa",
                "rj.unify.engine.datasource.password=",
                "authz.engine.flyway.table=authz_flyway_history_cfg"
            )
            .run(context -> {
                assertThat(context).hasBean("authzDataSource");
                assertThat(context).hasBean("authzFlyway");
                assertThat(context).hasBean("authzFlywayInitializer");
                assertThat(context).hasBean("authzSqlSessionFactory");
                assertThat(context).hasBean("authzTransactionManager");
                assertThat(context).hasSingleBean(AuthMetaModelMapper.class);

                DataSource dataSource = context.getBean("authzDataSource", DataSource.class);
                assertThat(dataSource).isInstanceOf(DruidDataSource.class);
                JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
                AuthMetaModelMapper authMetaModelMapper = context.getBean(AuthMetaModelMapper.class);
                Integer historyCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM authz_flyway_history_cfg",
                    Integer.class
                );
                Integer metaModelCount = authMetaModelMapper.selectCount(null);
                Integer boMetaModelCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM authz_bo_meta_model",
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
                Integer tenantUserCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM dap_sys_user WHERE tenant_id = 'T001' AND app_code = 'CRM'",
                    Integer.class
                );

                assertThat(historyCount).isNotNull();
                assertThat(historyCount).isGreaterThan(0);
                assertThat(metaModelCount).isZero();
                assertThat(boMetaModelCount).isZero();
                assertThat(globalActionCount).isGreaterThanOrEqualTo(4);
                assertThat(globalTemplateCount).isGreaterThanOrEqualTo(3);
                assertThat(tenantMetaModelCount).isZero();
                assertThat(tenantUserCount).isZero();
            });
    }

    @Test
    void shouldSkipDedicatedBeansWhenAuthzEngineDisabled() {
        contextRunner
            .withPropertyValues(
                "authz.engine.enabled=false",
                "rj.unify.engine.datasource.url=jdbc:h2:mem:authz_engine_disabled;MODE=MYSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
                "rj.unify.engine.datasource.driver-class-name=org.h2.Driver",
                "rj.unify.engine.datasource.username=sa",
                "rj.unify.engine.datasource.password="
            )
            .run(context -> {
                assertThat(context).doesNotHaveBean("authzDataSource");
                assertThat(context).doesNotHaveBean("authzFlyway");
                assertThat(context).doesNotHaveBean("authzSqlSessionFactory");
                assertThat(context).doesNotHaveBean("authzTransactionManager");
            });
    }

    @Test
    void shouldRegisterHttpPepFilterWhenPepEnabled() {
        contextRunner
            .withPropertyValues(
                "rj.unify.engine.datasource.url=jdbc:h2:mem:authz_engine_pep;MODE=MYSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
                "rj.unify.engine.datasource.driver-class-name=org.h2.Driver",
                "rj.unify.engine.datasource.username=sa",
                "rj.unify.engine.datasource.password=",
                "rj.unify.engine.datasource.validation-query=SELECT 1",
                "authz.engine.tenant-id=T001",
                "authz.engine.app-code=CRM",
                "authz.engine.pep.enabled=true"
            )
            .withBean(AuthzFacade.class, () -> mock(AuthzFacade.class))
            .withBean(AuthzSubjectProvider.class, () -> new AuthzSubjectProvider() {
                @Override
                public String getCurrentUserId() {
                    return null;
                }
            })
            .run(context -> {
                FilterRegistrationBean<?> registration = context.getBean("authzHttpPepFilter", FilterRegistrationBean.class);

                assertThat(context).hasSingleBean(AuthzSubjectProvider.class);
                assertThat(context).hasBean("authzHttpPepFilter");
                assertThat(registration.getFilter()).isInstanceOf(AuthzHttpPepFilter.class);
            });
    }
}