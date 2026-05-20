package com.ruijie.dapengine.common.enums;

/**
 * 外部触发动作（对外 API 层）。
 * DELTA：增量同步（UPSERT 写入）；FULL_REFRESH：全量换表刷新。
 */
public enum TriggerAction {
    DELTA,
    FULL_REFRESH
}
