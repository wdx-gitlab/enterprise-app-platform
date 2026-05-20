package com.ruijie.uspportal.workbench.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruijie.uspportal.config.mybatis.JsonStringTypeHandler;
import org.apache.ibatis.type.JdbcType;
import lombok.Data;

@Data
@TableName(value = "usp_workbench_widget", autoResultMap = true)
public class WorkbenchWidgetEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("workbench_id")
    private Long workbenchId;

    @TableField("widget_code")
    private String widgetCode;

    @TableField("widget_name")
    private String widgetName;

    @TableField("widget_type")
    private String widgetType;

    @TableField("source_app_id")
    private Long sourceAppId;

    @TableField(value = "props_json", typeHandler = JsonStringTypeHandler.class, jdbcType = JdbcType.VARCHAR)
    private String propsJson;

    @TableField("row_no")
    private Integer rowNo;

    @TableField("col_no")
    private Integer colNo;

    private Integer width;

    private Integer height;

    @TableField("permission_code")
    private String permissionCode;

    private String status;

    @TableField("is_deleted")
    private Integer deleted;
}
