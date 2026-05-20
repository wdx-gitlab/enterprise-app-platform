package com.ruijie.uspportal.portalconfig.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("usp_portal_param_history")
public class PortalParamHistoryEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("param_id")
    private Long paramId;

    @TableField("old_value")
    private String oldValue;

    @TableField("new_value")
    private String newValue;

    @TableField("changed_by")
    private String changedBy;

    private String remark;
}
