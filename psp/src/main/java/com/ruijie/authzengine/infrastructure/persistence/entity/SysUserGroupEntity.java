package com.ruijie.authzengine.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户组目录持久化实体。
 */
@Data
@TableName("authz_usergroup")
@EqualsAndHashCode(callSuper = true)
public class SysUserGroupEntity extends BaseEntity {

    @TableField("tenant_id")
    private String tenantId;

    @TableField("app_code")
    private String appCode;

    @TableField("group_code")
    private String groupCode;

    @TableField("group_name")
    private String groupName;

    @TableField("status")
    private String status;
}