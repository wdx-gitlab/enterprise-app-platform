package com.ruijie.dapengine.common.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 同步日志查询响应 DTO（GET /dap/{subjectCode}/sync/logs）。
 */
@Data
@NoArgsConstructor
public class SyncLogDTO {

    private Long id;

    private String subjectCode;

    private String subjectName;

    /** 同步模式：SCHEDULE / EVENT */
    private String syncMode;

    /** 触发动作：DELTA / FULL_REFRESH */
    private String action;

    /** 执行状态：SUCCESS / FAIL */
    private String status;

    /** 本次同步写入记录数 */
    private int recordCount;

    /** 本次跳过记录数 */
    private int skipCount;

    /** 耗时（毫秒） */
    private Long costMs;

    /**
     * 错误信息；FULL_REFRESH 成功时为备份表名（"BACKUP_TABLE:xxx"）。
     */
    private String errorMsg;

    /** 同步任务创建时间 */
    private LocalDateTime createdAt;
}
