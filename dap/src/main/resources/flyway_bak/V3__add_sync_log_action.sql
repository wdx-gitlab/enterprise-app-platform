-- V3: dap_sys_sync_log 补充 action 列（DELTA / FULL_REFRESH）
-- 已有数据库执行此脚本；新建数据库由 V1 直接建表，无需执行。
-- 使用 prepared statement 实现幂等性（MySQL 8.0 不支持 ADD COLUMN IF NOT EXISTS）
SET @dap_action_col_count = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'dap_sys_sync_log'
      AND COLUMN_NAME  = 'action'
);
SET @dap_sql = IF(
    @dap_action_col_count = 0,
    'ALTER TABLE dap_sys_sync_log ADD COLUMN action VARCHAR(16) COMMENT ''DELTA/FULL_REFRESH'' AFTER sync_mode',
    'SELECT 1'
);
PREPARE dap_stmt FROM @dap_sql;
EXECUTE dap_stmt;
DEALLOCATE PREPARE dap_stmt;
