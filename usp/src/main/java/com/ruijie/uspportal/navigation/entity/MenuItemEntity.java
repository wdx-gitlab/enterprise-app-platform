package com.ruijie.uspportal.navigation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

import org.springframework.util.StringUtils;

@Data
@TableName("usp_menu_item")
public class MenuItemEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private String tenantId;

    @TableField("menu_code")
    private String menuCode;

    @TableField("menu_name")
    private String menuName;

    @TableField("menu_icon")
    private String menuIcon;

    @TableField("menu_type")
    private String menuType;

    @TableField("app_code")
    private String appCode;

    @TableField("app_id")
    private Long appId;

    @TableField("route_path")
    private String routePath;

    @TableField("target_url")
    private String targetUrl;

    @TableField("parent_id")
    private Long parentId;

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

    @TableField("publish_status")
    private String publishStatus;

    private String status;

    @TableField("created_by")
    private String createdBy;

    @TableField("created_time")
    private LocalDateTime createdTime;

    @TableField("updated_by")
    private String updatedBy;

    @TableField("updated_time")
    private LocalDateTime updatedTime;

    @TableField("tenant_code")
    private String tenantCode;

    @TableField("is_deleted")
    private Integer deleted;

    public String resolvedTargetPath() {
        return StringUtils.hasText(targetUrl) ? targetUrl : routePath;
    }

    public String resolvedTargetType() {
        return StringUtils.hasText(targetUrl) || "LINK".equalsIgnoreCase(menuType) ? "EXTERNAL_URL" : "INTERNAL_ROUTE";
    }

    public String resolvedOpenMode() {
        return "EXTERNAL_URL".equals(resolvedTargetType()) ? "NEW_TAB" : "CURRENT_TAB";
    }
}
