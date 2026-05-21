package com.ruijie.authzengine.autoconfigure;

import com.ruijie.authzengine.application.sdk.AuthzDataScopeService;
import com.ruijie.authzengine.application.sdk.AuthzGovernanceService;
import com.ruijie.authzengine.application.sdk.AuthzQueryService;
import com.ruijie.authzengine.application.sdk.AuthzRuntimeService;
import com.ruijie.authzengine.application.sdk.impl.DefaultAuthzDataScopeService;
import com.ruijie.authzengine.application.sdk.impl.DefaultAuthzGovernanceService;
import com.ruijie.authzengine.application.sdk.impl.DefaultAuthzQueryService;
import com.ruijie.authzengine.application.sdk.impl.DefaultAuthzRuntimeService;
import com.ruijie.authzengine.application.service.AssignmentAppService;
import com.ruijie.authzengine.application.service.AuthzContractAppService;
import com.ruijie.authzengine.application.service.AuthzDecisionAppService;
import com.ruijie.authzengine.infrastructure.config.AuditMetaObjectHandler;
import com.ruijie.authzengine.application.service.AuthzFacade;
import com.ruijie.authzengine.application.service.AuthzQueryAppService;
import com.ruijie.authzengine.application.service.MetaAppService;
import com.ruijie.authzengine.application.service.PermissionAppService;
import com.ruijie.authzengine.application.service.SubjectAppService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruijie.authzengine.domain.spi.AuthzSubjectProvider;
import com.ruijie.authzengine.domain.repository.PermissionRepository;
import com.ruijie.authzengine.domain.repository.ResourceRepository;
import com.ruijie.authzengine.infrastructure.authz.AuthzHttpPepFilter;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import com.ruijie.authzengine.shared.AuthzEngineCoreConfiguration;

/**
 * authz-engine Starter 自动装配入口。
 *
 * <p>负责专属数据源、Flyway、Mapper/事务工厂隔离、PEP 切面、专属 ObjectMapper 等核心 Bean 的条件注册。
 */
@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
@Import({AuthzEngineCoreConfiguration.class})
@AutoConfigureAfter({JacksonAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class})
@EnableConfigurationProperties({
    AuthzEngineProperties.class,
    AuthzEngineDataSourceProperties.class,
    AuthzEngineFlywayProperties.class,
    AuthzPepProperties.class
})
@ConditionalOnProperty(prefix = "authz.engine", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AuthzEngineAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(AuthzEngineAutoConfiguration.class);

    /**
     * authz-engine 专属 ObjectMapper，与宿主全局 ObjectMapper 隔离。
     *
     * <p>避免宿主自定义 Jackson 配置（如 SNAKE_CASE、FAIL_ON_UNKNOWN_PROPERTIES 等）
     * 影响引擎内部 JSON 处理的确定性。
     *
     * @return authz-engine 专属 ObjectMapper
     */
    @Bean(name = "authzObjectMapper")
    @ConditionalOnMissingBean(name = "authzObjectMapper")
    public ObjectMapper authzObjectMapper() {
        // 序列化：统一输出 yyyy-MM-dd HH:mm:ss
        DateTimeFormatter serializer = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        // 反序列化：同时兼容 ISO 8601 (2026-04-30T00:00:00) 和 yyyy-MM-dd HH:mm:ss 两种格式
        DateTimeFormatter deserializer = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd")
                .optionalStart().appendLiteral('T').optionalEnd()
                .optionalStart().appendLiteral(' ').optionalEnd()
                .appendPattern("HH:mm:ss")
                .toFormatter();
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(serializer));
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(deserializer));
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(javaTimeModule);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        // 禁止 BigDecimal 序列化为科学计数法（BigDecimalSerializer 检查 SerializationFeature；
        // DecimalNode.serialize 直接调用 gen.writeNumber，需同时设置 JsonGenerator.Feature）
        mapper.enable(SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN);
        mapper.configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true);
        return mapper;
    }

    /**
     * 当容器中同时存在宿主 sqlSessionFactory 和 authz 专属 authzSqlSessionFactory 时，
     * 自动将宿主的 sqlSessionFactory 标记为 Primary，使宿主 @MapperScan 无需显式配置 sqlSessionFactoryRef。
     */
    @Bean
    public static BeanFactoryPostProcessor authzSqlSessionFactoryPrimaryFixer() {
        return beanFactory -> {
            if (!(beanFactory instanceof DefaultListableBeanFactory)) {
                return;
            }
            DefaultListableBeanFactory dlbf = (DefaultListableBeanFactory) beanFactory;
            if (dlbf.containsBeanDefinition("sqlSessionFactory")
                    && dlbf.containsBeanDefinition("authzSqlSessionFactory")
                    && !dlbf.getBeanDefinition("sqlSessionFactory").isPrimary()) {
                dlbf.getBeanDefinition("sqlSessionFactory").setPrimary(true);
                log.info("[authz-engine] 检测到多 SqlSessionFactory 共存，已自动将宿主 sqlSessionFactory 标记为 Primary");
            }
        };
    }

    /**
     * 检测多个 {@code @Primary ObjectMapper} 共存（如 Spring Boot 的 {@code jacksonObjectMapper}
     * 与 springdoc 的 {@code serializingObjectMapper} 同时被标记为 Primary）时，
     * 保留 {@code jacksonObjectMapper} 为唯一 Primary，降级其余第三方 ObjectMapper，
     * 避免宿主应用按类型注入 {@link ObjectMapper} 时触发 {@code NoUniqueBeanDefinitionException}。
     *
     * <p>此修复与 {@link #authzSqlSessionFactoryPrimaryFixer()} 采用相同模式：
     * 通过 {@link BeanFactoryPostProcessor} 在 Bean 实例化前静默修正 BeanDefinition，
     * 宿主应用无需做任何改动。
     *
     * <p>{@code authzObjectMapper} 本身未标注 {@code @Primary}，不参与此修复逻辑。
     */
    @Bean
    public static BeanFactoryPostProcessor authzObjectMapperPrimaryFixer() {
        return beanFactory -> {
            if (!(beanFactory instanceof DefaultListableBeanFactory)) {
                return;
            }
            DefaultListableBeanFactory dlbf = (DefaultListableBeanFactory) beanFactory;

            // 收集所有标记为 @Primary 的 ObjectMapper BeanDefinition 名称
            String[] allNames = dlbf.getBeanNamesForType(ObjectMapper.class, true, false);
            List<String> primaryNames = new ArrayList<>();
            for (String name : allNames) {
                if (dlbf.containsBeanDefinition(name)
                        && dlbf.getBeanDefinition(name).isPrimary()) {
                    primaryNames.add(name);
                }
            }

            // 无冲突时不做任何修改
            if (primaryNames.size() <= 1) {
                return;
            }

            // 优先保留 jacksonObjectMapper（Spring Boot 默认），否则保留第一个找到的
            String keepPrimary = primaryNames.contains("jacksonObjectMapper")
                    ? "jacksonObjectMapper"
                    : primaryNames.get(0);
            for (String name : primaryNames) {
                if (!name.equals(keepPrimary)) {
                    BeanDefinition bd = dlbf.getBeanDefinition(name);
                    bd.setPrimary(false);
                    log.info("[authz-engine] 检测到多个 @Primary ObjectMapper，"
                            + "已降级 '{}' 的 Primary 标记，保留 '{}' 为唯一 Primary",
                            name, keepPrimary);
                }
            }
        };
    }

    /**
     * 创建 authz-engine 专属数据源。
     *
     * @param properties 数据源配置
     * @return authz-engine 数据源
     */
    @Bean(name = "authzDataSource")
    @ConditionalOnMissingBean(name = "authzDataSource")
    @ConditionalOnProperty(prefix = "rj.unify.engine.datasource", name = "url")
    public DataSource authzDataSource(AuthzEngineDataSourceProperties properties) {
        log.info("初始化 authz-engine 专属数据源 url={}", properties.getUrl());
        return properties.buildDataSource();
    }

    /**
     * 创建 authz-engine 专属 Flyway 实例。
     *
     * @param dataSource authz-engine 专属数据源
     * @param properties Flyway 配置
     * @return Flyway 实例
     */
    @Bean(name = "authzFlyway")
    @ConditionalOnMissingBean(name = "authzFlyway")
    @ConditionalOnBean(name = "authzDataSource")
    @ConditionalOnProperty(prefix = "authz.engine.flyway", name = "enabled", havingValue = "true", matchIfMissing = true)
    public Flyway authzFlyway(@Qualifier("authzDataSource") DataSource dataSource,
                              AuthzEngineFlywayProperties properties) {
        FluentConfiguration configuration = Flyway.configure()
            .dataSource(dataSource)
            .table(properties.getTable())
            .baselineOnMigrate(properties.isBaselineOnMigrate())
            .baselineVersion(properties.getBaselineVersion());
        List<String> locations = properties.getLocations();
        if (locations != null && !locations.isEmpty()) {
            configuration.locations(locations.toArray(new String[0]));
        }
        log.info("初始化 authz-engine 专属 Flyway locations={}, table={}", locations, properties.getTable());
        return configuration.load();
    }

    /**
     * 在 Spring 上下文启动阶段执行 authz-engine 专属 Flyway 迁移。
     * <p>
     * 迁移前先执行 repair()，自动修正已应用脚本的 checksum 差异，
     * 避免开发阶段修改已执行的迁移脚本后启动失败。
     *
     * @param flyway authz-engine Flyway
     * @return Flyway 初始化器
     */
    @Bean(name = "authzFlywayInitializer")
    @ConditionalOnMissingBean(name = "authzFlywayInitializer")
    @ConditionalOnBean(name = "authzFlyway")
    public FlywayMigrationInitializer authzFlywayInitializer(@Qualifier("authzFlyway") Flyway flyway) {
        return new FlywayMigrationInitializer(flyway, f -> {
            f.repair();
            f.migrate();
        });
    }

    /**
     * 创建 authz-engine 专属事务管理器。
     *
     * @param dataSource authz-engine 专属数据源
     * @return 专属事务管理器
     */
    @Bean(name = "authzTransactionManager")
    @ConditionalOnMissingBean(name = "authzTransactionManager")
    @ConditionalOnBean(name = "authzDataSource")
    public PlatformTransactionManager authzTransactionManager(@Qualifier("authzDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    /**
     * 创建 authz-engine 专属 NamedParameterJdbcTemplate，绑定专属数据源。
     *
     * @param dataSource authz-engine 专属数据源
     * @return 专属 NamedParameterJdbcTemplate
     */
    @Bean(name = "authzNamedParameterJdbcTemplate")
    @ConditionalOnMissingBean(name = "authzNamedParameterJdbcTemplate")
    @ConditionalOnBean(name = "authzDataSource")
    public NamedParameterJdbcTemplate authzNamedParameterJdbcTemplate(@Qualifier("authzDataSource") DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    /**
     * 创建 authz-engine 专属 SqlSessionFactory。
     * <p>
     * 不再将 AuditMetaObjectHandler 注册为独立 @Bean，避免宿主已有 MetaObjectHandler 时产生
     * NoUniqueBeanDefinitionException。此处通过 ObjectProvider 优先复用宿主的 Handler，
     * 若宿主未注册任何 MetaObjectHandler 则回退到 AuditMetaObjectHandler。
     *
     * @param dataSource authz-engine 专属数据源
     * @param metaObjectHandlerProvider 宿主上下文中可选的 MetaObjectHandler
     * @return 专属 SqlSessionFactory
     * @throws Exception 构建异常
     */
    @Bean(name = "authzSqlSessionFactory")
    @ConditionalOnMissingBean(name = "authzSqlSessionFactory")
    @ConditionalOnBean(name = "authzDataSource")
    public SqlSessionFactory authzSqlSessionFactory(
        @Qualifier("authzDataSource") DataSource dataSource,
        ObjectProvider<MetaObjectHandler> metaObjectHandlerProvider
    ) throws Exception {
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setTypeAliasesPackage("com.ruijie.authzengine.infrastructure.persistence.entity");
        factoryBean.setMapperLocations(
            new PathMatchingResourcePatternResolver().getResources("classpath*:mapper/**/*.xml")
        );

        GlobalConfig globalConfig = new GlobalConfig();
        // 优先使用宿主已定义的 MetaObjectHandler；若宿主无任何 Handler，则使用内置 AuditMetaObjectHandler
        MetaObjectHandler metaObjectHandler = metaObjectHandlerProvider.getIfAvailable(AuditMetaObjectHandler::new);
        globalConfig.setMetaObjectHandler(metaObjectHandler);
        factoryBean.setGlobalConfig(globalConfig);

        SqlSessionFactory sqlSessionFactory = factoryBean.getObject();
        log.info("初始化 authz-engine 专属 SqlSessionFactory 完成");
        return sqlSessionFactory;
    }

    /**
     * 注册 authz-engine Mapper 专属扫描器。
     *
     * @return Mapper 扫描配置器
     */
    @Bean
    @ConditionalOnMissingBean(name = "authzMapperScannerConfigurer")
    @ConditionalOnBean(name = "authzSqlSessionFactory")
    public static MapperScannerConfigurer authzMapperScannerConfigurer() {
        MapperScannerConfigurer configurer = new MapperScannerConfigurer();
        configurer.setBasePackage("com.ruijie.authzengine.infrastructure.persistence.mapper");
        configurer.setSqlSessionFactoryBeanName("authzSqlSessionFactory");
        configurer.setAnnotationClass(Mapper.class);
        return configurer;
    }

    /**
     * 注册面向业务系统注入的公共运行时鉴权服务。
     *
     * @param authzDecisionAppService 内部鉴权应用服务
     * @param authzFacade 内部快捷鉴权门面
     * @return 默认运行时鉴权服务
     */
    @Bean
    @ConditionalOnMissingBean(AuthzRuntimeService.class)
    public AuthzRuntimeService authzRuntimeService(AuthzDecisionAppService authzDecisionAppService,
                                                   AuthzFacade authzFacade) {
        log.info("注册对外 SDK 运行时服务 AuthzRuntimeService");
        return new DefaultAuthzRuntimeService(authzDecisionAppService, authzFacade);
    }

    /**
     * 注册面向业务系统注入的公共权限查询服务。
     *
     * @param authzQueryAppService 内部权限查询应用服务
     * @return 默认权限查询服务
     */
    @Bean
    @ConditionalOnMissingBean(AuthzQueryService.class)
    public AuthzQueryService authzQueryService(AuthzQueryAppService authzQueryAppService) {
        log.info("注册对外 SDK 查询服务 AuthzQueryService");
        return new DefaultAuthzQueryService(authzQueryAppService);
    }

    /**
     * 注册面向业务系统注入的治理型 SDK 服务。
     *
     * @param metaAppService 治理元模型应用服务
     * @param permissionAppService 权限项应用服务
     * @param subjectAppService 主体目录应用服务
     * @param assignmentAppService 授权分配应用服务
     * @param objectMapper Jackson 序列化器
     * @return 默认治理型 SDK 服务
     */
    @Bean
    @ConditionalOnMissingBean(AuthzGovernanceService.class)
    public AuthzGovernanceService authzGovernanceService(
        MetaAppService metaAppService,
        PermissionAppService permissionAppService,
        SubjectAppService subjectAppService,
        AssignmentAppService assignmentAppService,
        ObjectMapper objectMapper
    ) {
        log.info("注册对外 SDK 治理服务 AuthzGovernanceService");
        return new DefaultAuthzGovernanceService(
            metaAppService,
            permissionAppService,
            subjectAppService,
            assignmentAppService,
            objectMapper
        );
    }

    /**
     * 注册面向业务系统注入的公共数据范围服务。
     *
     * @param authzContractAppService 内部合同应用服务
     * @return 默认数据范围服务
     */
    @Bean
    @ConditionalOnMissingBean(AuthzDataScopeService.class)
    public AuthzDataScopeService authzDataScopeService(AuthzContractAppService authzContractAppService) {
        log.info("注册对外 SDK 数据范围服务 AuthzDataScopeService");
        return new DefaultAuthzDataScopeService(authzContractAppService);
    }

    /**
     * 注册默认鉴权主体提供者（始终返回 null，触发 UNAUTHENTICATED）。
     * 宿主应用可注入自己的实现替换此默认 Bean。
     *
     * @return 默认鉴权主体提供者
     */
    @Bean
    @ConditionalOnMissingBean
    public AuthzSubjectProvider defaultAuthzSubjectProvider() {
        log.info("注册默认 AuthzSubjectProvider（空实现），宿主应用需自行替换");
        return new DefaultAuthzSubjectProvider();
    }

    /**
     * 注册 HTTP 全量拦截 PEP Filter（{@link AuthzHttpPepFilter}）。
     *
     * <p>当 {@code authz.engine.pep.enabled=true}（默认）且
     * {@code authz.engine.pep.http-filter-enabled=true}（默认）时自动注册。
     *
     * <p>Filter 优先级设为 {@code -100}，确保在宿主应用业务过滤器之前执行鉴权，
     * 但在 Spring Security、CORS 等基础设施过滤器之后（可通过配置调整）。
     *
     * @param authzFacade          统一鉴权入口
     * @param subjectProviders     SPI 主体提供者候选集合
     * @param resourceRepository   资源仓储
     * @param permissionRepository 权限项仓储
     * @param engineProperties     引擎总配置
     * @param pepProperties        PEP 配置
     * @return Servlet Filter 注册 Bean
     */
    @Bean
    @ConditionalOnProperty(prefix = "authz.engine.pep", name = {"enabled", "http-filter-enabled"}, havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(AuthzHttpPepFilter.class)
        public FilterRegistrationBean<AuthzHttpPepFilter> authzHttpPepFilter(
            AuthzFacade authzFacade,
            ObjectProvider<AuthzSubjectProvider> subjectProviders,
            ResourceRepository resourceRepository,
            PermissionRepository permissionRepository,
            com.ruijie.authzengine.domain.repository.DerivationPermissionRepository derivationPermissionRepository,
            AuthzEngineProperties engineProperties,
            AuthzPepProperties pepProperties,
            Environment environment) {
        AuthzSubjectProvider subjectProvider = subjectProviders.orderedStream()
                .findFirst()
                .orElseGet(DefaultAuthzSubjectProvider::new);
        // 优先从 Environment 中读取属性，兼容不同命名风格（tenantId / tenant-id）
        String finalTenantId = environment.getProperty("authz.engine.tenantId",
            environment.getProperty("authz.engine.tenant-id", engineProperties.getTenantId()));
        String finalAppCode = environment.getProperty("authz.engine.appCode",
            environment.getProperty("authz.engine.app-code", engineProperties.getAppCode()));

        AuthzHttpPepFilter filter = new AuthzHttpPepFilter(
            authzFacade,
            subjectProvider,
            resourceRepository,
            permissionRepository,
            derivationPermissionRepository,
            finalTenantId,
            finalAppCode,
            pepProperties.getIncludePatterns(),
            pepProperties.getExcludePatterns(),
            pepProperties.getEngineWhitelist(),
            pepProperties.getUndeclaredResourceStrategy());
        FilterRegistrationBean<AuthzHttpPepFilter> registration = new FilterRegistrationBean<>(filter);
        registration.addUrlPatterns("/*");
        registration.setOrder(pepProperties.getFilterOrder());
        registration.setName("authzHttpPepFilter");
        log.info("成功注册 HTTP PEP Filter，tenantId={}, appCode={}, undeclaredStrategy={}, subjectProvider={}",
            finalTenantId,
            finalAppCode,
            pepProperties.getUndeclaredResourceStrategy(),
            subjectProvider.getClass().getSimpleName());
        return registration;
    }

    /**
     * 注册 FIELD 策略字段控制响应拦截器（{@link AuthzFieldControlAdvice}）。
     *
     * <p>在响应序列化前自动执行 fieldControls 指令，支持 {@link com.ruijie.authzengine.shared.web.ApiResponse}、
     * {@link com.ruijie.authzengine.domain.model.governance.PageResult} 和普通 Collection/POJO 三种包装形式。
     * 仅在当前请求线程的 {@link com.ruijie.authzengine.infrastructure.authz.AuthzDecisionHolder} 包含
     * 非空 {@code fieldControls} 时介入，无字段控制时直接透传。
     *
     * @param fieldControlExecutor 字段控制执行器
     * @param objectMapper         authz-engine 专属 ObjectMapper
     * @return 字段控制响应拦截器
     */
    @Bean
    @ConditionalOnProperty(prefix = "authz.engine.pep", name = {"enabled"}, havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(AuthzFieldControlAdvice.class)
    public AuthzFieldControlAdvice authzFieldControlAdvice(
            com.ruijie.authzengine.infrastructure.authz.FieldControlExecutor fieldControlExecutor,
            @Qualifier("authzObjectMapper") ObjectMapper objectMapper) {
        log.info("注册 FIELD 策略字段控制响应拦截器 AuthzFieldControlAdvice");
        return new AuthzFieldControlAdvice(fieldControlExecutor, objectMapper);
    }

    /**
     * 注册 DATA 策略行过滤 MyBatis 拦截器。
     *
     * <p>Spring Boot MyBatis 自动配置会将容器中所有 {@link org.apache.ibatis.plugin.Interceptor} Bean
     * 自动添加到宿主 {@code sqlSessionFactory}，从而实现对宿主 Mapper SQL 的自动行级过滤注入。
     * authz-engine 专属的 {@code authzSqlSessionFactory} 为手动构建，不受此机制影响。
     *
     * @return DATA 策略行过滤拦截器
     */
    @Bean
    @ConditionalOnProperty(prefix = "authz.engine.pep", name = {"enabled", "http-filter-enabled"}, havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(AuthzRowFilterInterceptor.class)
    public AuthzRowFilterInterceptor authzRowFilterInterceptor() {
        log.info("注册 DATA 策略行过滤拦截器 AuthzRowFilterInterceptor");
        return new AuthzRowFilterInterceptor();
    }
}