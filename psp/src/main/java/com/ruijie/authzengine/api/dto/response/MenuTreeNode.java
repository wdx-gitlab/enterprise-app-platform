package com.ruijie.authzengine.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * user-context 菜单树节点。
 */
@Data
@Builder
@Schema(description = "菜单树节点")
public class MenuTreeNode {

    @Schema(description = "菜单编码", example = "MENU_CONTRACT")
    private String menuCode;

    @Schema(description = "菜单名称", example = "合同管理")
    private String menuName;

    @Schema(description = "前端路由路径", example = "/contracts")
    private String routePath;

    @Schema(description = "子菜单节点")
    private List<MenuTreeNode> children;
}