package com.ruijie.uspportal.portalconfig.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("usp_feature_flag")
public class FeatureFlagEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("flag_key")
    private String flagKey;

    @TableField("flag_name")
    private String flagName;

    private String description;

    private String status;
}
