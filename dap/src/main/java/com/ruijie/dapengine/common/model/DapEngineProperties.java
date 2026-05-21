package com.ruijie.dapengine.common.model;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * DAP Engine Starter 配置属性，前缀 dap.engine
 */
@Data
@ConfigurationProperties(prefix = "dap.engine")
public class DapEngineProperties {

    /** 是否启用 DAP Engine，默认 true */
    private boolean enabled = true;

    /** 租户 ID（必填） */
    private String tenantId;

    /** 接入业务应用编码（必填） */
    private String appCode;

    @NestedConfigurationProperty
    private SecurityProperties security = new SecurityProperties();

    @NestedConfigurationProperty
    private SyncProperties sync = new SyncProperties();

    @NestedConfigurationProperty
    private CacheProperties cache = new CacheProperties();

    @NestedConfigurationProperty
    private MigrationProperties migration = new MigrationProperties();

    // -------------------------------------------------------------------------

    @Data
    @ConfigurationProperties(prefix = "rj.unify.engine.datasource")
    public static class DataSourceProperties {
        /** JDBC Driver 类名，默认 com.mysql.cj.jdbc.Driver */
        private String driverClassName = "com.mysql.cj.jdbc.Driver";
        /** 平台库 JDBC URL（必填） */
        private String url;
        /** 平台库用户名（必填） */
        private String username;
        /** 平台库密码（必填） */
        private String password;
        /** 初始化连接数，默认 5 */
        private int initialSize = 5;
        /** 最小空闲连接数，默认 5 */
        private int minIdle = 5;
        /** 最大活跃连接数，默认 20 */
        private int maxActive = 20;
        /** 获取连接最大等待时间（毫秒），默认 60000 */
        private long maxWait = 60000L;
        /** 连接有效性校验 SQL，默认 SELECT 1 FROM DUAL */
        private String validationQuery = "SELECT 1 FROM DUAL";
        /** 是否在空闲检测时校验连接，默认 true */
        private boolean testWhileIdle = true;
        /** 是否在借出时校验连接，默认 false */
        private boolean testOnBorrow = false;
        /** 是否在归还时校验连接，默认 false */
        private boolean testOnReturn = false;
        /** 是否启用 Druid keepAlive，默认 true */
        private boolean keepAlive = true;
        /** 空闲连接检测周期（毫秒），默认 30000 */
        private long timeBetweenEvictionRunsMillis = 30000L;
        /** 连接最小空闲存活时间（毫秒），默认 30000 */
        private long minEvictableIdleTimeMillis = 30000L;
        /** 是否启用 PSCache，默认 true */
        private boolean poolPreparedStatements = true;
        /** 每个连接 PSCache 大小，默认 20 */
        private int maxPoolPreparedStatementPerConnectionSize = 20;
    }

    @Data
    public static class SecurityProperties {
        /** AES-256 加密密钥（必填，长度至少 16 字符，推荐 32 字符以上）；通过环境变量注入，不提交代码仓库 */
        private String encryptKey;
    }

    @Data
    public static class SyncProperties {
        /** 主数据写入目标：PLATFORM_DB（平台库）或 BUSINESS_DB（业务库）；默认 PLATFORM_DB */
        private StorageTarget targetStorage = StorageTarget.PLATFORM_DB;

        @NestedConfigurationProperty
        private HttpProperties http = new HttpProperties();

        @NestedConfigurationProperty
        private OsdsProperties osds = new OsdsProperties();

        /** 增量同步高水位安全延迟（毫秒），默认 30000ms */
        private int deltaSafeDelayMs = 30000;
    }

    @Data
    public static class HttpProperties {
        /** HTTP 连接超时（毫秒），默认 3000 */
        private int connectTimeoutMs = 3000;
        /** HTTP 读取超时（毫秒），默认 8000 */
        private int readTimeoutMs = 8000;
    }

    @Data
    public static class OsdsProperties {
        /** OSDS 服务基础地址。 */
        private String baseUrl = "http://service-gw.ruijie.com.cn/api";
    }

    @Data
    public static class CacheProperties {
        /** 缓存 TTL（分钟），默认 5 */
        private int ttlMinutes = 5;
        /** 最大缓存条数，默认 5000 */
        private int maximumSize = 5000;
        /**
         * 树形查询全量加载阈值。
         * 数据量 <= 此值时内存全量建树；> 此值时使用 WITH RECURSIVE CTE（MySQL 8.0+）。
         * 配置项：{@code dap.engine.cache.tree-full-load-threshold}，默认 5000。
         */
        private int treeFullLoadThreshold = 5000;
    }

    @Data
    public static class MigrationProperties {
        /** 是否执行 DAP Engine Flyway 迁移，默认 true；已手动管理 Schema 时可设为 false */
        private boolean enabled = true;
    }

    /** 主数据写入目标存储枚举 */
    public enum StorageTarget {
        /** DAP 平台库（独立 Schema） */
        PLATFORM_DB,
        /** 业务系统自身数据库 */
        BUSINESS_DB
    }
}
