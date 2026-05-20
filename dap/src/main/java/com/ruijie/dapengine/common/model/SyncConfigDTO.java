package com.ruijie.dapengine.common.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 查询同步配置的响应体（GET /dap/{subjectCode}/sync）。
 *
 * <p>datasourceConfig 中的敏感字段已替换为掩码值 "****"，不回显原文。</p>
 */
@Data
@NoArgsConstructor
public class SyncConfigDTO {

    private String subjectCode;

    private String subjectName;

    /** 同步触发模式：SCHEDULE / EVENT */
    private String syncMode;

    /** 数据提供者类型：HTTP / DB / MQ */
    private String providerType;

    /** Cron 表达式（原文，SCHEDULE 模式时存在） */
    private String cronExpr;

    /** 数据源配置（敏感字段已掩码） */
    private SyncDataSourceConfig datasourceConfig;

    /** 字段映射规则列表 */
    private List<FieldMapping> fieldMapping;

    /** 同步动作：DELTA / FULL_REFRESH */
    private String syncAction;

    /** 状态：1=启用，0=停用 */
    private Integer status;
}
