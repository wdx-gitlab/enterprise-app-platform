package com.ruijie.authzengine.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 鏍囧噯鍔ㄤ綔鎸佷箙鍖栧疄浣撱€?
 */
@Data
@TableName("authz_std_act_dict")
@EqualsAndHashCode(callSuper = true)
public class StandardActionEntity extends BaseEntity {

    @TableField("tenant_id")
    private String tenantId;

    @TableField("act_code")
    private String actCode;

    @TableField("act_name")
    private String actName;

    @TableField("act_type")
    private String actType;

    @TableField("res_category")
    private String resCategory;

    @TableField("act_aliases")
    private String actAliases;

    @TableField("risk_level")
    private Integer riskLevel;
}