package com.ruijie.dapengine.common.enums;

/**
 * 本地写入动作类型（写入侧）。
 * UPSERT：INSERT ON DUPLICATE KEY UPDATE；FULL_REFRESH：全量换表；DELETE：按 code 删除。
 */
public enum SyncAction {
    UPSERT,
    FULL_REFRESH,
    DELETE
}
