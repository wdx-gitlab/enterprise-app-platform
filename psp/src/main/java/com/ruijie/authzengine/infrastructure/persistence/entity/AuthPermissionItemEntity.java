package com.ruijie.authzengine.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 鏉冮檺椤规寔涔呭寲瀹炰綋銆?
 */
@Data
@TableName("authz_permission_item")
@EqualsAndHashCode(callSuper = true)
public class AuthPermissionItemEntity extends BaseEntity {

    @TableField("tenant_id")
    private String tenantId;

    @TableField("app_code")
    private String appCode;

    @TableField("perm_code")
    private String permCode;

    @TableField("res_model_code")
    private String resModelCode;

    @TableField("res_id")
    private String resId;

    @TableField("act_code")
    private String actCode;

    @TableField("fail_strategy")
    private String failStrategy;
}