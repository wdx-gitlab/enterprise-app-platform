package com.ruijie.uspportal.workbench.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class WidgetSaveRequest {

    @NotNull(message = "请选择工作台")
    private Long workbenchId;

    @NotBlank(message = "请输入组件编码")
    private String widgetCode;

    @NotBlank(message = "请输入组件名称")
    private String widgetName;

    @NotBlank(message = "请选择组件类型")
    private String widgetType;

    private Long sourceAppId;

    private String propsJson;

    private Integer rowNo;

    private Integer colNo;

    private Integer width;

    private Integer height;

    private String permissionCode;
}
