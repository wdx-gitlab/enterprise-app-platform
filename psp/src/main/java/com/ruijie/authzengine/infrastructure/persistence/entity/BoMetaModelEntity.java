package com.ruijie.authzengine.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 涓氬姟瀵硅薄鍏冩ā鍨嬫寔涔呭寲瀹炰綋銆?
 */
@Data
@TableName("authz_bo_meta_model")
@EqualsAndHashCode(callSuper = true)
public class BoMetaModelEntity extends BaseEntity {

    @TableField("tenant_id")
    private String tenantId;

    @TableField("app_code")
    private String appCode;

    @TableField("bo_code")
    private String boCode;

    @TableField("bo_name")
    private String boName;

    @TableField("schema_json")
    private String schemaJson;

    @TableField("adapter_type")
    private String adapterType;

    @TableField("resolver")
    private String resolver;
}