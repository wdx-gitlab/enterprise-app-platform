package com.ruijie.dapengine.common.model;

import lombok.Data;

/**
 * 字段元数据响应 DTO。
 */
@Data
public class FieldConfigDTO {
    private Long id;
    private String fieldName;
    private String fieldType;
    private Integer maxLength;
    private String fieldLabel;
    private boolean required;
    private String dictCode;
    private int sortOrder;
    /** 0=有效，1=废弃（逻辑删除） */
    private int isDelete;
    /** true 表示系统字段（code/name/parent_code），前端只读 */
    private boolean system;
}
