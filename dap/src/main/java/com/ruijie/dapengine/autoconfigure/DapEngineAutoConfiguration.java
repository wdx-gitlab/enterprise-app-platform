package com.ruijie.dapengine.autoconfigure;

import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.alibaba.druid.pool.DruidDataSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.ruijie.dapengine.admin.controller.GlobalExceptionHandler;
import com.ruijie.dapengine.admin.controller.HrNotifyController;
import com.ruijie.dapengine.admin.controller.MasterDataBrowseController;
import com.ruijie.dapengine.admin.controller.MetadataConfigController;
import com.ruijie.dapengine.admin.controller.SyncConfigController;
import com.ruijie.dapengine.admin.service.OrgSyncService;
import com.ruijie.dapengine.admin.service.QueueMessageHandleService;
import com.ruijie.dapengine.sdk.DapEngineService;
import com.ruijie.dapengine.sdk.DapEngineServiceImpl;
import com.ruijie.dapengine.sdk.MasterDataCacheService;
import com.ruijie.dapengine.sdk.MasterDataMetaService;
import com.ruijie.dapengine.sdk.MasterDataMetaServiceImpl;
import com.ruijie.dapengine.sdk.MasterDataQueryService;
import com.ruijie.dapengine.sdk.MasterDataQueryServiceImpl;
import com.ruijie.dapengine.sdk.MasterDataService;
import com.ruijie.dapengine.sdk.MasterDataSyncService;
import com.ruijie.dapengine.sdk.MasterDataSyncServiceImpl;
import com.ruijie.dapengine.admin.service.MetadataConfigService;
import com.ruijie.dapengine.admin.service.SchemaStatusService;
import com.ruijie.dapengine.admin.service.SyncConfigService;
import com.ruijie.dapengine.common.exception.DapValidationException;
import com.ruijie.dapengine.common.model.DapEngineProperties;
import com.ruijie.dapengine.common.model.RuijieAuthProperties;
import com.ruijie.dapengine.common.util.AesCipher;
import com.ruijie.dapengine.common.handler.LocalDateTimeTypeHandler;
import com.ruijie.dapengine.mapper.CheckpointMapper;
import com.ruijie.dapengine.mapper.DapSysOrgMapper;
import com.ruijie.dapengine.mapper.DapSysUserMapper;
import com.ruijie.dapengine.mapper.MetadataConfigMapper;
import com.ruijie.dapengine.mapper.SubjectMapper;
import com.ruijie.dapengine.mapper.SyncConfigMapper;
import com.ruijie.dapengine.mapper.SyncLogMapper;
import com.ruijie.dapengine.migration.DapEngineSchemaInitializer;
import com.ruijie.dapengine.provider.DataProvider;
import com.ruijie.dapengine.provider.DbDataProvider;
import com.ruijie.dapengine.provider.HttpDataProvider;
import com.ruijie.dapengine.provider.MqDataProvider;
import com.ruijie.dapengine.repository.CheckpointRepository;
import com.ruijie.dapengine.repository.DapSysOrgRepository;
import com.ruijie.dapengine.repository.DapSysUserRepository;
import com.ruijie.dapengine.repository.MetadataRepository;
import com.ruijie.dapengine.repository.SubjectRepository;
import com.ruijie.dapengine.repository.SyncConfigRepository;
import com.ruijie.dapengine.repository.SyncLogRepository;
import com.ruijie.dapengine.sync.FieldMappingService;
import com.ruijie.dapengine.sync.OSDSService;
import com.ruijie.dapengine.sync.SyncExecutor;
import com.ruijie.dapengine.sync.SyncInterceptor;
import com.ruijie.dapengine.sync.SubjectSyncLockManager;
import com.ruijie.dapengine.sync.SyncScheduler;
import com.ruijie.dapengine.sync.SyncSchedulerImpl;
import com.ruijie.dapengine.writer.LocalDataWriter;
import org.apache.ibatis.session.SqlSessionFactory;
import org.flywaydb.core.Flyway;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * DAP Engine Starter 自动装配入口。
 * 条件：dap.engine.enabled=true（默认）。
 * 职责：校验必填配置、注册平台库数据源、执行 Flyway 迁移、注册缓存管理器。
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties({DapEngineProperties.class, DapEngineProperties.DataSourceProperties.class,
        RuijieAuthProperties.class})
@Import(DapEngineAutoConfiguration.AdminWebConfiguration.class)
@ConditionalOnProperty(prefix = "dap.engine", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DapEngineAutoConfiguration implements InitializingBean {

    private final DapEngineProperties props;
    private final DapEngineProperties.DataSourceProperties dataSourceProperties;
    private final ApplicationContext applicationContext;

    public DapEngineAutoConfiguration(DapEngineProperties props,
                                      DapEngineProperties.DataSourceProperties dataSourceProperties,
                                      ApplicationContext applicationContext) {
        this.props = props;
        this.dataSourceProperties = dataSourceProperties;
        this.applicationContext = applicationContext;
    }

    // -------------------------------------------------------------------------
    // 配置校验（启动阶段快速失败）
    // -------------------------------------------------------------------------

    @Override
    public void afterPropertiesSet() {
        validateConfig();
    }

    private void validateConfig() {
        if (isEmpty(props.getTenantId())) {
            throw new DapValidationException(
                "[DAP Engine] Configuration validation failed: 'dap.engine.tenant-id' must not be empty.");
        }
        if (isEmpty(props.getAppCode())) {
            throw new DapValidationException(
                "[DAP Engine] Configuration validation failed: 'dap.engine.app-code' must not be empty.");
        }
        DapEngineProperties.DataSourceProperties ds = dataSourceProperties;
        if (isEmpty(ds.getUrl())) {
            throw new DapValidationException(
                "[DAP Engine] Configuration validation failed: 'rj.unify.engine.datasource.url' must not be empty.");
        }
        if (isEmpty(ds.getUsername())) {
            throw new DapValidationException(
                "[DAP Engine] Configuration validation failed: 'rj.unify.engine.datasource.username' must not be empty.");
        }
        // 密码允许为空（无密码数据库实例为合法配置）
        String encryptKey = props.getSecurity().getEncryptKey();
        if (isEmpty(encryptKey)) {
            throw new DapValidationException(
                "[DAP Engine] Configuration validation failed: 'dap.engine.security.encrypt-key' must not be empty.");
        }
        AesCipher.validateKey(encryptKey);

        if (props.getSync().getTargetStorage() == DapEngineProperties.StorageTarget.BUSINESS_DB) {
            if (!applicationContext.containsBean("dataSource")) {
                throw new DapValidationException(
                    "[DAP Engine] Configuration validation failed: 'dap.engine.sync.target-storage=BUSINESS_DB'" +
                    " requires a bean named 'dataSource' (business datasource) to be present.");
            }
        }
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    // -------------------------------------------------------------------------
    // 平台库数据源
    // -------------------------------------------------------------------------

    /**
     * DAP Engine 专属 Druid 数据源，与业务系统{@code dataSource} Bean 完全隔离。
     * 连接池名称 {@code dap-engine-platform} 便于监控区分。
     */
    @Bean(name = "dapEngineDataSource", destroyMethod = "close")
    @ConditionalOnMissingBean(DapEngineDataSource.class)
    public DapEngineDataSource dapEngineDataSource() {
        DapEngineProperties.DataSourceProperties dsProp = dataSourceProperties;
        DruidDataSource druid = new DruidDataSource();
        druid.setName("dap-engine-platform");
        druid.setDriverClassName(dsProp.getDriverClassName());
        druid.setUrl(dsProp.getUrl());
        druid.setUsername(dsProp.getUsername());
        druid.setPassword(dsProp.getPassword());
        druid.setInitialSize(dsProp.getInitialSize());
        druid.setMinIdle(dsProp.getMinIdle());
        druid.setMaxActive(dsProp.getMaxActive());
        druid.setMaxWait(dsProp.getMaxWait());
        druid.setValidationQuery(dsProp.getValidationQuery());
        druid.setTestOnBorrow(dsProp.isTestOnBorrow());
        druid.setTestOnReturn(dsProp.isTestOnReturn());
        druid.setTestWhileIdle(dsProp.isTestWhileIdle());
        druid.setKeepAlive(dsProp.isKeepAlive());
        druid.setTimeBetweenEvictionRunsMillis(dsProp.getTimeBetweenEvictionRunsMillis());
        druid.setMinEvictableIdleTimeMillis(dsProp.getMinEvictableIdleTimeMillis());
        druid.setPoolPreparedStatements(dsProp.isPoolPreparedStatements());
        druid.setMaxPoolPreparedStatementPerConnectionSize(dsProp.getMaxPoolPreparedStatementPerConnectionSize());
        return new DapEngineDataSource(druid);
    }

    /**
     * DAP Engine 专属 JdbcTemplate，绑定 {@code dapEngineDataSource}。
     * 用于执行 DDL、{@code information_schema} 元数据查询及原生 UPSERT SQL。
     */
    @Bean(name = "dapEngineJdbcTemplate")
    @ConditionalOnMissingBean(DapEngineJdbcTemplate.class)
    public DapEngineJdbcTemplate dapEngineJdbcTemplate(DapEngineDataSource dapEngineDataSource) {
        return new DapEngineJdbcTemplate(dapEngineDataSource.getDataSource());
    }

    /**
     * DAP Engine 专属事务管理器，绑定 {@code dapEngineDataSource}。
     * {@code MetadataConfigService#saveSubjectConfig()} 通过
     * {@code @Transactional("dapTransactionManager")} 引用的 Bean，确保
     * 元数据写操作。DAP 专属数据源上事务性执行，不干扰业务系统事务。
     */
    @Bean(name = "dapTransactionManager")
    @ConditionalOnMissingBean(name = "dapTransactionManager")
    public PlatformTransactionManager dapTransactionManager(DapEngineDataSource dapEngineDataSource) {
        return new DataSourceTransactionManager(dapEngineDataSource.getDataSource());
    }

    // -------------------------------------------------------------------------
    // Flyway 系统表迁移（独立实例，不干扰业务系统 Flyway。
    // -------------------------------------------------------------------------

    /**
     * DAP Engine 专属 Flyway 实例，历史表使用 {@code dap_schema_history}。
     * 与业务系统{@code flyway_schema_history} 互不干扰。
     * 迁移脚本路径 {@code classpath:db/dap_migration/}，仅管理 DAP 系统。DDL。
     *
     * <p>自愈机制：若当前 catalog 。dap_sys_subject 不存在但 dap_schema_history 中已有
     * baseline 记录，则清除 history 表后重新迁移。catalog 限定避免跨库误判。
     */
    @Bean(name = "dapFlyway")
    @ConditionalOnMissingBean(name = "dapFlyway")
    @ConditionalOnProperty(prefix = "dap.engine", name = "migration.enabled", havingValue = "true", matchIfMissing = true)
    public Flyway dapFlyway(DapEngineDataSource dapEngineDataSource) {
        javax.sql.DataSource ds = dapEngineDataSource.getDataSource();
        // 自愈：检查当。catalog 。dap_sys_subject 是否存在；不存在则清理 history 。Flyway 重新建表
        try (java.sql.Connection conn = ds.getConnection()) {
            String catalog = conn.getCatalog();  // 限定为当前库，避。getTables(null,...) 跨库误判
            boolean subjectTableExists;
            try (java.sql.ResultSet rs = conn.getMetaData().getTables(
                    catalog, null, "dap_sys_subject", new String[]{"TABLE"})) {
                subjectTableExists = rs.next();
            }
            if (!subjectTableExists) {
                try (java.sql.Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("DROP TABLE IF EXISTS dap_schema_history");
                }
            }
        } catch (Exception ignored) {
            // 连接失败时跳过自愈，。Flyway 自行处理
        }
        Flyway flyway = Flyway.configure()
            .dataSource(ds)
            .locations("classpath:db/dap_migration")
            .table("dap_schema_history")
            .baselineOnMigrate(true)
            .baselineVersion("0")
            .validateOnMigrate(false)  // 禁用 validate，避。V1=FAILED 记录阻断启动
            .load();
        flyway.repair();   // 清除 dap_schema_history 中的 FAILED 记录，允许重。
        flyway.migrate();
        return flyway;
    }

    // -------------------------------------------------------------------------
    // 加解密工。Bean
    // -------------------------------------------------------------------------

    /**
     * AES-256 加解密工具，密钥来自配置使用 {@code dap.engine.security.encrypt-key}。
     * 用于同步配置中敏感数据源信息的加密存储与运行时解密。
     */
    @Bean
    @ConditionalOnMissingBean(AesCipher.class)
    public AesCipher aesCipher() {
        return new AesCipher(props.getSecurity().getEncryptKey());
    }

    // -------------------------------------------------------------------------
    // 缓存管理器（优先 Redis，降。Caffeine。
    // -------------------------------------------------------------------------

    /**
     * Redis 缓存管理器（高优先级）：使用 {@code RedisConnectionFactory} Bean 存在时激活，
     * 用于生产环境分布式缓存。Key 过期策略。Redis 端配置。
     */
    @Bean(name = "dapCacheManager")
    @ConditionalOnBean(RedisConnectionFactory.class)
    @ConditionalOnMissingBean(name = "dapCacheManager")
    public CacheManager dapRedisCacheManager(RedisConnectionFactory factory) {
        return RedisCacheManager.create(factory);
    }

    /**
     * Caffeine 本地缓存管理器（兜底）：Redis 不可用或未配置时激活。
     * TTL 和最大容量通过 {@code dap.engine.cache.*} 配置项控制。
     */
    @Bean(name = "dapCacheManager")
    @ConditionalOnMissingBean(name = "dapCacheManager")
    public CacheManager dapCaffeineCacheManager() {
        DapEngineProperties.CacheProperties cacheProp = props.getCache();
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine(
            Caffeine.newBuilder()
                .expireAfterWrite(cacheProp.getTtlMinutes(), TimeUnit.MINUTES)
                .maximumSize(cacheProp.getMaximumSize())
        );
        return manager;
    }

    // -------------------------------------------------------------------------
    // MyBatis-Plus SqlSessionFactory（绑定 DAP 专属数据源）
    // -------------------------------------------------------------------------

    /**
     * DAP Engine 专属 MyBatis-Plus SqlSessionFactory。
     * 绑定 {@code dapEngineDataSource}，通过 {@code MapperFactoryBean} 手动注册
     * Mapper Bean，不使用 {@code @MapperScan}，避免扫描影响宿主应。Mapper 注册。
     * 关闭 MyBatis-Plus 启动 Banner 以保持日志清洁。
     */
    @Bean(name = "dapSqlSessionFactory")
    @ConditionalOnMissingBean(name = "dapSqlSessionFactory")
    public SqlSessionFactory dapSqlSessionFactory(DapEngineDataSource dapEngineDataSource) throws Exception {
        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dapEngineDataSource.getDataSource());
        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.setBanner(false);
        factory.setGlobalConfig(globalConfig);
        // 兼容 MySQL Connector/J 5.x：用 getTimestamp() 代替 JDBC 4.2 的 getObject(col, Class)
        factory.setTypeHandlers(new org.apache.ibatis.type.TypeHandler[]{new LocalDateTimeTypeHandler()});
        return factory.getObject();
    }

    /**
     * dap_sys_subject 。Mapper Bean，绑定 {@code dapSqlSessionFactory}。
     * 使用 {@link SubjectRepository} 注入使用，执。Subject CRUD 操作。
     */
    @Bean
    @ConditionalOnMissingBean(SubjectMapper.class)
    public SubjectMapper subjectMapper(
            @Qualifier("dapSqlSessionFactory") SqlSessionFactory dapSqlSessionFactory) throws Exception {
        MapperFactoryBean<SubjectMapper> factory = new MapperFactoryBean<>(SubjectMapper.class);
        factory.setSqlSessionFactory(dapSqlSessionFactory);
        factory.afterPropertiesSet();
        return factory.getObject();
    }

    /**
     * dap_sys_metadata_config 。Mapper Bean，绑定 {@code dapSqlSessionFactory}。
     * 使用 {@link MetadataRepository} 注入使用，执行字段元数据 CRUD 操作。
     */
    @Bean
    @ConditionalOnMissingBean(MetadataConfigMapper.class)
    public MetadataConfigMapper metadataConfigMapper(
            @Qualifier("dapSqlSessionFactory") SqlSessionFactory dapSqlSessionFactory) throws Exception {
        MapperFactoryBean<MetadataConfigMapper> factory = new MapperFactoryBean<>(MetadataConfigMapper.class);
        factory.setSqlSessionFactory(dapSqlSessionFactory);
        factory.afterPropertiesSet();
        return factory.getObject();
    }

    /**
     * dap_sys_sync_config 。Mapper Bean，绑定 {@code dapSqlSessionFactory}。
     * 使用 {@link com.ruijie.dapengine.repository.SyncConfigRepository} 注入使用。
     */
    @Bean
    @ConditionalOnMissingBean(SyncConfigMapper.class)
    public SyncConfigMapper syncConfigMapper(
            @Qualifier("dapSqlSessionFactory") SqlSessionFactory dapSqlSessionFactory) throws Exception {
        MapperFactoryBean<SyncConfigMapper> factory = new MapperFactoryBean<>(SyncConfigMapper.class);
        factory.setSqlSessionFactory(dapSqlSessionFactory);
        factory.afterPropertiesSet();
        return factory.getObject();
    }

    /**
     * dap_sys_checkpoint 。Mapper Bean，绑定 {@code dapSqlSessionFactory}。
     * 使用 {@link com.ruijie.dapengine.repository.CheckpointRepository} 注入使用。
     */
    @Bean
    @ConditionalOnMissingBean(CheckpointMapper.class)
    public CheckpointMapper checkpointMapper(
            @Qualifier("dapSqlSessionFactory") SqlSessionFactory dapSqlSessionFactory) throws Exception {
        MapperFactoryBean<CheckpointMapper> factory = new MapperFactoryBean<>(CheckpointMapper.class);
        factory.setSqlSessionFactory(dapSqlSessionFactory);
        factory.afterPropertiesSet();
        return factory.getObject();
    }

    /**
     * dap_sys_user Mapper Bean，绑定 {@code dapSqlSessionFactory}。
     */
    @Bean
    @ConditionalOnMissingBean(DapSysUserMapper.class)
    public DapSysUserMapper dapSysUserMapper(
            @Qualifier("dapSqlSessionFactory") SqlSessionFactory dapSqlSessionFactory) throws Exception {
        MapperFactoryBean<DapSysUserMapper> factory = new MapperFactoryBean<>(DapSysUserMapper.class);
        factory.setSqlSessionFactory(dapSqlSessionFactory);
        factory.afterPropertiesSet();
        return factory.getObject();
    }

    /**
     * dap_sys_org Mapper Bean，绑定 {@code dapSqlSessionFactory}。
     */
    @Bean
    @ConditionalOnMissingBean(DapSysOrgMapper.class)
    public DapSysOrgMapper dapSysOrgMapper(
            @Qualifier("dapSqlSessionFactory") SqlSessionFactory dapSqlSessionFactory) throws Exception {
        MapperFactoryBean<DapSysOrgMapper> factory = new MapperFactoryBean<>(DapSysOrgMapper.class);
        factory.setSqlSessionFactory(dapSqlSessionFactory);
        factory.afterPropertiesSet();
        return factory.getObject();
    }

    /**
     * dap_sys_sync_log 。Mapper Bean，绑定 {@code dapSqlSessionFactory}。
     * 使用 {@link com.ruijie.dapengine.repository.SyncLogRepository} 注入使用。
     */
    @Bean
    @ConditionalOnMissingBean(SyncLogMapper.class)
    public SyncLogMapper syncLogMapper(
            @Qualifier("dapSqlSessionFactory") SqlSessionFactory dapSqlSessionFactory) throws Exception {
        MapperFactoryBean<SyncLogMapper> factory = new MapperFactoryBean<>(SyncLogMapper.class);
        factory.setSqlSessionFactory(dapSqlSessionFactory);
        factory.afterPropertiesSet();
        return factory.getObject();
    }

    // -------------------------------------------------------------------------
    // Admin API Beans（Repository / Service / Controller。
    // -------------------------------------------------------------------------

    /**
     * Subject 数据访问层，使用 MyBatis-Plus {@link SubjectMapper} 操作 dap_sys_subject 表。
     * 注入 tenantId / appCode 作为租户隔离条件。
     */
    @Bean
    @ConditionalOnMissingBean(SubjectRepository.class)
    public SubjectRepository subjectRepository(SubjectMapper subjectMapper) {
        return new SubjectRepository(subjectMapper, props.getTenantId(), props.getAppCode());
    }

    /**
     * 字段元数据数据访问层，使。MyBatis-Plus {@link MetadataConfigMapper}
     * 操作 dap_sys_metadata_config 表。
     */
    @Bean
    @ConditionalOnMissingBean(MetadataRepository.class)
    public MetadataRepository metadataRepository(MetadataConfigMapper metadataConfigMapper) {
        return new MetadataRepository(metadataConfigMapper, props.getTenantId(), props.getAppCode());
    }

    /**
     * Schema 状态计算服务，通过查询 {@code information_schema.COLUMNS} 判断动态表
     * 是否与元数据定义同步。此 Bean 继续使用 {@code DapEngineJdbcTemplate} 执行原生 SQL。
     * 不适合使用 MyBatis-Plus 实体映射。
     */
    @Bean
    @ConditionalOnMissingBean(SchemaStatusService.class)
    public SchemaStatusService schemaStatusService(DapEngineJdbcTemplate dapEngineJdbcTemplate) {
        return new SchemaStatusService(dapEngineJdbcTemplate.getJdbcTemplate());
    }

    /**
     * 元数据管理核心业务服务，聚合 Subject 生命周期与字段元数据的完。10 。saveSubjectConfig 流程。
     * 事务使用 {@code dapTransactionManager} 管理，确保跨表写操作的原子性。
     */
    @Bean
    @ConditionalOnMissingBean(MetadataConfigService.class)
    public MetadataConfigService metadataConfigService(SubjectRepository subjectRepository,
                                                       MetadataRepository metadataRepository,
                                                       SchemaStatusService schemaStatusService) {
        return new MetadataConfigService(subjectRepository, metadataRepository, schemaStatusService);
    }

    /**
     * 动。Schema 引擎：根据元数据配置在平台库中建表、补列、类型展宽。
     * 实现 {@link org.springframework.boot.ApplicationRunner}，Starter 启动时自动执行一次兜。apply。
     * 三个必须依赖通过构造器注入；{@link com.ruijie.dapengine.sync.SyncScheduler} 为可选字段注入（Phase 4）。
     */
    @Bean
    @ConditionalOnMissingBean(DapEngineSchemaInitializer.class)
    public DapEngineSchemaInitializer dapEngineSchemaInitializer(
            DapEngineJdbcTemplate dapEngineJdbcTemplate,
            MetadataConfigService metadataConfigService,
            SchemaStatusService schemaStatusService) {
        return new DapEngineSchemaInitializer(dapEngineJdbcTemplate, metadataConfigService, schemaStatusService);
    }

    /**
     * 同步配置数据访问层，使用 MyBatis-Plus {@link SyncConfigMapper} 操作 dap_sys_sync_config 表。
     * subject_code 唯一键支。INSERT-or-UPDATE 语义。
     */
    @Bean
    @ConditionalOnMissingBean(SyncConfigRepository.class)
    public SyncConfigRepository syncConfigRepository(SyncConfigMapper syncConfigMapper) {
        return new SyncConfigRepository(syncConfigMapper, props.getTenantId(), props.getAppCode());
    }

    /**
     * 同步位点数据访问层（dap_sys_checkpoint）。
     * 查询使用 MyBatis-Plus {@link CheckpointMapper}，写入使使用 {@link DapEngineJdbcTemplate} 执行原生 UPSERT SQL。
     */
    @Bean
    @ConditionalOnMissingBean(CheckpointRepository.class)
    public CheckpointRepository checkpointRepository(CheckpointMapper checkpointMapper,
                                                      DapEngineJdbcTemplate dapEngineJdbcTemplate) {
        return new CheckpointRepository(checkpointMapper, dapEngineJdbcTemplate,
                props.getTenantId(), props.getAppCode());
    }

    /**
     * 同步日志数据访问层（dap_sys_sync_log），通过 {@link SyncLogMapper}（MyBatis-Plus）操作。
     */
    @Bean
    @ConditionalOnMissingBean(SyncLogRepository.class)
    public SyncLogRepository syncLogRepository(SyncLogMapper syncLogMapper) {
        return new SyncLogRepository(syncLogMapper, props.getTenantId(), props.getAppCode());
    }

    /**
     * 员工主档数据访问层（dap_sys_user）。
     */
    @Bean
    @ConditionalOnMissingBean(DapSysUserRepository.class)
    public DapSysUserRepository dapSysUserRepository(DapSysUserMapper dapSysUserMapper) {
        return new DapSysUserRepository(dapSysUserMapper, props.getTenantId(), props.getAppCode());
    }

    /**
     * 组织架构数据访问层（dap_sys_org）。
     */
    @Bean
    @ConditionalOnMissingBean(DapSysOrgRepository.class)
    public DapSysOrgRepository dapSysOrgRepository(DapSysOrgMapper dapSysOrgMapper) {
        return new DapSysOrgRepository(dapSysOrgMapper, props.getTenantId(), props.getAppCode());
    }

    /**
     * 字段映射服务：将 DataProvider 返回的源字段名映射为目标字段名称 
     */
    @Bean
    @ConditionalOnMissingBean(FieldMappingService.class)
    public FieldMappingService fieldMappingService() {
        return new FieldMappingService();
    }

    /**
     * 本地动态主数据表写入器（UPSERT 。FULL_REFRESH）。
     */
    @Bean
    @ConditionalOnMissingBean(LocalDataWriter.class)
    public LocalDataWriter localDataWriter(DapEngineJdbcTemplate dapEngineJdbcTemplate,
                                            ObjectMapper objectMapper) {
        return new LocalDataWriter(dapEngineJdbcTemplate, props.getTenantId(), props.getAppCode(),
                objectMapper);
    }

    /**
     * DAP Engine 内部使用的 ObjectMapper。
     * 非 Web 环境下 Spring Boot 不会自动注册 ObjectMapper（依赖 Jackson2ObjectMapperBuilder），
     * 此处兜底注册一个标准实例；Web 环境下 @ConditionalOnMissingBean 保证不覆盖宿主配置。
     */
    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper dapObjectMapper() {
        return new ObjectMapper();
    }

    /**
     * OSDS 员工信息 HTTP 客户端。
     */
    @Bean
    @ConditionalOnMissingBean(OSDSService.class)
    public OSDSService osdsService(ObjectMapper objectMapper,
                                   DapEngineProperties dapEngineProperties,
                                   RuijieAuthProperties ruijieAuthProperties) {
        return new OSDSService(objectMapper, dapEngineProperties, ruijieAuthProperties);
    }

    /**
     * 组织架构递归补齐服务。
     */
    @Bean
    @ConditionalOnMissingBean(OrgSyncService.class)
    public OrgSyncService orgSyncService(OSDSService osdsService,
                                         DapSysOrgRepository dapSysOrgRepository) {
        return new OrgSyncService(osdsService, dapSysOrgRepository);
    }

    /**
     * 主数据缓存服务：Redis Hash（有 Redis 时）/ Caffeine（常驻兜底）双模式缓存封装。
     * RedisTemplate 可选注入（无 Redis 时为 null）。
     */
    @Bean
    @ConditionalOnMissingBean(MasterDataCacheService.class)
    public MasterDataCacheService masterDataCacheService(
            @Autowired(required = false) RedisTemplate<String, String> redisTemplate,
            @Qualifier("dapCacheManager") CacheManager dapCacheManager,
            ObjectMapper objectMapper) {
        return new MasterDataCacheService(redisTemplate, dapCacheManager, objectMapper);
    }

    /**
     * 主数据查询核心服务：聚合缓存、元数据白名单校验与动。SQL 查询。
     */
    @Bean
    @ConditionalOnMissingBean(MasterDataService.class)
    public MasterDataService masterDataService(
            DapEngineJdbcTemplate dapEngineJdbcTemplate,
            MetadataConfigService metadataConfigService,
            MasterDataCacheService masterDataCacheService) {
        return new MasterDataService(dapEngineJdbcTemplate, metadataConfigService, masterDataCacheService, props);
    }

    /**
     * 主数据查询门面接口默认实现，委托 {@link MasterDataService}。
     */
    @Bean
    @ConditionalOnMissingBean(MasterDataQueryService.class)
    public MasterDataQueryService masterDataQueryService(MasterDataService masterDataService) {
        return new MasterDataQueryServiceImpl(masterDataService);
    }

    /**
     * 主数据元数据门面接口默认实现。
     */
    @Bean
    @ConditionalOnMissingBean(MasterDataMetaService.class)
    public MasterDataMetaService masterDataMetaService(MetadataConfigService metadataConfigService) {
        return new MasterDataMetaServiceImpl(metadataConfigService);
    }

    /**
     * DAP 同步专属线程池调度器，poolSize=4，线程名前缀 "dap-sync-"。
     */
    @Bean(name = "dapSyncTaskScheduler", destroyMethod = "shutdown")
    @ConditionalOnMissingBean(name = "dapSyncTaskScheduler")
    public ThreadPoolTaskScheduler dapSyncTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("dap-sync-");
        scheduler.setWaitForTasksToCompleteOnShutdown(false);
        scheduler.initialize();
        return scheduler;
    }

    /**
     * 动态定时同步调度器实现。启动时恢复所。SCHEDULE+APPLIED 主题。Cron 任务。
     */
    @Bean
    @ConditionalOnMissingBean(SyncScheduler.class)
    public SyncScheduler syncScheduler(SubjectRepository subjectRepository,
                                        SyncConfigRepository syncConfigRepository,
                                        MetadataConfigService metadataConfigService,
                                        SchemaStatusService schemaStatusService,
                                        @Autowired(required = false) SyncExecutor syncExecutor,
                                        @Qualifier("dapSyncTaskScheduler") TaskScheduler taskScheduler) {
        return new SyncSchedulerImpl(subjectRepository, syncConfigRepository, metadataConfigService,
                schemaStatusService, syncExecutor, taskScheduler);
    }

    /**
     * 主题级同步串行锁管理器（US6）。
     * 使用 {@link SyncExecutor} 注入，确保同一 Subject 的并发同步任务串行执行。
     */
    @Bean
    @ConditionalOnMissingBean(SubjectSyncLockManager.class)
    public SubjectSyncLockManager subjectSyncLockManager() {
        return new SubjectSyncLockManager();
    }

    /**     * 主数据同步门面接口默认实现，委托 {@link com.ruijie.dapengine.sync.SyncExecutor}。
     * SyncExecutor 为可选注入：非 Web 上下文中不存在，此时调用 sync 方法会抛出明确错误。
     */
    @Bean
    @ConditionalOnMissingBean(MasterDataSyncService.class)
    public MasterDataSyncService masterDataSyncService(
            @Autowired(required = false) SyncExecutor syncExecutor,
            SyncLogRepository syncLogRepository) {
        return new MasterDataSyncServiceImpl(syncExecutor, syncLogRepository);
    }

    /**
     * DAP Engine 主门面服务（{@link DapEngineService}），聚合 query / meta / sync 三个子服务。
     * 业务系统可直接注入此 Bean 操作主数据。
     */
    @Bean
    @ConditionalOnMissingBean(DapEngineService.class)
    public DapEngineService dapEngineService(MasterDataQueryService masterDataQueryService,
                                               MasterDataMetaService masterDataMetaService,
                                               MasterDataSyncService masterDataSyncService) {
        return new DapEngineServiceImpl(masterDataQueryService, masterDataMetaService,
                masterDataSyncService);
    }

    /**     * Admin REST Controller 及全局异常处理器（仅在 Spring MVC 可用时注册）。
     * 通过 {@code @ConditionalOnClass(DispatcherServlet)} 保护，避免在。Spring 上下文中误注册。
     */
    @Configuration
    @ConditionalOnClass(name = "org.springframework.web.servlet.DispatcherServlet")
    static class AdminWebConfiguration {

        /**
         * Admin 元数据管。REST Controller，提。Subject 和字段元数据。CRUD 接口。
         * URL 前缀 {@code /dap-engine/admin/metadata/}。
         */
        @Bean
        @ConditionalOnMissingBean(MetadataConfigController.class)
        public MetadataConfigController metadataConfigController(
                MetadataConfigService metadataConfigService,
                DapEngineSchemaInitializer dapEngineSchemaInitializer) {
            return new MetadataConfigController(metadataConfigService, dapEngineSchemaInitializer);
        }

        /**
         * 同步配置管理核心业务服务。
         * {@code SyncScheduler} 为可选注入（Phase 7 实现）；ObjectMapper 来自宿主 Spring Boot 上下文。
         */
        @Bean
        @ConditionalOnMissingBean(SyncConfigService.class)
        public SyncConfigService syncConfigService(
                SyncConfigRepository syncConfigRepository,
                SubjectRepository subjectRepository,
                MetadataConfigService metadataConfigService,
                SchemaStatusService schemaStatusService,
                AesCipher aesCipher,
                @Autowired(required = false) SyncScheduler syncScheduler,
                ObjectMapper objectMapper) {
            return new SyncConfigService(syncConfigRepository, subjectRepository, metadataConfigService,
                    schemaStatusService, aesCipher, syncScheduler, objectMapper);
        }

        /**
         * 同步执行器，负责 DELTA 。FULL_REFRESH 同步生命周期管理。
         */
        @Bean
        @ConditionalOnMissingBean(SyncExecutor.class)
        public SyncExecutor syncExecutor(SubjectRepository subjectRepository,
                                          SyncConfigRepository syncConfigRepository,
                                          CheckpointRepository checkpointRepository,
                                          SyncLogRepository syncLogRepository,
                                          List<DataProvider> dataProviders,
                                          FieldMappingService fieldMappingService,
                                          LocalDataWriter localDataWriter,
                                          @Autowired(required = false) List<SyncInterceptor> syncInterceptors,
                                          MetadataConfigService metadataConfigService,
                                          SchemaStatusService schemaStatusService,
                                          ObjectMapper objectMapper,
                                          SubjectSyncLockManager subjectSyncLockManager,
                                          @Autowired(required = false) MasterDataCacheService masterDataCacheService) {
            List<SyncInterceptor> interceptors = syncInterceptors != null
                    ? syncInterceptors : new java.util.ArrayList<>();
            return new SyncExecutor(subjectRepository, syncConfigRepository, checkpointRepository,
                    syncLogRepository, dataProviders, fieldMappingService, localDataWriter,
                    interceptors, metadataConfigService, schemaStatusService, objectMapper,
                    subjectSyncLockManager, masterDataCacheService);
        }

        /**
         * 同步配置管理 REST Controller。
         * URL 前缀 {@code /dap/{subjectCode}/sync}。
         */
        @Bean
        @ConditionalOnMissingBean(SyncConfigController.class)
        public SyncConfigController syncConfigController(SyncConfigService syncConfigService,
                                                         List<DataProvider> dataProviders,
                                                         SyncExecutor syncExecutor,
                                                         SyncLogRepository syncLogRepository) {
            return new SyncConfigController(syncConfigService, dataProviders, syncExecutor, syncLogRepository);
        }

        /**
         * HTTP DataProvider Bean，支。REST API 拉取数据。
         */
        @Bean
        @ConditionalOnMissingBean(HttpDataProvider.class)
        public HttpDataProvider httpDataProvider(AesCipher aesCipher, ObjectMapper objectMapper) {
            return new HttpDataProvider(aesCipher, objectMapper);
        }

        /**
         * DB DataProvider Bean，通过临时 Druid 连接池查询外部数据库。
         */
        @Bean
        @ConditionalOnMissingBean(DbDataProvider.class)
        public DbDataProvider dbDataProvider(AesCipher aesCipher) {
            return new DbDataProvider(aesCipher);
        }

        /**
         * MQ DataProvider Bean，通过 TCP Socket 探测 MQ 可达性。
         */
        @Bean
        @ConditionalOnMissingBean(MqDataProvider.class)
        public MqDataProvider mqDataProvider() {
            return new MqDataProvider();
        }

        /**
         * DAP Engine 控制器专用异常处理器。
         * 使用独立 Bean 名，避免与宿主应用或其它 SDK 的 {@code globalExceptionHandler} 发生命名冲突。
         */
        @Bean(name = "dapEngineGlobalExceptionHandler")
        @ConditionalOnMissingBean(name = "dapEngineGlobalExceptionHandler")
        public GlobalExceptionHandler globalExceptionHandler() {
            return new GlobalExceptionHandler();
        }

        /**
         * 主数据浏览 Admin REST Controller.
         * URL 前缀 {@code /dap-engine/admin/browse/{subjectCode}}.
         */
        @Bean
        @ConditionalOnMissingBean(MasterDataBrowseController.class)
        public MasterDataBrowseController masterDataBrowseController(MasterDataService masterDataService) {
            return new MasterDataBrowseController(masterDataService);
        }

        /**
         * HR 消息处理业务服务。
         */
        @Bean
        @ConditionalOnMissingBean(QueueMessageHandleService.class)
        public QueueMessageHandleService queueMessageHandleService(OSDSService osdsService,
                                                                   OrgSyncService orgSyncService,
                                                                   DapSysUserRepository dapSysUserRepository) {
            return new QueueMessageHandleService(osdsService, orgSyncService, dapSysUserRepository);
        }

        /**
         * HR 外部通知入口。
         */
        @Bean
        @ConditionalOnMissingBean(HrNotifyController.class)
        public HrNotifyController hrNotifyController(QueueMessageHandleService queueMessageHandleService) {
            return new HrNotifyController(queueMessageHandleService);
        }
    }
}
