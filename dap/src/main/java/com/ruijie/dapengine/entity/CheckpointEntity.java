package com.ruijie.dapengine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * dap_sys_checkpoint 表持久化实体。
 *
 * <p>记录每个 Subject 的同步位点信息，供 DELTA 增量同步确定拉取起点。
 * subject_code 列有唯一约束，UPSERT 以此字段为冲突键。</p>
 */
@Data
@TableName("dap_sys_checkpoint")
public class CheckpointEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String tenantId;

    private String appCode;

    /** Subject 唯一编码，唯一键 */
    private String subjectCode;

    /** 上次同步批次毫秒时间戳（用于增量过滤参数） */
    private Long lastVersion;

    /** 上次同步完成时间 */
    private LocalDateTime lastSyncTime;

    /** 已同步总记录数（累计） */
    private Long recordCount;

    /** 增量安全延迟（毫秒），避免时钟漂移导致数据遗漏，默认 30000 */
    private Integer safeDelayMs;

    /** 逻辑删除：0=正常，1=已删除 */
    private Integer isDelete;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String createdBy;

    private String updatedBy;
}
