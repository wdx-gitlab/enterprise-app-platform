package com.ruijie.authzengine.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 鏉冮檺鍏冩ā鍨嬫寔涔呭寲瀹炰綋銆?
 */
@Data
@TableName("authz_meta_model")
@EqualsAndHashCode(callSuper = true)
public class AuthMetaModelEntity extends BaseEntity {

    @TableField("tenant_id")
    private String tenantId;

    @TableField("app_code")
    private String appCode;

    @TableField("model_code")
    private String modelCode;

    @TableField("model_name")
    private String modelName;

    @TableField("category")
    private String category;

    @TableField("adapter_type")
    private String adapterType;

    @TableField("resolver")
    private String resolver;

    @TableField("schema_view")
    private String schemaView;
}