package com.ruijie.dapengine.common.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 保存同步配置的请求体（POST /dap/{subjectCode}/sync）。
 *
 * <p>datasourceConfig 中的敏感字段（password、Authorization 等 Header 值）在请求中为明文，
 * SyncConfigService 接收后进行 AES-256 加密再落库。</p>
 */
@Data
@NoArgsConstructor
public class SyncConfigRequest {

    /** 同步触发模式：SCHEDULE / EVENT */
    private String syncMode;

    /** 数据提供者类型：HTTP / DB / MQ */
    private String providerType;

    /**
     * Cron 表达式（Spring 6 位格式，含秒字段）。
     * syncMode=SCHEDULE 时必填；syncMode=EVENT 时须为 null 或空。
     */
    private String cronExpr;

    /** 数据源配置（明文，含 password 等敏感字段） */
    private SyncDataSourceConfig datasourceConfig;

    /** 字段映射规则列表，不能为空 */
    private List<FieldMapping> fieldMapping;

    /**
     * 同步动作：DELTA（增量 UPSERT）/ FULL_REFRESH（全量换表）。
     * 默认 DELTA。
     */
    private String syncAction = "DELTA";

    /** 配置状态：1=启用，0=停用；默认启用 */
    private Integer status = 1;
}
