package com.ruijie.dapengine.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 同步位点内存模型（非数据库实体，传递给 DataProvider 用于增量参数绑定）。
 *
 * <p>lastVersion 为毫秒时间戳，用于 HTTP 增量参数和 DB SQL 占位符绑定。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SyncCheckpoint {

    private String subjectCode;

    /** 上次同步批次毫秒时间戳（0 表示初次同步，无历史位点） */
    private long lastVersion;

    /** 上次同步完成时间（可为 null，初次同步时为 null） */
    private LocalDateTime lastSyncTime;

    /** 已同步总记录数 */
    private long recordCount;

    /**
     * 构建一个空位点（初次同步使用）。
     */
    public static SyncCheckpoint empty(String subjectCode) {
        return new SyncCheckpoint(subjectCode, 0L, null, 0L);
    }
}
