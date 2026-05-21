package com.ruijie.authzengine.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 鏍囧噯绛栫暐妯℃澘鎸佷箙鍖栧疄浣撱€?
 */
@Data
@TableName("authz_std_pol_template")
@EqualsAndHashCode(callSuper = true)
public class StandardPolicyTemplateEntity extends BaseEntity {

    @TableField("tenant_id")
    private String tenantId;

    @TableField("template_code")
    private String templateCode;

    @TableField("template_name")
    private String templateName;

    @TableField("pol_type")
    private String polType;

    @TableField("expression_script")
    private String expressionScript;

    @TableField("param_schema")
    private String paramSchema;

    @TableField("status")
    private String status;
}