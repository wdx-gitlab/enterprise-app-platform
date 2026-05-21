package com.ruijie.authzengine.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 菜单项持久化实体，对应 usp_menu_item 表。
 * 该表主键采用数据库 AUTO_INCREMENT，审计时间字段为 created_time/updated_time，
 * 与 BaseEntity 约定不同，因此不继承 BaseEntity，独立声明所有字段。
 */
@Data
@TableName("usp_menu_item")
public class SysResMenuEntity {

    /** 主键，数据库自增。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private String tenantId;

    @TableField("tenant_code")
    private String tenantCode;

    @TableField("app_code")
    private String appCode;

    @TableField("app_id")
    private Long appId;

    @TableField("menu_code")
    private String menuCode;

    @TableField("menu_name")
    private String menuName;

    @TableField("menu_icon")
    private String menuIcon;

    /** 菜单类型：DIRECTORY / MENU / LINK。 */
    @TableField("menu_type")
    private String menuType;

    @TableField("route_path")
    private String routePath;

    @TableField("target_url")
    private String targetUrl;

    /** 父节点主键 ID，根节点为 null。 */
    @TableField("parent_id")
    private Long parentId;

    /** 排序号，数值越小越靠前。 */
    @TableField("sort_no")
    private Integer sortNo;

    @TableField("tree_level")
    private Integer treeLevel;

    @TableField("tree_path")
    private String treePath;

    @TableField("permission_code")
    private String permissionCode;

    @TableField("visible_expression")
    private String visibleExpression;

    /** 发布状态：DRAFT / PUBLISHED。 */
    @TableField("publish_status")
    private String publishStatus;

    @TableField("status")
    private String status;

    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(value = "created_by", fill = FieldFill.INSERT)
    private String createdBy;

    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;

    @TableField(value = "updated_by", fill = FieldFill.INSERT_UPDATE)
    private String updatedBy;

    @TableLogic
    @TableField(value = "is_deleted", fill = FieldFill.INSERT)
    private Integer isDeleted;
}