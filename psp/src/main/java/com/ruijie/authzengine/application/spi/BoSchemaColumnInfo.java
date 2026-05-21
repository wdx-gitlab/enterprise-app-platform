package com.ruijie.authzengine.application.spi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * BO 元数据采集结果中的单列信息。
 *
 * <p>由 {@link BoMetaModelAdapter#fetchBoSchema} 或 Native JDBC 采集器返回，
 * 供前端治理页展示结构化字段清单，辅助管理员配置 {@code entities[].attributes[]} 初稿。
 *
 * <p><b>语义约束</b>：
 * <ul>
 *   <li>该对象仅作为"候选输入"展示给管理员，<b>不能直接落库</b>，必须经过治理确认。</li>
 *   <li>前端可按以下规则生成默认值：{@code code=columnName}、
 *       {@code fieldName=列名转驼峰}、{@code filterable=false}、{@code fieldControl=false}。</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoSchemaColumnInfo {

    /** 表名，对应实体级 {@code tableName}。 */
    private String tableName;

    /** 列名（物理列名），对应属性级 {@code columnName}。 */
    private String columnName;

    /**
     * 列数据类型，映射到属性级 {@code type}（STRING / LONG / INTEGER / DECIMAL / BOOLEAN / DATE / DATETIME）。
     * <p>采集到的原始 DB 类型由采集器负责映射到引擎类型枚举，无法映射时置为 {@code STRING} 兜底。</p>
     */
    private String columnType;

    /** 是否为主键列，对应属性级 {@code isPk}。 */
    private boolean isPrimaryKey;

    /** 是否可空；供页面提示，不强制影响 {@code attributes[]} 结构。 */
    private boolean nullable;

    /** 列注释或展示名候选，对应属性级 {@code name} 的默认值候选。 */
    private String comment;
}
