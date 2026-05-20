package com.ruijie.dapengine.common.model;

import lombok.Data;

/**
 * 字段配置请求对象，用于 POST/PUT Subject 请求中的字段列表。
 */
@Data
public class FieldConfigRequest {
    /** 字段名，正则 ^[a-z][a-z0-9_]{0,127}$，不得为系统保留字 */
    private String fieldName;
    /** 字段类型，对应 FieldType 枚举 */
    private String fieldType;
    /** 字符串/枚举字段最大长度；非长度敏感字段可为空 */
    private Integer maxLength;
    /** 字段显示名 */
    private String fieldLabel;
    /** 是否必填，默认 false */
    private boolean required = false;
    /** 字典编码，ENUM 类型时必填 */
    private String dictCode;
    /** 排序权重，默认 0 */
    private int sortOrder = 0;
}
