package com.ruijie.authzengine.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 瑙掕壊鐩綍鎸佷箙鍖栧疄浣撱€?
 */
@Data
@TableName("authz_role")
@EqualsAndHashCode(callSuper = true)
public class AuthRoleEntity extends BaseEntity {

    @TableField("tenant_id")
    private String tenantId;

    @TableField("app_code")
    private String appCode;

    @TableField("role_code")
    private String roleCode;

    @TableField("role_name")
    private String roleName;

    @TableField("role_scope")
    private String roleScope;

    @TableField("status")
    private String status;
}