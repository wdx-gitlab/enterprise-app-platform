package com.ruijie.dapengine.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ruijie.dapengine.common.enums.SchemaStatus;
import lombok.Data;

import java.util.List;

/**
 * Subject 响应 DTO，包含动态计算的 schemaStatus 与字段列表。
 */
@Data
public class SubjectDTO {
    private Long id;
    private String code;
    private String name;
    private String description;
    @JsonProperty("isTree")
    private boolean isTree;
    /** 是否内置主题：内置主题不可删除 */
    @JsonProperty("isBuiltIn")
    private boolean isBuiltIn;
    private int status;
    /** 动态计算：APPLIED/PENDING */
    private SchemaStatus schemaStatus;
    private String createdAt;
    private String updatedAt;
    private List<FieldConfigDTO> fields;
    /** 逻辑删除标志：0=正常，1=已删除 */
    private Integer isDelete;
}
