package com.ruijie.uspportal.workbench.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("usp_workbench")
public class WorkbenchEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("workbench_code")
    private String workbenchCode;

    @TableField("workbench_name")
    private String workbenchName;

    @TableField("workbench_type")
    private String workbenchType;

    @TableField("owner_user_id")
    private String ownerUserId;

    @TableField("layout_template")
    private String layoutTemplate;

    @TableField("is_default")
    private Boolean defaultWorkbench;

    private String status;

    @TableField("tenant_code")
    private String tenantCode;

    @TableField("is_deleted")
    private Integer deleted;
}
