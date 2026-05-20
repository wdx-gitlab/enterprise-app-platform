package com.ruijie.dapengine.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 字段映射规则：将外部数据源的 source 字段名映射为本地动态表的 target 列名。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FieldMapping {

    /** 外部数据源字段名（来自 DataProvider 返回的 records Map key） */
    private String source;

    /**
     * 本地动态表列名（必须在该 Subject 的字段元数据中存在，
     * 或为系统保留字段 code / name / parent_code）。
     */
    private String target;
}
