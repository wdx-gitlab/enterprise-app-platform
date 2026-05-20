package com.ruijie.dapengine.common.enums;

/**
 * 主数据字段类型枚举。
 * 枚举值与 dap_sys_metadata_config.field_type 列存储值一致。
 */
public enum FieldType {
    /** 短字符串，对应数据库列类型 {@code VARCHAR(256)}。 */
    STRING,
    /** 长字符串，对应数据库列类型 {@code VARCHAR(1024)}。 */
    STRING_LONG,
    /** 超长文本，对应数据库列类型 {@code TEXT}，不建议建索引。 */
    TEXT,
    /** 整数，对应数据库列类型 {@code BIGINT}。 */
    INT,
    /** 小数，对应数据库列类型 {@code DECIMAL(18,6)}。 */
    DECIMAL,
    /** 日期（不含时分秒），对应数据库列类型 {@code DATE}，格式 {@code yyyy-MM-dd}。 */
    DATE,
    /** 日期时间（含时分秒），对应数据库列类型 {@code DATETIME}，格式 {@code yyyy-MM-dd HH:mm:ss}。 */
    DATETIME,
    /** 枚举型，字段配置中 {@code dict_code} 必填，存储字典码值。 */
    ENUM
}
