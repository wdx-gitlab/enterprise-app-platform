package com.ruijie.dapengine.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 手动触发同步的响应体（POST /dap/{subjectCode}/sync/trigger）。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SyncResultDTO {

    private String subjectCode;

    /** 是否同步成功 */
    private boolean success;

    /** 本次写入记录数 */
    private int recordCount;

    /** 本次同步耗时（毫秒） */
    private long costMs;

    /** 结果描述（成功时为 "同步完成"，失败时包含错误摘要） */
    private String message;

    /** 触发动作：DELTA / FULL_REFRESH */
    private String action;
}
