package com.ruijie.uspportal.host.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 宿主直接消费的菜单信息。
 */
@Data
@Builder
public class HostMenuItemResponse {

    private Long id;

    private String tenantId;

    private String tenantCode;

    private String menuCode;

    private String menuName;

    private String menuIcon;

    private String menuType;

    private String appCode;

    private Long appId;

    private String routePath;

    private String targetUrl;

    private Long parentId;

    private Integer sortNo;

    private Integer treeLevel;

    private String treePath;

    private String permissionCode;

    private String visibleExpression;

    private String publishStatus;

    private String status;

    private String createdBy;

    private LocalDateTime createdTime;

    private String updatedBy;

    private LocalDateTime updatedTime;
}