package com.ruijie.dapengine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * dap_sys_sync_log 表持久化实体。
 *
 * <p>记录每次同步任务的执行结果。FULL_REFRESH 时 errorMsg 字段冗用于存储备份表名，
 * 格式为 "BACKUP_TABLE:dap_{subject}_bak_{yyyyMMddHHmmss}"。</p>
 */
@Data
@TableName("dap_sys_sync_log")
public class SyncLogEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String tenantId;

    private String appCode;

    private String subjectCode;

    private String subjectName;

    /** 同步模式：SCHEDULE / EVENT */
    private String syncMode;

    /** 触发动作：DELTA / FULL_REFRESH */
    private String action;

    /** 执行状态：SUCCESS / FAIL */
    private String status;

    /** 本次同步写入记录数 */
    private Integer recordCount;

    /** 本次跳过记录数（校验失败等原因） */
    private Integer skipCount;

    /** 本次同步耗时（毫秒） */
    private Long costMs;

    /**
     * 错误信息；FULL_REFRESH 成功时记录备份表名（"BACKUP_TABLE:xxx"）。
     */
    private String errorMsg;

    /** 逻辑删除：0=正常，1=已删除 */
    private Integer isDelete;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String createdBy;

    private String updatedBy;
}
