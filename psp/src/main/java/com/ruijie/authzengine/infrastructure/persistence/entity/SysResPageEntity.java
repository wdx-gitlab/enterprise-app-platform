package com.ruijie.authzengine.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 页面资源持久化实体。
 */
@Data
@TableName("usp_page")
@EqualsAndHashCode(callSuper = true)
public class SysResPageEntity extends BaseEntity {

    @TableField("tenant_id")
    private String tenantId;

    @TableField("app_code")
    private String appCode;

    @TableField("page_code")
    private String pageCode;

    @TableField("page_name")
    private String pageName;

    @TableField("menu_id")
    private Long menuId;

    @TableField("page_path")
    private String pagePath;

    @TableField("status")
    private String status;

    /** 显示排序号，数值越小越靠前。 */
    @TableField("sort_order")
    private Integer sortOrder;
}