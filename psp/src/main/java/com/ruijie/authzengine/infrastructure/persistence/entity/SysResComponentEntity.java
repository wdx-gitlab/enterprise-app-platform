package com.ruijie.authzengine.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 组件资源持久化实体。
 */
@Data
@TableName("usp_component")
@EqualsAndHashCode(callSuper = true)
public class SysResComponentEntity extends BaseEntity {

    @TableField("tenant_id")
    private String tenantId;

    @TableField("app_code")
    private String appCode;

    @TableField("component_code")
    private String componentCode;

    @TableField("component_name")
    private String componentName;

    @TableField("page_id")
    private Long pageId;

    @TableField("component_type")
    private String componentType;

    @TableField("status")
    private String status;
}