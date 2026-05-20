package com.ruijie.uspportal.portalconfig.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("usp_feature_flag_rule")
public class FeatureFlagRuleEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("flag_id")
    private Long flagId;

    @TableField("rule_type")
    private String ruleType;

    @TableField("rule_operator")
    private String ruleOperator;

    @TableField("rule_value")
    private String ruleValue;

    @TableField("priority_no")
    private Integer priorityNo;

    private String status;
}
