package com.ruijie.authzengine.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

/**
 * 用户权限上下文合集响应 DTO（Q4）。
 *
 * <p>一次返回：权限项快照 + 可访问资源列表 + UI 元素可见性，
 * 适用于用户登录后系统初始化场景，减少网络往返次数。
 */
@Data
@Builder
@Schema(description = "用户权限上下文合集响应（Q4）")
public class UserContextResponse {

    @Schema(description = "主体标识", example = "U10001")
    private String subjectId;

    @Schema(description = "主体类型", example = "SUB_USER")
    private String subjectModel;

    @Schema(description = "主体拥有的全部权限项编码列表（含角色/岗位/组织间接授权，不含策略过滤）",
        example = "[\"PERM_CONTRACT_READ\", \"PERM_CONTRACT_APPROVE\"]")
    private List<String> permCodes;

    @Schema(description = "可访问资源列表，key 为资源类型，value 为该类型下有权访问的资源编码列表",
        example = "{\"RES_UI_MENU\": [\"MENU_HOME\", \"MENU_CONTRACT_LIST\"]}")
    private Map<String, List<String>> accessibleResources;

    @Schema(description = "可直接渲染的菜单树")
    private List<MenuTreeNode> menuTree;

    @Schema(description = "UI 组件可见性状态，key 为 componentCode，value 为可见性状态")
    private Map<String, UiVisibilityResponse.UiVisibilityItem> visibility;

    @Schema(description = "引擎评估耗时（毫秒）", example = "18")
    private long evalTimeMs;
}

