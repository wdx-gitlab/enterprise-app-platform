package com.ruijie.dapengine.common.enums;

/**
 * 同步触发模式。
 * SCHEDULE：按 cron 表达式定时触发；EVENT：外部事件驱动触发（Phase 5+ 实现）。
 */
public enum SyncMode {
    SCHEDULE,
    EVENT
}
