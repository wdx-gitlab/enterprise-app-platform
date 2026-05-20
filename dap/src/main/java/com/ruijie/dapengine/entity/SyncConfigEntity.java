package com.ruijie.dapengine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * dap_sys_sync_config 表持久化实体。
 *
 * <p>datasourceConfig 字段存储经 AES-256 加密的 JSON 字符串（password/敏感 Header 字段级加密）。
 * fieldMapping 字段存储字段映射规则 JSON 数组（明文）。</p>
 *
 * <p>status 使用 Integer 类型避免 Lombok @Data 对 boolean 生成 isXxx() getter 导致
 * MyBatis-Plus 列名映射偏移（与 SubjectEntity 保持一致）。</p>
 */
@Data
@TableName("dap_sys_sync_config")
public class SyncConfigEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String tenantId;

    private String appCode;

    private Long subjectId;

    private String subjectCode;

    private String subjectName;

    /** 同步触发模式：SCHEDULE / EVENT */
    private String syncMode;

    /** 数据提供者类型：HTTP / DB / MQ */
    private String providerType;

    /** Cron 表达式，SCHEDULE 模式时必填（Spring 6 位格式，含秒字段） */
    private String cronExpr;

    /** 数据源配置 JSON（含 AES 加密的敏感字段） */
    private String datasourceConfig;

    /** 字段映射规则 JSON 数组，格式：[{"source":"x","target":"y"}] */
    private String fieldMapping;

    /** 同步动作：DELTA / FULL_REFRESH */
    private String syncAction;

    /** 状态：1=启用，0=停用 */
    private Integer status;

    /** 逻辑删除：0=正常，1=已删除 */
    private Integer isDelete;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String createdBy;

    private String updatedBy;
}
