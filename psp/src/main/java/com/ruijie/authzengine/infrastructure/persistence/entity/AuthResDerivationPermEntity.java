package com.ruijie.authzengine.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 娲剧敓鏉冮檺鍏宠仈鎸佷箙鍖栧疄浣撱€?
 */
@Data
@TableName("authz_res_derivation_perm")
@EqualsAndHashCode(callSuper = true)
public class AuthResDerivationPermEntity extends BaseEntity {

    @TableField("tenant_id")
    private String tenantId;

    @TableField("app_code")
    private String appCode;

    @TableField("res_type")
    private String resType;

    @TableField("res_id")
    private Long resId;

    @TableField("perm_item_id")
    private Long permItemId;

    @TableField("sort_order")
    private Integer sortOrder;
}