package com.ruijie.authzengine.api.controller;

import com.ruijie.authzengine.api.dto.request.AccessibleResourceRequest;
import com.ruijie.authzengine.api.dto.response.MenuTreeNode;
import com.ruijie.authzengine.api.dto.request.SubjectContextQueryRequest;
import com.ruijie.authzengine.api.dto.request.UiVisibilityRequest;
import com.ruijie.authzengine.api.dto.response.AccessibleResourceResponse;
import com.ruijie.authzengine.api.dto.response.SubjectPermissionSnapshotResponse;
import com.ruijie.authzengine.api.dto.response.UiVisibilityResponse;
import com.ruijie.authzengine.api.dto.response.UiVisibilityResponse.UiVisibilityItem;
import com.ruijie.authzengine.api.dto.response.UserContextResponse;
import com.ruijie.authzengine.application.service.AuthzQueryAppService;
import com.ruijie.authzengine.application.service.AuthzQueryAppService.MenuTreeNodeResult;
import com.ruijie.authzengine.application.service.AuthzQueryAppService.UserContextResult;
import com.ruijie.authzengine.shared.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 权限查询 Open API（Q 系列）。
 *
 * <p>与 {@link AuthzDecisionController} 的语义区别：
 * <ul>
 *   <li>{@code AuthzDecisionController}：回答"此次操作 <b>能否执行</b>"（PERMIT/NOT_PERMIT/INDETERMINATE）</li>
 *   <li>{@code AuthzQueryController}：回答"我 <b>能做什么 / 能看到什么</b>"（权限快照、资源可见性）</li>
 * </ul>
 *
 * <p>调用方：各业务系统服务端（页面初始化）/ 前端（导航渲染）。
 *
 * <p><b>注意</b>：本系列接口仅执行 RBAC 身份展开 + 授权记录归并，
 * 不含 PBAC/ABAC 策略过滤，不能替代 {@code /authz-engine/api/v1/authz/check} 作为最终鉴权依据。
 * 实际操作执行前仍须通过 check 接口做精确鉴权。
 *
 * <h3>两种接入模式说明</h3>
 * <ul>
 *   <li><b>Native Mode</b>：主体关系来自引擎内置表，直接查询，无 Hook 调用。</li>
 *   <li><b>Shadow Mode</b>：当前版本基于引擎内置关系表，Shadow Mode 的 Hook 展开待后续迭代接入 PIP 补全。</li>
 * </ul>
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/authz-engine/api/v1/authz")
@Tag(name = "Authz Query", description = "权限查询 Open API — 主体权限快照、资源可见性（Q 系列）")
public class AuthzQueryController {

    private final AuthzQueryAppService authzQueryAppService;

    /**
     * Q1：主体权限快照。
     *
     * <p>返回指定主体经 RBAC 身份展开后拥有的全部权限项编码列表（含角色/岗位/组织间接授权）。
     * 不含 PBAC/ABAC 策略过滤，适用于"我的权限"展示页和调试排障。
     *
     * @param request 包含 tenantId、appCode、subjectId、subjectModel
     * @return 权限项编码列表及评估耗时
     */
    @PostMapping("/permissions/mine")
    @Operation(summary = "主体权限快照（Q1）",
        description = "返回指定主体经 RBAC 展开后拥有的全部权限项编码列表，不含策略过滤，适用于\"我的权限\"展示和排障")
    public ApiResponse<SubjectPermissionSnapshotResponse> myPermissions(
            @Valid @RequestBody SubjectContextQueryRequest request) {
        log.info("[Q1] 主体权限快照: tenantId={}, appCode={}, subjectId={}",
            request.getTenantId(), request.getAppCode(), request.getSubjectId());
        long start = System.currentTimeMillis();
        List<String> permCodes = authzQueryAppService.queryPermissionSnapshot(
            request.getTenantId(), request.getAppCode(),
            request.getSubjectId(), request.getSubjectModel());
        return ApiResponse.success(SubjectPermissionSnapshotResponse.builder()
            .permCodes(permCodes)
            .evalTimeMs(System.currentTimeMillis() - start)
            .build());
    }

    /**
     * Q2：可访问资源列表。
     *
     * <p>返回主体在指定资源类型下有访问权限的全部资源编码列表，供前端渲染导航菜单树。
     *
     * @param request 包含 tenantId、appCode、subjectId、subjectModel、resourceType
     * @return 资源编码列表及评估耗时
     */
    @PostMapping("/resources/accessible")
    @Operation(summary = "可访问资源列表（Q2）",
        description = "返回主体在指定资源类型下有权访问的资源编码列表，典型用途：前端导航菜单树初始化")
    public ApiResponse<AccessibleResourceResponse> accessibleResources(
            @Valid @RequestBody AccessibleResourceRequest request) {
        log.info("[Q2] 可访问资源列表: tenantId={}, appCode={}, subjectId={}, resourceType={}",
            request.getTenantId(), request.getAppCode(), request.getSubjectId(), request.getResourceType());
        long start = System.currentTimeMillis();
        List<String> resourceCodes = authzQueryAppService.queryAccessibleResources(
            request.getTenantId(), request.getAppCode(),
            request.getSubjectId(), request.getSubjectModel(),
            request.getResourceType());
        return ApiResponse.success(AccessibleResourceResponse.builder()
            .resourceType(request.getResourceType())
            .resourceCodes(resourceCodes)
            .evalTimeMs(System.currentTimeMillis() - start)
            .build());
    }

    /**
     * Q3：UI 元素可见性批量查询。
     *
     * <p>批量确定 UI 组件对当前主体的渲染状态（visible/disabled/readonly），
     * 适用于页面加载后一次性确定"审批""删除""导出"等按钮的渲染状态。
     *
     * <p><b>visible 判断规则</b>：
     * <ul>
     *   <li>该 componentCode 未在引擎注册权限项 → 不受权限控制，默认 visible=true；</li>
     *   <li>已注册权限项且主体有授权 → visible=true；</li>
     *   <li>已注册权限项但主体无授权 → visible=false。</li>
     * </ul>
     * <p><b>disabled/readonly</b> 当前统一返回 false，精确值需通过 check 接口的 obligations 获取。
     *
     * @param request 包含 tenantId、appCode、subjectId、subjectModel、componentCodes
     * @return 每个组件的可见性状态 Map 及评估耗时
     */
    @PostMapping("/resources/visibility")
    @Operation(summary = "UI 元素可见性批量查询（Q3）",
        description = "批量确定 UI 组件渲染状态（visible/disabled/readonly），适用于按钮/Tab 渲染初始化")
    public ApiResponse<UiVisibilityResponse> uiVisibility(
            @Valid @RequestBody UiVisibilityRequest request) {
        log.info("[Q3] UI 可见性查询: tenantId={}, appCode={}, subjectId={}, codes.size={}",
            request.getTenantId(), request.getAppCode(), request.getSubjectId(),
            request.getComponentCodes().size());
        long start = System.currentTimeMillis();
        Map<String, Boolean> visibleMap = authzQueryAppService.queryUiVisibility(
            request.getTenantId(), request.getAppCode(),
            request.getSubjectId(), request.getSubjectModel(),
            request.getComponentCodes());
        // 将 visible 布尔值包装为 UiVisibilityItem（disabled/readonly 当前均为 false）
        Map<String, UiVisibilityItem> visibility = new LinkedHashMap<>();
        visibleMap.forEach((code, visible) ->
            visibility.put(code, UiVisibilityItem.builder()
                .visible(visible)
                .disabled(false)
                .readonly(false)
                .build()));
        return ApiResponse.success(UiVisibilityResponse.builder()
            .visibility(visibility)
            .evalTimeMs(System.currentTimeMillis() - start)
            .build());
    }

    /**
     * Q4：用户权限上下文合集查询（Q1 + Q2 + Q3 聚合版本）。
     *
     * <p>适用于用户登录后系统初始化场景，调用方只需传入三个参数即可获取：
     * <ul>
     *   <li>该用户拥有的全部权限项编码（Q1）；</li>
     *   <li>该用户可访问的菜单列表（Q2，引擎自动查 RES_UI_MENU）；</li>
     *   <li>该应用所有已注册按钮/组件的可见性状态（Q3，引擎自动查 RES_UI_COMPONENT 全量）。</li>
     * </ul>
     *
     * <p><b>为什么是 GET 而不是 POST：</b>本接口是纯查询语义，无副作用，GET 语义更正确；
     * 参数均为简单标量，通过 Query String 传递即可，无需 RequestBody。
     *
     * <p><b>为什么 Q1/Q2/Q3 仍用 POST：</b>Q2/Q3 需要传入列表类型参数（resourceType 列表、
     * componentCodes 列表），URL Query String 表达复杂集合参数不友好，因此保留 POST + RequestBody。
     *
     * @param tenantId     租户标识（必须，多租户隔离，同一 subjectId 在不同租户权限完全不同）
     * @param appCode      应用标识（必须，同一租户下不同应用权限独立隔离）
     * @param subjectId    主体标识，即"用户唯一标识"
     * @param subjectModel 主体类型，默认 SUB_USER（可不传）
     * @return 权限快照 + 可访问菜单 + 全量 UI 组件可见性
     */
    @GetMapping("/user-context")
    @Operation(summary = "用户权限上下文合集查询（Q4）",
        description = "GET 接口，只需 tenantId+appCode+subjectId 三个参数，引擎自动返回权限快照+菜单+按钮可见性，适用于登录后初始化")
    public ApiResponse<UserContextResponse> userContext(
            @Parameter(description = "租户标识", required = true, example = "T001")
            @RequestParam("tenantId") @NotBlank(message = "tenantId 不能为空") String tenantId,
            @Parameter(description = "应用标识", required = true, example = "CRM")
            @RequestParam("appCode") @NotBlank(message = "appCode 不能为空") String appCode,
            @Parameter(description = "主体标识（用户唯一标识）", required = true, example = "U10001")
            @RequestParam("subjectId") @NotBlank(message = "subjectId 不能为空") String subjectId,
            @Parameter(description = "主体类型，默认 SUB_USER", example = "SUB_USER")
            @RequestParam(value = "subjectModel", defaultValue = "SUB_USER") String subjectModel) {
        log.info("[Q4] 用户权限上下文: tenantId={}, appCode={}, subjectId={}", tenantId, appCode, subjectId);
        UserContextResult result = authzQueryAppService.queryUserContext(
            tenantId, appCode, subjectId, subjectModel);
        // 将 visibility Boolean 包装为 UiVisibilityItem（disabled/readonly 当前均为 false）
        Map<String, UiVisibilityItem> visibility = Collections.emptyMap();
        if (result.visibility != null && !result.visibility.isEmpty()) {
            visibility = new LinkedHashMap<>();
            for (Map.Entry<String, Boolean> entry : result.visibility.entrySet()) {
                visibility.put(entry.getKey(), UiVisibilityItem.builder()
                    .visible(Boolean.TRUE.equals(entry.getValue()))
                    .disabled(false)
                    .readonly(false)
                    .build());
            }
        }
        return ApiResponse.success(UserContextResponse.builder()
            .subjectId(subjectId)
            .subjectModel(subjectModel)
            .permCodes(result.permCodes)
            .accessibleResources(result.accessibleResources)
            .menuTree(toMenuTree(result.menuTree))
            .visibility(visibility)
            .evalTimeMs(result.evalTimeMs)
            .build());
    }

    private List<MenuTreeNode> toMenuTree(List<MenuTreeNodeResult> menuTree) {
        if (menuTree == null || menuTree.isEmpty()) {
            return Collections.emptyList();
        }
        List<MenuTreeNode> nodes = new ArrayList<>();
        for (MenuTreeNodeResult node : menuTree) {
            nodes.add(toMenuTreeNode(node));
        }
        return nodes;
    }

    private MenuTreeNode toMenuTreeNode(MenuTreeNodeResult node) {
        return MenuTreeNode.builder()
            .menuCode(node.menuCode)
            .menuName(node.menuName)
            .routePath(node.routePath)
            .children(toMenuTree(node.children))
            .build();
    }
}

