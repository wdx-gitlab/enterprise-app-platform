-- V3: dap_sys_sync_log 补充 action 列（DELTA / FULL_REFRESH）
-- 已有数据库执行此脚本；新建数据库由 V1 直接建表，无需执行。
ALTER TABLE dap_sys_sync_log
    ADD COLUMN action VARCHAR(16) COMMENT 'DELTA/FULL_REFRESH' AFTER sync_mode;
