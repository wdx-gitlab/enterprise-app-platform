package com.ruijie.uspportal.portalconfig.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
public class FeatureFlagSaveRequest {

    @NotBlank(message = "请输入开关键")
    private String flagKey;

    @NotBlank(message = "请输入开关名称")
    private String flagName;

    private String description;

    private String status;

    private List<FeatureFlagRuleSaveRequest> rules;
}
