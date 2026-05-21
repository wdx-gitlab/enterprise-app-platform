package com.ruijie.authzengine.application.sdk.model;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户权限上下文结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserContextResult {

    private String subjectId;

    private String subjectModel;

    private List<String> permCodes;

    private Map<String, List<String>> accessibleResources;

    private List<MenuTreeNode> menuTree;

    private Map<String, UiVisibilityResult.VisibilityItem> visibility;

    private long evalTimeMs;

    /**
     * 可直接渲染的菜单树节点。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MenuTreeNode {

        private String menuCode;

        private String menuName;

        private String routePath;

        private List<MenuTreeNode> children;
    }
}