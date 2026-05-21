package com.ruijie.authzengine.autoconfigure;

import com.alibaba.druid.pool.DruidDataSource;
import com.ruijie.authzengine.infrastructure.config.AuditMetaObjectHandler;
import java.util.LinkedHashMap;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * US1：Starter 配置属性绑定与默认值测试。
 * <p>
 * 验证 {@code rj.unify.engine.datasource.*}、{@code authz.engine.flyway.*} 与
 * {@code authz.engine.enabled} 的绑定正确性和默认值边界。
 * </p>
 */
class AuthzEnginePropertiesBindingTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(AuthzEngineAutoConfiguration.class, AuditMetaObjectHandler.class);

    private AuthzEngineDataSourceProperties bindDataSourceProperties(String... propertyValues) {
        AuthzEngineDataSourceProperties properties = new AuthzEngineDataSourceProperties();
        Binder binder = new Binder(new MapConfigurationPropertySource(toPropertyMap(propertyValues)));
        binder.bind("rj.unify.engine.datasource", Bindable.ofInstance(properties));
        return properties;
    }

    private AuthzEngineProperties bindEngineProperties(String... propertyValues) {
        AuthzEngineProperties properties = new AuthzEngineProperties();
        Binder binder = new Binder(new MapConfigurationPropertySource(toPropertyMap(propertyValues)));
        binder.bind("authz.engine", Bindable.ofInstance(properties));
        return properties;
    }

    private AuthzEngineFlywayProperties bindFlywayProperties(String... propertyValues) {
        AuthzEngineFlywayProperties properties = new AuthzEngineFlywayProperties();
        Binder binder = new Binder(new MapConfigurationPropertySource(toPropertyMap(propertyValues)));
        binder.bind("authz.engine.flyway", Bindable.ofInstance(properties));
        return properties;
    }

    private Map<String, Object> toPropertyMap(String... propertyValues) {
        Map<String, Object> propertyMap = new LinkedHashMap<>();
        for (String propertyValue : propertyValues) {
            int splitIndex = propertyValue.indexOf('=');
            propertyMap.put(propertyValue.substring(0, splitIndex), propertyValue.substring(splitIndex + 1));
        }
        return propertyMap;
    }

    // ---- authz.engine.enabled ----

    @Test
    void shouldDefaultEnabledToTrue() {
        AuthzEngineProperties props = bindEngineProperties();
        assertThat(props.isEnabled()).isTrue();
    }

    @Test
    void shouldSkipAllAuthzBeansWhenEnabledFalse() {
        // 当 authz.engine.enabled=false 时，整个自动装配被跳过（包括 Properties Bean）
        contextRunner
            .withPropertyValues("authz.engine.enabled=false")
            .run(context -> {
                assertThat(context).doesNotHaveBean("authzDataSource");
                assertThat(context).doesNotHaveBean("authzFlyway");
                assertThat(context).doesNotHaveBean("authzSqlSessionFactory");
                assertThat(context).doesNotHaveBean("authzTransactionManager");
            });
    }

    // ---- rj.unify.engine.datasource.* ----

    @Test
    void shouldBindDatasourceUrl() {
        AuthzEngineDataSourceProperties props = bindDataSourceProperties(
            "rj.unify.engine.datasource.url=jdbc:h2:mem:bind_test_ds;MODE=MYSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
            "rj.unify.engine.datasource.driver-class-name=org.h2.Driver",
            "rj.unify.engine.datasource.username=sa",
            "rj.unify.engine.datasource.password="
        );
        assertThat(props.getUrl()).startsWith("jdbc:h2:mem:bind_test_ds");
        assertThat(props.getDriverClassName()).isEqualTo("org.h2.Driver");
        assertThat(props.getUsername()).isEqualTo("sa");
        assertThat(props.isConfigured()).isTrue();
    }

    @Test
    void shouldReportNotConfiguredWhenUrlMissing() {
        AuthzEngineDataSourceProperties props = bindDataSourceProperties();
        assertThat(props.isConfigured()).isFalse();
    }

    @Test
    void shouldDefaultDriverAndTypeToMysqlCjAndDruid() {
        AuthzEngineDataSourceProperties props = bindDataSourceProperties();
        assertThat(props.getDriverClassName()).isEqualTo("com.mysql.cj.jdbc.Driver");
        assertThat(props.getType()).isEqualTo(DruidDataSource.class);
    }

    @Test
    void shouldBindDruidPoolPropertiesAndApplyThemToDatasource() {
        AuthzEngineDataSourceProperties props = bindDataSourceProperties(
            "rj.unify.engine.datasource.url=jdbc:h2:mem:bind_test_druid;MODE=MYSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
            "rj.unify.engine.datasource.driver-class-name=org.h2.Driver",
            "rj.unify.engine.datasource.username=sa",
            "rj.unify.engine.datasource.password=",
            "rj.unify.engine.datasource.type=com.alibaba.druid.pool.DruidDataSource",
            "rj.unify.engine.datasource.initial-size=5",
            "rj.unify.engine.datasource.min-idle=5",
            "rj.unify.engine.datasource.max-active=20",
            "rj.unify.engine.datasource.max-wait=60000",
            "rj.unify.engine.datasource.time-between-eviction-runs-millis=30000",
            "rj.unify.engine.datasource.min-evictable-idle-time-millis=30000",
            "rj.unify.engine.datasource.validation-query=SELECT 1",
            "rj.unify.engine.datasource.test-while-idle=true",
            "rj.unify.engine.datasource.test-on-borrow=false",
            "rj.unify.engine.datasource.test-on-return=false",
            "rj.unify.engine.datasource.pool-prepared-statements=true",
            "rj.unify.engine.datasource.max-pool-prepared-statement-per-connection-size=20"
        );
        assertThat(props.getInitialSize()).isEqualTo(5);
        assertThat(props.getMinIdle()).isEqualTo(5);
        assertThat(props.getMaxActive()).isEqualTo(20);
        assertThat(props.getMaxWait()).isEqualTo(60000L);
        assertThat(props.getTimeBetweenEvictionRunsMillis()).isEqualTo(30000L);
        assertThat(props.getMinEvictableIdleTimeMillis()).isEqualTo(30000L);
        assertThat(props.getValidationQuery()).isEqualTo("SELECT 1");
        assertThat(props.getTestWhileIdle()).isTrue();
        assertThat(props.getTestOnBorrow()).isFalse();
        assertThat(props.getTestOnReturn()).isFalse();
        assertThat(props.getPoolPreparedStatements()).isTrue();
        assertThat(props.getMaxPoolPreparedStatementPerConnectionSize()).isEqualTo(20);

        DruidDataSource dataSource = (DruidDataSource) props.buildDataSource();
        assertThat(dataSource.getInitialSize()).isEqualTo(5);
        assertThat(dataSource.getMinIdle()).isEqualTo(5);
        assertThat(dataSource.getMaxActive()).isEqualTo(20);
        assertThat(dataSource.getMaxWait()).isEqualTo(60000L);
        assertThat(dataSource.getTimeBetweenEvictionRunsMillis()).isEqualTo(30000L);
        assertThat(dataSource.getMinEvictableIdleTimeMillis()).isEqualTo(30000L);
        assertThat(dataSource.getValidationQuery()).isEqualTo("SELECT 1");
        assertThat(dataSource.isTestWhileIdle()).isTrue();
        assertThat(dataSource.isTestOnBorrow()).isFalse();
        assertThat(dataSource.isTestOnReturn()).isFalse();
        assertThat(dataSource.isPoolPreparedStatements()).isTrue();
        assertThat(dataSource.getMaxPoolPreparedStatementPerConnectionSize()).isEqualTo(20);
        dataSource.close();
    }

    // ---- authz.engine.flyway.* ----

    @Test
    void shouldDefaultFlywayTableToAuthzFlywayHistory() {
        AuthzEngineFlywayProperties props = bindFlywayProperties();
        assertThat(props.getTable()).isEqualTo("authz_flyway_history");
    }

    @Test
    void shouldDefaultFlywayLocationsToAuthzMigrationPath() {
        AuthzEngineFlywayProperties props = bindFlywayProperties();
        List<String> locations = props.getLocations();
        assertThat(locations).hasSize(1);
        assertThat(locations.get(0)).isEqualTo("classpath:db/authz-migration");
    }

    @Test
    void shouldBindCustomFlywayTable() {
        AuthzEngineFlywayProperties props = bindFlywayProperties("authz.engine.flyway.table=custom_authz_history");
        assertThat(props.getTable()).isEqualTo("custom_authz_history");
    }

    @Test
    void shouldDefaultFlywayEnabledToTrue() {
        AuthzEngineFlywayProperties props = bindFlywayProperties();
        assertThat(props.isEnabled()).isTrue();
    }

    @Test
    void shouldBindFlywayEnabledFalse() {
        AuthzEngineFlywayProperties props = bindFlywayProperties("authz.engine.flyway.enabled=false");
        assertThat(props.isEnabled()).isFalse();
    }
}
