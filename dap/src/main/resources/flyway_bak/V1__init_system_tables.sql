-- DAP Engine 系统表初始化迁移脚本
-- Flyway 历史表: dap_schema_history（与业务系统 flyway_schema_history 区分）
-- 路径: classpath:db/dap.migration/V1__init_system_tables.sql

-- ============================================================
-- dap_sys_subject — 主数据类型定义
-- ============================================================
CREATE TABLE IF NOT EXISTS dap_sys_subject (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    tenant_id   VARCHAR(64)  NOT NULL DEFAULT '',
    app_code    VARCHAR(64)  NOT NULL DEFAULT '',
    code        VARCHAR(64)  NOT NULL,
    name        VARCHAR(128) NOT NULL,
    description VARCHAR(512),
    status      TINYINT      NOT NULL DEFAULT 1    COMMENT '1=启用 0=停用',
    is_tree     TINYINT      NOT NULL DEFAULT 0    COMMENT '1=树形 0=平铺',
    is_built_in TINYINT      NOT NULL DEFAULT 0    COMMENT '1=内置主题，不可删除',
    is_delete   TINYINT      NOT NULL DEFAULT 0    COMMENT '0=正常 1=已删除',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by  VARCHAR(64)  NOT NULL DEFAULT '',
    updated_by  VARCHAR(64)  NOT NULL DEFAULT '',
    UNIQUE KEY  uk_subject_code (code)
);

CREATE INDEX idx_subject_tenant_app ON dap_sys_subject (tenant_id, app_code);

-- ============================================================
-- dap_sys_metadata_config — 元数据字段配置
-- ============================================================
CREATE TABLE IF NOT EXISTS dap_sys_metadata_config (
    id           BIGINT       PRIMARY KEY AUTO_INCREMENT,
    tenant_id    VARCHAR(64)  NOT NULL DEFAULT '',
    app_code     VARCHAR(64)  NOT NULL DEFAULT '',
    subject_id   BIGINT       NOT NULL,
    subject_code VARCHAR(64)  NOT NULL,
    subject_name VARCHAR(128) NOT NULL DEFAULT '',
    field_name   VARCHAR(128) NOT NULL,
    field_type   VARCHAR(32)  NOT NULL  COMMENT 'STRING/STRING_LONG/TEXT/INT/DECIMAL/DATE/DATETIME/ENUM',
    field_label  VARCHAR(128),
    required     TINYINT      NOT NULL DEFAULT 0,
    max_length   INT          NOT NULL DEFAULT 0    COMMENT '字符串/枚举字段最大长度；0 表示按字段类型默认值',
    dict_code    VARCHAR(64),
    sort_order   INT          NOT NULL DEFAULT 0,
    is_delete    TINYINT      NOT NULL DEFAULT 0,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by   VARCHAR(64)  NOT NULL DEFAULT '',
    updated_by   VARCHAR(64)  NOT NULL DEFAULT '',
    UNIQUE KEY   uk_subject_field (subject_id, field_name)
);

CREATE INDEX idx_metadata_subject ON dap_sys_metadata_config (subject_code);

-- ============================================================
-- dap_sys_sync_config — 同步配置
-- ============================================================
CREATE TABLE IF NOT EXISTS dap_sys_sync_config (
    id                BIGINT       PRIMARY KEY AUTO_INCREMENT,
    tenant_id         VARCHAR(64)  NOT NULL DEFAULT '',
    app_code          VARCHAR(64)  NOT NULL DEFAULT '',
    subject_id        BIGINT       NOT NULL,
    subject_code      VARCHAR(64)  NOT NULL,
    subject_name      VARCHAR(128) NOT NULL DEFAULT '',
    sync_mode         VARCHAR(16)  NOT NULL  COMMENT 'SCHEDULE/EVENT',
    provider_type     VARCHAR(16)  NOT NULL  COMMENT 'HTTP/DB/MQ',
    cron_expr         VARCHAR(64),
    datasource_config TEXT                   COMMENT '数据源 JSON；敏感字段 AES 加密',
    field_mapping     TEXT                   COMMENT '字段映射 JSON 数组',
    sync_action       VARCHAR(16)  NOT NULL DEFAULT 'DELTA' COMMENT 'DELTA/FULL_REFRESH',
    status            TINYINT      NOT NULL DEFAULT 1,
    is_delete         TINYINT      NOT NULL DEFAULT 0,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by        VARCHAR(64)  NOT NULL DEFAULT '',
    updated_by        VARCHAR(64)  NOT NULL DEFAULT '',
    UNIQUE KEY        uk_sync_subject (subject_id)
);

-- ============================================================
-- dap_sys_checkpoint — 同步位点
-- ============================================================
CREATE TABLE IF NOT EXISTS dap_sys_checkpoint (
    id              BIGINT      PRIMARY KEY AUTO_INCREMENT,
    tenant_id       VARCHAR(64) NOT NULL DEFAULT '',
    app_code        VARCHAR(64) NOT NULL DEFAULT '',
    subject_code    VARCHAR(64) NOT NULL,
    last_version    BIGINT      NOT NULL DEFAULT 0   COMMENT '上次同步批次毫秒时间戳',
    last_sync_time  DATETIME,
    record_count    BIGINT      NOT NULL DEFAULT 0,
    safe_delay_ms   INT         NOT NULL DEFAULT 30000 COMMENT '增量同步高水位安全延迟（毫秒）',
    is_delete       TINYINT     NOT NULL DEFAULT 0,
    created_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by      VARCHAR(64) NOT NULL DEFAULT '',
    updated_by      VARCHAR(64) NOT NULL DEFAULT '',
    UNIQUE KEY      uk_checkpoint_subject (subject_code)
);

-- ============================================================
-- dap_sys_sync_log — 同步日志
-- ============================================================
CREATE TABLE IF NOT EXISTS dap_sys_sync_log (
    id           BIGINT       PRIMARY KEY AUTO_INCREMENT,
    tenant_id    VARCHAR(64)  NOT NULL DEFAULT '',
    app_code     VARCHAR(64)  NOT NULL DEFAULT '',
    subject_code VARCHAR(64)  NOT NULL DEFAULT '',
    subject_name VARCHAR(128) NOT NULL DEFAULT '',
    sync_mode    VARCHAR(16)           COMMENT 'SCHEDULE/EVENT',
    action       VARCHAR(16)           COMMENT 'DELTA/FULL_REFRESH',
    status       VARCHAR(16)           COMMENT 'SUCCESS/FAIL',
    record_count INT          NOT NULL DEFAULT 0,
    skip_count   INT          NOT NULL DEFAULT 0  COMMENT '校验失败跳过条数',
    cost_ms      BIGINT,
    error_msg    TEXT,
    is_delete    TINYINT      NOT NULL DEFAULT 0,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by   VARCHAR(64)  NOT NULL DEFAULT '',
    updated_by   VARCHAR(64)  NOT NULL DEFAULT ''
);

CREATE INDEX idx_sync_log_subject_time ON dap_sys_sync_log (subject_code, created_at);
