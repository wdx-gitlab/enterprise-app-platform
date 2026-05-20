package com.ruijie.uspportal.config.datasource;

import com.alibaba.druid.pool.DruidDataSource;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.logging.stdout.StdOutImpl;
import org.apache.ibatis.session.SqlSessionFactory;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;

/**
 * USP 独立数据源配置。
 *
 * <p>负责装配 USP 模块专属的数据源、事务管理器、MyBatis 会话工厂与 Flyway 迁移能力。</p>
 */
@Configuration
@EnableTransactionManagement
@MapperScan(
    basePackages = {
        "com.ruijie.uspportal.auth.mapper",
        "com.ruijie.uspportal.tenant.mapper",
        "com.ruijie.uspportal.appregistry.mapper",
        "com.ruijie.uspportal.navigation.mapper",
        "com.ruijie.uspportal.workbench.mapper",
        "com.ruijie.uspportal.portalconfig.mapper",
        "com.ruijie.uspportal.eventbus.mapper"
    },
    sqlSessionFactoryRef = "uengineSqlSessionFactory",
    sqlSessionTemplateRef = "uengineSqlSessionTemplate"
)
public class UengineDataSourceConfiguration {

    @Bean(name = "uengineDataSource")
    @Primary
    @ConfigurationProperties(prefix = "rj.unify.engine.datasource")
    /**
     * 创建 USP 模块独立数据源。
     *
     * @return USP 数据源
     */
    public DataSource uengineDataSource() {
        return DataSourceBuilder.create()
            .type(DruidDataSource.class)
            .build();
    }

    /**
     * 创建 USP 数据源对应的事务管理器。
     *
     * @param uengineDataSource USP 数据源
     * @return 事务管理器
     */
    @Bean(name = "uengineTransactionManager")
    public PlatformTransactionManager uengineTransactionManager(
        @Qualifier("uengineDataSource") DataSource uengineDataSource
    ) {
        return new DataSourceTransactionManager(uengineDataSource);
    }

    /**
     * 创建 USP 模块的 MyBatis 会话工厂。
     *
     * @param uengineDataSource USP 数据源
     * @param mapperLocations Mapper 资源路径
     * @param typeHandlersPackage TypeHandler 包路径
     * @param mapUnderscoreToCamelCase 是否启用驼峰映射
     * @param logImplClassName MyBatis 日志实现类名
     * @return MyBatis 会话工厂
     * @throws Exception 初始化异常
     */
    @Bean(name = "uengineSqlSessionFactory")
    public SqlSessionFactory uengineSqlSessionFactory(
        @Qualifier("uengineDataSource") DataSource uengineDataSource,
        @Value("${mybatis-plus.mapper-locations:classpath:/mapper/**/*.xml}") String mapperLocations,
        @Value("${mybatis-plus.type-handlers-package:}") String typeHandlersPackage,
        @Value("${mybatis-plus.configuration.map-underscore-to-camel-case:true}") boolean mapUnderscoreToCamelCase,
        @Value("${mybatis-plus.configuration.log-impl:org.apache.ibatis.logging.stdout.StdOutImpl}") String logImplClassName
    ) throws Exception {
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(uengineDataSource);
        Resource[] mapperResources = resolveMapperResources(mapperLocations);
        if (mapperResources.length > 0) {
            factoryBean.setMapperLocations(mapperResources);
        }
        if (StringUtils.hasText(typeHandlersPackage)) {
            factoryBean.setTypeHandlersPackage(typeHandlersPackage);
        }

        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(mapUnderscoreToCamelCase);
        if (StdOutImpl.class.getName().equals(logImplClassName)) {
            configuration.setLogImpl(StdOutImpl.class);
        }
        factoryBean.setConfiguration(configuration);
        return factoryBean.getObject();
    }

    /**
     * 解析 Mapper 资源路径。
     *
     * @param mapperLocations Mapper 配置路径
     * @return Mapper 资源数组
     * @throws Exception 资源解析异常
     */
    private Resource[] resolveMapperResources(String mapperLocations) throws Exception {
        String locationPattern = mapperLocations;
        if (locationPattern.startsWith("classpath:/")) {
            locationPattern = "classpath*:" + locationPattern.substring("classpath:/".length());
        }
        return new PathMatchingResourcePatternResolver().getResources(locationPattern);
    }

    /**
     * 创建 USP 模块专用的 SqlSessionTemplate。
     *
     * @param uengineSqlSessionFactory USP 会话工厂
     * @return SqlSessionTemplate 实例
     */
    @Bean(name = "uengineSqlSessionTemplate")
    public SqlSessionTemplate uengineSqlSessionTemplate(
        @Qualifier("uengineSqlSessionFactory") SqlSessionFactory uengineSqlSessionFactory
    ) {
        return new SqlSessionTemplate(uengineSqlSessionFactory);
    }

    /**
     * 创建 USP 数据源对应的 Flyway 迁移实例。
     *
     * @param uengineDataSource USP 数据源
     * @param flywayLocations 迁移脚本路径
     * @param baselineOnMigrate 是否启用 baseline-on-migrate
     * @param baselineVersion baseline 版本号
     * @return Flyway 实例
     */
    @Bean(name = "uengineFlyway", initMethod = "migrate")
    @ConditionalOnProperty(prefix = "flyway", name = "enabled", havingValue = "true", matchIfMissing = true)
    public Flyway uengineFlyway(
        @Qualifier("uengineDataSource") DataSource uengineDataSource,
        @Value("${flyway.locations:classpath:db/migration}") String flywayLocations,
        @Value("${spring.flyway.baseline-on-migrate:true}") boolean baselineOnMigrate,
        @Value("${spring.flyway.validate-on-migrate:true}") boolean validateOnMigrate,
        @Value("${spring.flyway.baseline-version:0}") String baselineVersion
    ) {
        return Flyway.configure()
            .dataSource(uengineDataSource)
            .locations(StringUtils.commaDelimitedListToStringArray(flywayLocations))
            .baselineOnMigrate(baselineOnMigrate)
            .validateOnMigrate(validateOnMigrate)
            .baselineVersion(MigrationVersion.fromVersion(baselineVersion))
            .load();
    }
}
