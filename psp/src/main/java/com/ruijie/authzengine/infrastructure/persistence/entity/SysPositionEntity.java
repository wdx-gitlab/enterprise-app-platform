package com.ruijie.authzengine.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 岗位目录持久化实体。
 */
@Data
@TableName("authz_position")
@EqualsAndHashCode(callSuper = true)
public class SysPositionEntity extends BaseEntity {

    @TableField("tenant_id")
    private String tenantId;

    @TableField("app_code")
    private String appCode;

    @TableField("position_code")
    private String positionCode;

    @TableField("position_name")
    private String positionName;

    @TableField("org_id")
    private Long orgId;

    @TableField("status")
    private String status;
}