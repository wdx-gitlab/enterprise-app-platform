package com.ruijie.uspportal.portalconfig.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class FeatureFlagRuleSaveRequest {

    @NotBlank(message = "请选择规则类型")
    private String ruleType;

    @NotBlank(message = "请选择规则运算符")
    private String ruleOperator;

    @NotBlank(message = "请输入规则值")
    private String ruleValue;

    private Integer priorityNo;
}
