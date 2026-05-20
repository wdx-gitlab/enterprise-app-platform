package com.ruijie.uspportal.portalconfig.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("usp_portal_param")
public class PortalParamEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("param_key")
    private String paramKey;

    @TableField("param_name")
    private String paramName;

    @TableField("param_group")
    private String paramGroup;

    @TableField("value_type")
    private String valueType;

    @TableField("param_value")
    private String paramValue;

    @TableField("default_value")
    private String defaultValue;

    private String status;

    private String description;
}
