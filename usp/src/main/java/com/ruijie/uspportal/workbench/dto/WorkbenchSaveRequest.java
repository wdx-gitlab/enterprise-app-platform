package com.ruijie.uspportal.workbench.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class WorkbenchSaveRequest {

    @NotBlank(message = "请输入工作台编码")
    private String workbenchCode;

    @NotBlank(message = "请输入工作台名称")
    private String workbenchName;

    @NotBlank(message = "请选择工作台类型")
    private String workbenchType;

    private String layoutTemplate;

    private Boolean defaultWorkbench;
}
