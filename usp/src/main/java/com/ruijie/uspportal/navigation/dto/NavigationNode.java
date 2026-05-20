package com.ruijie.uspportal.navigation.dto;

import com.ruijie.uspportal.navigation.entity.MenuItemEntity;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class NavigationNode {

    private Long id;

    private String menuCode;

    private String menuName;

    private String menuIcon;

    private String menuType;

    private Long appId;

    private String routePath;

    private String targetUrl;

    private String permissionCode;

    private List<NavigationNode> children;

    public static NavigationNode from(MenuItemEntity entity) {
        return NavigationNode.builder()
                .id(entity.getId())
                .menuCode(entity.getMenuCode())
                .menuName(entity.getMenuName())
                .menuIcon(entity.getMenuIcon())
                .menuType(entity.getMenuType())
                .appId(entity.getAppId())
                .routePath(entity.getRoutePath())
                .targetUrl(entity.getTargetUrl())
                .permissionCode(entity.getPermissionCode())
                .children(new ArrayList<>())
                .build();
    }
}
