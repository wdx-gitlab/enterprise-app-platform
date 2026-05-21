package com.ruijie.authzengine.application.service;

import com.ruijie.authzengine.application.spi.AuthMetaModelAdapter;
import com.ruijie.authzengine.application.spi.DataItem;
import com.ruijie.authzengine.application.spi.ModelCode;
import com.ruijie.authzengine.application.spi.ModelFieldSchema;
import com.ruijie.authzengine.domain.model.common.SubjectKey;
import com.ruijie.authzengine.domain.model.governance.AuthMetaModelDefinition;
import com.ruijie.authzengine.domain.model.governance.PageResult;
import com.ruijie.authzengine.domain.model.governance.assignment.SysAuthAssignment;
import com.ruijie.authzengine.domain.model.governance.permission.AuthPermissionItem;
import com.ruijie.authzengine.domain.model.governance.resource.SysResComponent;
import com.ruijie.authzengine.domain.model.governance.resource.SysResMenu;
import com.ruijie.authzengine.domain.model.governance.resource.SysResPage;
import com.ruijie.authzengine.domain.model.governance.subject.AuthSubjectRelation;
import com.ruijie.authzengine.domain.repository.AssignmentRepository;
import com.ruijie.authzengine.domain.repository.DerivationPermissionRepository;
import com.ruijie.authzengine.domain.repository.MetaRepository;
import com.ruijie.authzengine.domain.repository.PermissionRepository;
import com.ruijie.authzengine.domain.repository.ResourceRepository;
import com.ruijie.authzengine.domain.repository.SubjectRepository;
import com.ruijie.authzengine.infrastructure.authz.AuthMetaResolverRouter;
import com.ruijie.authzengine.infrastructure.config.UserContextDerivationProperties;
import com.ruijie.authzengine.shared.exception.BusinessException;
import com.ruijie.authzengine.shared.exception.ErrorCode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 权限查询应用服务（Q 系列 Open API 的应用层实现）。
 *
 * <p>与 {@link AuthzDecisionAppService} 的职责区别：
 * <ul>
 *   <li>{@code AuthzDecisionAppService}：通过 PDP 三段式决策链路判断"此次操作能否执行"；</li>
 *   <li>{@code AuthzQueryAppService}：基于授权数据聚合查询回答"主体拥有哪些权限 / 能访问哪些资源"，
 *       不经过完整的策略执行链路（不含 PBAC/ABAC 策略过滤），适用于前端渲染初始化场景。</li>
 * </ul>
 *
 * <p><b>注意</b>：本服务的查询结果仅代表 RBAC 身份展开后的授权记录归并，
 * 不含策略模板的条件过滤（PBAC/ABAC），因此不能替代 {@code AuthzDecisionAppService.check()} 作为最终鉴权依据。
 * 实际操作执行前仍须通过 check 接口做精确鉴权。
 *
 * <p><b>主体关联说明</b>：主体展开统一使用引擎内置关系表 `authz_subject_relation`。
 * Subject Hook 仅补充主体属性，不再作为角色/组织/岗位/用户组关系来源。
 */
@Slf4j
@Service
public class AuthzQueryAppService {

    /** 默认菜单资源类型，Q4 合集接口未指定 resourceTypes 时的兜底值。 */
    private static final String DEFAULT_RESOURCE_TYPE = "RES_UI_MENU";

    /** 默认组件资源类型。 */
    private static final String DEFAULT_COMPONENT_RESOURCE_TYPE = "RES_UI_COMPONENT";

    /** 默认页面资源类型。 */
    private static final String DEFAULT_PAGE_RESOURCE_TYPE = "RES_UI_PAGE";

    private static final DerivationPermissionRepository NO_OP_DERIVATION_PERMISSION_REPOSITORY =
        new DerivationPermissionRepository() { };

    private final SubjectRepository subjectRepository;

    private final AssignmentRepository assignmentRepository;

    private final PermissionRepository permissionRepository;

    private final ResourceRepository resourceRepository;

    private final MetaRepository metaRepository;

    private final AuthMetaResolverRouter authMetaResolverRouter;

    private final DerivationPermissionRepository derivationPermissionRepository;

    private final UserContextDerivationProperties userContextDerivationProperties;

    @Autowired
    public AuthzQueryAppService(SubjectRepository subjectRepository,
                                AssignmentRepository assignmentRepository,
                                PermissionRepository permissionRepository,
                                ResourceRepository resourceRepository,
                                MetaRepository metaRepository,
                                AuthMetaResolverRouter authMetaResolverRouter,
                                ObjectProvider<DerivationPermissionRepository> derivationPermissionRepositoryProvider,
                                ObjectProvider<UserContextDerivationProperties> userContextDerivationPropertiesProvider) {
        this(subjectRepository,
            assignmentRepository,
            permissionRepository,
            resourceRepository,
            metaRepository,
            authMetaResolverRouter,
            derivationPermissionRepositoryProvider.getIfAvailable(() -> NO_OP_DERIVATION_PERMISSION_REPOSITORY),
            userContextDerivationPropertiesProvider.getIfAvailable(UserContextDerivationProperties::new));
    }

    public AuthzQueryAppService(SubjectRepository subjectRepository,
                                AssignmentRepository assignmentRepository,
                                PermissionRepository permissionRepository,
                                ResourceRepository resourceRepository,
                                MetaRepository metaRepository,
                                AuthMetaResolverRouter authMetaResolverRouter) {
        this(subjectRepository,
            assignmentRepository,
            permissionRepository,
            resourceRepository,
            metaRepository,
            authMetaResolverRouter,
            NO_OP_DERIVATION_PERMISSION_REPOSITORY,
            new UserContextDerivationProperties());
    }

    public AuthzQueryAppService(SubjectRepository subjectRepository,
                                AssignmentRepository assignmentRepository,
                                PermissionRepository permissionRepository,
                                ResourceRepository resourceRepository,
                                MetaRepository metaRepository,
                                AuthMetaResolverRouter authMetaResolverRouter,
                                DerivationPermissionRepository derivationPermissionRepository,
                                UserContextDerivationProperties userContextDerivationProperties) {
        this.subjectRepository = subjectRepository;
        this.assignmentRepository = assignmentRepository;
        this.permissionRepository = permissionRepository;
        this.resourceRepository = resourceRepository;
        this.metaRepository = metaRepository;
        this.authMetaResolverRouter = authMetaResolverRouter;
        this.derivationPermissionRepository = derivationPermissionRepository == null
            ? NO_OP_DERIVATION_PERMISSION_REPOSITORY : derivationPermissionRepository;
        this.userContextDerivationProperties = userContextDerivationProperties == null
            ? new UserContextDerivationProperties() : userContextDerivationProperties;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Q1：主体权限快照
    // ─────────────────────────────────────────────────────────────────────

    /**
     * 查询主体权限快照：返回指定主体经 RBAC 身份展开后拥有的全部权限项编码列表。
     *
     * <p>实现步骤：
     * <ol>
     *   <li>展开主体身份键集合（用户本身 + 所关联的角色/岗位/组织/用户组）；</li>
     *   <li>按身份键集合查询所有命中的授权记录（一次批量查询）；</li>
     *   <li>按 permItemId 批量反查权限项，收集 permCode 列表去重返回。</li>
     * </ol>
     *
     * @param tenantId     租户标识
     * @param appCode      应用标识
     * @param subjectId    主体标识（通常为用户 ID）
     * @param subjectModel 主体类型（默认 SUB_USER）
     * @return 权限项编码列表，无授权时返回空列表
     */
    public List<String> queryPermissionSnapshot(String tenantId, String appCode,
                                                String subjectId, String subjectModel) {
        log.debug("[权限查询] Q1 主体权限快照: tenantId={}, appCode={}, subjectId={}",
            tenantId, appCode, subjectId);
        // 步骤 1：展开主体身份集合
        List<SubjectKey> subjectKeys = expandSubjectKeys(tenantId, appCode, subjectId, subjectModel);
        // 步骤 2：按身份集合查询授权记录
        List<SysAuthAssignment> assignments = assignmentRepository.findAssignmentsBySubjectSet(
            tenantId, appCode, subjectKeys);
        if (assignments.isEmpty()) {
            return Collections.emptyList();
        }
        // 步骤 3：批量反查权限项，收集 permCode
        List<Long> permItemIds = assignments.stream()
            .map(SysAuthAssignment::getPermItemId)
            .distinct()
            .collect(Collectors.toList());
        List<AuthPermissionItem> permItems = permissionRepository.findPermissionItemsByIds(
            tenantId, appCode, permItemIds);
        List<String> permCodes = permItems.stream()
            .map(AuthPermissionItem::getPermCode)
            .distinct()
            .collect(Collectors.toList());
        log.debug("[权限查询] Q1 完成: subjectId={}, permCodes.size={}", subjectId, permCodes.size());
        return permCodes;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Q2：可访问资源列表
    // ─────────────────────────────────────────────────────────────────────

    /**
     * 查询主体对指定资源类型下有访问权限的全部资源编码列表。
     *
     * <p>实现步骤：
     * <ol>
     *   <li>展开主体身份键集合；</li>
     *   <li>按身份键集合查询所有命中的授权记录，收集 permItemId 集合；</li>
     *   <li>按 resourceType（resModelCode）查询该类型下全部权限项；</li>
     *   <li>取两者交集（主体有授权的权限项 ∩ 该类型下的权限项），提取 resId 作为资源编码。</li>
     * </ol>
     *
     * <p><b>模型级权限处理</b>：若权限项的 resId 为空（模型级权限），表示对该类型下所有实例均有权限，
     * 此时返回该类型下已注册的全部资源编码（需后续扩展，当前版本返回空字符串占位）。
     *
     * @param tenantId     租户标识
     * @param appCode      应用标识
     * @param subjectId    主体标识
     * @param subjectModel 主体类型
     * @param resourceType 资源类型，如 RES_UI_MENU、RES_API
     * @return 有权访问的资源编码列表
     */
    public List<String> queryAccessibleResources(String tenantId, String appCode,
                                                 String subjectId, String subjectModel,
                                                 String resourceType) {
        log.debug("[权限查询] Q2 可访问资源列表: tenantId={}, appCode={}, subjectId={}, resourceType={}",
            tenantId, appCode, subjectId, resourceType);
        // 步骤 1：展开主体身份集合，获取已授权的 permItemId 集合
        List<SubjectKey> subjectKeys = expandSubjectKeys(tenantId, appCode, subjectId, subjectModel);
        List<SysAuthAssignment> assignments = assignmentRepository.findAssignmentsBySubjectSet(
            tenantId, appCode, subjectKeys);
        if (assignments.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> authorizedPermItemIds = assignments.stream()
            .map(SysAuthAssignment::getPermItemId)
            .collect(Collectors.toSet());
        // 步骤 2：按资源类型查询该类型下全部权限项，取交集，提取 resId
        List<AuthPermissionItem> typePermItems = permissionRepository.findPermissionItemsByResModelCode(
            tenantId, appCode, resourceType);
        List<String> resourceCodes = typePermItems.stream()
            .filter(item -> authorizedPermItemIds.contains(item.getId()))
            .map(AuthPermissionItem::getResId)
            .filter(resId -> resId != null && !resId.trim().isEmpty() && !"__MODEL__".equals(resId))
            .distinct()
            .collect(Collectors.toList());
        log.debug("[权限查询] Q2 完成: subjectId={}, resourceType={}, resourceCodes.size={}",
            subjectId, resourceType, resourceCodes.size());
        return resourceCodes;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Q3：UI 元素可见性批量查询
    // ─────────────────────────────────────────────────────────────────────

    /**
     * 批量查询 UI 组件对当前主体的可见性状态（visible / disabled / readonly）。
     *
     * <p>判断规则：
     * <ul>
     *   <li>该 componentCode 在引擎内未注册权限项 → 不受权限控制，默认可见（{@code visible=true}）；</li>
     *   <li>该 componentCode 已注册权限项，且主体有授权记录 → 可见；</li>
     *   <li>该 componentCode 已注册权限项，但主体无授权记录 → 不可见（{@code visible=false}）；</li>
     *   <li>{@code disabled/readonly} 状态需通过完整策略执行（check 接口的 obligations）才能精确获取，
     *       当前版本统一返回 {@code false}，后续迭代可通过批量 check 扩展。</li>
     * </ul>
     *
     * @param tenantId       租户标识
     * @param appCode        应用标识
     * @param subjectId      主体标识
     * @param subjectModel   主体类型
     * @param componentCodes 要查询的 UI 组件编码列表（对应权限项 resId）
     * @return componentCode → 可见性状态 Map
     */
    public Map<String, Boolean> queryUiVisibility(String tenantId, String appCode,
                                                  String subjectId, String subjectModel,
                                                  List<String> componentCodes) {
        log.debug("[权限查询] Q3 UI 可见性: tenantId={}, appCode={}, subjectId={}, codes.size={}",
            tenantId, appCode, subjectId, componentCodes == null ? 0 : componentCodes.size());
        if (componentCodes == null || componentCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> authorizedPermItemIds = loadAuthorizedPermItemIds(tenantId, appCode, subjectId, subjectModel);
        Map<String, Boolean> result = resolveComponentVisibility(
            tenantId,
            appCode,
            authorizedPermItemIds,
            componentCodes
        );
        log.debug("[权限查询] Q3 完成: subjectId={}, codes.size={}", subjectId, componentCodes.size());
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Q4：用户权限上下文合集查询
    // ─────────────────────────────────────────────────────────────────────

    /**
     * 用户权限上下文合集查询（Q1 + Q2 + Q3 的聚合版本）。
     *
     * <p>适用于用户登录后系统初始化场景，调用方只需传入三个必要参数（tenantId、appCode、subjectId），
     * 引擎自动计算并返回以下内容：
     * <ul>
     *   <li>Q1：主体权限快照（所有权限项编码）；</li>
    *   <li>Q2：可访问菜单列表（{@code RES_UI_MENU} 类型下有权访问的菜单编码）及菜单树；</li>
     *   <li>Q3：所有已注册 UI 组件的可见性（{@code RES_UI_COMPONENT} 类型下全量组件的渲染状态）。</li>
     * </ul>
     *
     * <p>资源类型和组件列表由引擎自主决策，调用方无需感知应用内部的资源/组件定义。
     *
     * @param tenantId     租户标识
     * @param appCode      应用标识
     * @param subjectId    主体标识（通常为用户 ID）
     * @param subjectModel 主体类型，默认 SUB_USER
    * @return UserContextResult{permCodes, accessibleResources, menuTree, visibility}
     */
    public UserContextResult queryUserContext(String tenantId, String appCode,
                                             String subjectId, String subjectModel) {
        log.info("[权限查询] Q4 用户权限上下文: tenantId={}, appCode={}, subjectId={}",
            tenantId, appCode, subjectId);
        long start = System.currentTimeMillis();

        // ── 公共阶段：主体展开 + 授权记录一次加载 ──
        Set<Long> authorizedPermItemIds = loadAuthorizedPermItemIds(tenantId, appCode, subjectId, subjectModel);

        // 批量拉取所有涉及的权限项（Q1 复用）
        List<AuthPermissionItem> allPermItems = authorizedPermItemIds.isEmpty()
            ? Collections.emptyList()
            : permissionRepository.findPermissionItemsByIds(tenantId, appCode,
                new ArrayList<>(authorizedPermItemIds));

        // ── Q1：权限快照 ──
        List<String> permCodes = allPermItems.stream()
            .map(AuthPermissionItem::getPermCode)
            .filter(c -> c != null && !c.isEmpty())
            .distinct()
            .collect(Collectors.toList());

        // ── Q2：可访问菜单列表（优先走 RES_UI_PAGE 派生链路，兼容期按配置回退）──
        List<String> accessibleMenuCodes = resolveAccessibleMenuCodes(tenantId, appCode, authorizedPermItemIds);
        Map<String, List<String>> accessibleResources = new HashMap<>();
        accessibleResources.put(DEFAULT_RESOURCE_TYPE, accessibleMenuCodes);
        List<MenuTreeNodeResult> menuTree = buildMenuTree(
            loadMenusForUserContext(tenantId, appCode, accessibleMenuCodes),
            accessibleMenuCodes
        );

        // ── Q3：全量 UI 组件可见性（优先走派生链路，兼容期按配置回退）──
        Map<String, Boolean> visibility = resolveUserContextComponentVisibility(
            tenantId,
            appCode,
            authorizedPermItemIds
        );

        long evalTimeMs = System.currentTimeMillis() - start;
        log.info("[权限查询] Q4 完成: subjectId={}, permCodes.size={}, menus.size={}, menuTreeRoots.size={}, components.size={}, evalTimeMs={}",
            subjectId, permCodes.size(), accessibleMenuCodes.size(), menuTree.size(), visibility.size(), evalTimeMs);
        return new UserContextResult(permCodes, accessibleResources, menuTree, visibility, evalTimeMs);
    }

    /**
     * user-context 菜单树优先走 Shadow 批量查询；未声明 resolver 时回退到引擎内置资源仓储。
     */
    private List<SysResMenu> loadMenusForUserContext(String tenantId, String appCode, List<String> accessibleMenuCodes) {
        ShadowAdapterContext shadowContext = tryResolveShadow(tenantId, appCode, DEFAULT_RESOURCE_TYPE);
        if (shadowContext == null) {
            return resourceRepository.listMenus(tenantId, appCode);
        }
        if (accessibleMenuCodes == null || accessibleMenuCodes.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, SysResMenu> resolvedMenus = new LinkedHashMap<>();
        Set<String> pendingMenuCodes = new LinkedHashSet<>(accessibleMenuCodes);
        while (!pendingMenuCodes.isEmpty()) {
            List<String> currentBatch = new ArrayList<>(pendingMenuCodes);
            pendingMenuCodes.clear();
            List<DataItem> batchItems = shadowContext.adapter.batchResolveItems(ModelCode.RES_UI_MENU, currentBatch);
            if (batchItems == null) {
                throw new BusinessException(ErrorCode.ADAPTER_NOT_IMPLEMENTED,
                    "业务系统暂未完成此接口的适配开发工作, modelCode=RES_UI_MENU");
            }
            for (DataItem batchItem : batchItems) {
                SysResMenu menu = ShadowDataMapper.toModel(
                    batchItem,
                    SysResMenu.class,
                    shadowContext.schema,
                    tenantId,
                    appCode
                );
                resolvedMenus.putIfAbsent(menu.getMenuCode(), menu);
                if (StringUtils.hasText(menu.getParentMenuCode()) && !resolvedMenus.containsKey(menu.getParentMenuCode())) {
                    pendingMenuCodes.add(menu.getParentMenuCode());
                }
            }
        }
        return new ArrayList<>(resolvedMenus.values());
    }

    /**
     * 根据可访问菜单编码装配可直接渲染的树结构；若子菜单可访问，则补齐其祖先节点。
     */

    /**
     * 统一加载主体已授权的权限项主键集合，供 Q1/Q3/Q4 复用。
     */
    private Set<Long> loadAuthorizedPermItemIds(String tenantId, String appCode,
                                                String subjectId, String subjectModel) {
        List<SubjectKey> subjectKeys = expandSubjectKeys(tenantId, appCode, subjectId, subjectModel);
        List<SysAuthAssignment> assignments = assignmentRepository.findAssignmentsBySubjectSet(
            tenantId, appCode, subjectKeys);
        return assignments.stream()
            .map(SysAuthAssignment::getPermItemId)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Q2 菜单可见性：若存在页面派生绑定，则由页面目录反推菜单；否则按配置回退旧菜单权限项路径。
     */
    private List<String> resolveAccessibleMenuCodes(String tenantId, String appCode,
                                                    Set<Long> authorizedPermItemIds) {
        Set<String> accessiblePageCodes = new LinkedHashSet<>();
        if (derivationPermissionRepository.hasDerivationBindings(tenantId, appCode, DEFAULT_PAGE_RESOURCE_TYPE)) {
            accessiblePageCodes.addAll(sanitizeCodes(
                derivationPermissionRepository.findDerivedResourceCodesByPermItemIds(
                    tenantId,
                    appCode,
                    DEFAULT_PAGE_RESOURCE_TYPE,
                    authorizedPermItemIds
                )));
        }
        if (!accessiblePageCodes.isEmpty()) {
            return loadPagesForUserContext(tenantId, appCode, new ArrayList<>(accessiblePageCodes)).stream()
                .filter(page -> accessiblePageCodes.contains(page.getPageCode()))
                .map(SysResPage::getMenuCode)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
        }
        if (derivationPermissionRepository.hasDerivationBindings(tenantId, appCode, DEFAULT_PAGE_RESOURCE_TYPE)) {
            return Collections.emptyList();
        }
        if (userContextDerivationProperties.getMode() == UserContextDerivationProperties.LoadMode.COMPAT) {
            return resolveLegacyAccessibleMenuCodes(tenantId, appCode, authorizedPermItemIds);
        }
        if (userContextDerivationProperties.getMissingBindingStrategy()
            == UserContextDerivationProperties.MissingBindingStrategy.ALLOW) {
            return loadAllMenuCodes(tenantId, appCode);
        }
        return Collections.emptyList();
    }

    private List<String> resolveLegacyAccessibleMenuCodes(String tenantId, String appCode,
                                                          Set<Long> authorizedPermItemIds) {
        List<AuthPermissionItem> menuItems = permissionRepository.findPermissionItemsByResModelCode(
            tenantId, appCode, DEFAULT_RESOURCE_TYPE);
        return menuItems.stream()
            .filter(item -> authorizedPermItemIds.contains(item.getId()))
            .map(AuthPermissionItem::getResId)
            .filter(resId -> StringUtils.hasText(resId) && !"__MODEL__".equals(resId))
            .distinct()
            .collect(Collectors.toList());
    }

    /**
     * Q3 明细查询：优先走派生绑定；无派生记录时按配置决定回退或降级。
     */
    private Map<String, Boolean> resolveComponentVisibility(String tenantId, String appCode,
                                                            Set<Long> authorizedPermItemIds,
                                                            List<String> componentCodes) {
        List<String> sanitizedCodes = sanitizeCodes(componentCodes);
        if (sanitizedCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        if (derivationPermissionRepository.hasDerivationBindings(tenantId, appCode, DEFAULT_COMPONENT_RESOURCE_TYPE)) {
            Set<String> visibleCodes = new LinkedHashSet<>(sanitizeCodes(
                derivationPermissionRepository.findDerivedResourceCodesByPermItemIds(
                    tenantId,
                    appCode,
                    DEFAULT_COMPONENT_RESOURCE_TYPE,
                    authorizedPermItemIds
                )));
            return buildVisibilityMap(sanitizedCodes, visibleCodes);
        }
        if (userContextDerivationProperties.getMode() == UserContextDerivationProperties.LoadMode.COMPAT) {
            return resolveLegacyComponentVisibility(tenantId, appCode, authorizedPermItemIds, sanitizedCodes);
        }
        return applyMissingBindingStrategy(sanitizedCodes);
    }

    /**
     * Q4 合集查询：组件全集在派生模式下来自派生仓储；纯派生且未配置时按目录全集执行 ALLOW/DENY。
     */
    private Map<String, Boolean> resolveUserContextComponentVisibility(String tenantId, String appCode,
                                                                       Set<Long> authorizedPermItemIds) {
        if (derivationPermissionRepository.hasDerivationBindings(tenantId, appCode, DEFAULT_COMPONENT_RESOURCE_TYPE)) {
            List<String> allComponentCodes = sanitizeCodes(
                derivationPermissionRepository.findAllDerivedResourceCodes(
                    tenantId,
                    appCode,
                    DEFAULT_COMPONENT_RESOURCE_TYPE
                ));
            Set<String> visibleCodes = new LinkedHashSet<>(sanitizeCodes(
                derivationPermissionRepository.findDerivedResourceCodesByPermItemIds(
                    tenantId,
                    appCode,
                    DEFAULT_COMPONENT_RESOURCE_TYPE,
                    authorizedPermItemIds
                )));
            return buildVisibilityMap(allComponentCodes, visibleCodes);
        }
        if (userContextDerivationProperties.getMode() == UserContextDerivationProperties.LoadMode.COMPAT) {
            return resolveLegacyUserContextVisibility(tenantId, appCode, authorizedPermItemIds);
        }
        return applyMissingBindingStrategy(loadAllComponentCodes(tenantId, appCode));
    }

    private Map<String, Boolean> resolveLegacyComponentVisibility(String tenantId, String appCode,
                                                                  Set<Long> authorizedPermItemIds,
                                                                  List<String> componentCodes) {
        List<AuthPermissionItem> componentPermItems = permissionRepository
            .findPermissionItemsByResModelCode(tenantId, appCode, DEFAULT_COMPONENT_RESOURCE_TYPE);
        Map<String, Long> resIdToPermItemId = componentPermItems.stream()
            .filter(item -> StringUtils.hasText(item.getResId()))
            .collect(Collectors.toMap(
                AuthPermissionItem::getResId,
                AuthPermissionItem::getId,
                (left, right) -> left,
                LinkedHashMap::new
            ));
        Map<String, Boolean> result = new LinkedHashMap<>();
        for (String code : componentCodes) {
            Long permItemId = resIdToPermItemId.get(code);
            result.put(code, permItemId == null || authorizedPermItemIds.contains(permItemId));
        }
        return result;
    }

    private Map<String, Boolean> resolveLegacyUserContextVisibility(String tenantId, String appCode,
                                                                    Set<Long> authorizedPermItemIds) {
        List<AuthPermissionItem> componentItems = permissionRepository
            .findPermissionItemsByResModelCode(tenantId, appCode, DEFAULT_COMPONENT_RESOURCE_TYPE);
        Map<String, Boolean> visibility = new LinkedHashMap<>();
        for (AuthPermissionItem item : componentItems) {
            String resId = item.getResId();
            if (StringUtils.hasText(resId)) {
                visibility.put(resId, authorizedPermItemIds.contains(item.getId()));
            }
        }
        return visibility;
    }

    private List<String> loadAllMenuCodes(String tenantId, String appCode) {
        ShadowAdapterContext shadowContext = tryResolveShadow(tenantId, appCode, DEFAULT_RESOURCE_TYPE);
        if (shadowContext != null) {
            PageResult<DataItem> pageResult = shadowContext.adapter.pageItems(
                ModelCode.RES_UI_MENU,
                Collections.emptyMap(),
                1,
                Integer.MAX_VALUE
            );
            if (pageResult == null || pageResult.getRecords() == null) {
                log.warn("[权限查询] Shadow 菜单适配器未实现 pageItems，无法装配纯派生模式菜单全集. tenantId={} appCode={}",
                    tenantId, appCode);
                return Collections.emptyList();
            }
            return pageResult.getRecords().stream()
                .map(DataItem::getCode)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
        }
        return resourceRepository.listMenus(tenantId, appCode).stream()
            .map(SysResMenu::getMenuCode)
            .filter(StringUtils::hasText)
            .map(String::trim)
            .distinct()
            .collect(Collectors.toList());
    }

    private Map<String, Boolean> applyMissingBindingStrategy(List<String> componentCodes) {
        boolean visible = userContextDerivationProperties.getMissingBindingStrategy()
            == UserContextDerivationProperties.MissingBindingStrategy.ALLOW;
        Map<String, Boolean> visibility = new LinkedHashMap<>();
        for (String componentCode : sanitizeCodes(componentCodes)) {
            visibility.put(componentCode, visible);
        }
        return visibility;
    }

    private Map<String, Boolean> buildVisibilityMap(List<String> allCodes, Set<String> visibleCodes) {
        Map<String, Boolean> visibility = new LinkedHashMap<>();
        for (String code : sanitizeCodes(allCodes)) {
            visibility.put(code, visibleCodes.contains(code));
        }
        return visibility;
    }

    private List<String> loadAllComponentCodes(String tenantId, String appCode) {
        ShadowAdapterContext shadowContext = tryResolveShadow(tenantId, appCode, DEFAULT_COMPONENT_RESOURCE_TYPE);
        if (shadowContext != null) {
            return loadAllShadowComponentCodes(tenantId, appCode, shadowContext);
        }
        return resourceRepository.listComponents(tenantId, appCode).stream()
            .map(SysResComponent::getComponentCode)
            .filter(StringUtils::hasText)
            .map(String::trim)
            .distinct()
            .collect(Collectors.toList());
    }

    /**
     * 页面目录优先走 Shadow 批量查询；未声明 resolver 时回退到引擎内置页面仓储。
     */
    private List<SysResPage> loadPagesForUserContext(String tenantId, String appCode, List<String> pageCodes) {
        ShadowAdapterContext shadowContext = tryResolveShadow(tenantId, appCode, DEFAULT_PAGE_RESOURCE_TYPE);
        if (shadowContext == null) {
            return resourceRepository.listPages(tenantId, appCode);
        }
        if (pageCodes == null || pageCodes.isEmpty()) {
            return Collections.emptyList();
        }
        List<DataItem> batchItems = shadowContext.adapter.batchResolveItems(ModelCode.RES_UI_PAGE, pageCodes);
        if (batchItems == null) {
            throw new BusinessException(ErrorCode.ADAPTER_NOT_IMPLEMENTED,
                "业务系统暂未完成此接口的适配开发工作, modelCode=RES_UI_PAGE");
        }
        return batchItems.stream()
            .map(batchItem -> ShadowDataMapper.toModel(
                batchItem,
                SysResPage.class,
                shadowContext.schema,
                tenantId,
                appCode
            ))
            .collect(Collectors.toList());
    }

    private List<String> loadAllShadowComponentCodes(String tenantId, String appCode, ShadowAdapterContext shadowContext) {
        PageResult<DataItem> pageResult = shadowContext.adapter.pageItems(
            ModelCode.RES_UI_COMPONENT,
            Collections.emptyMap(),
            1,
            Integer.MAX_VALUE
        );
        if (pageResult == null || pageResult.getRecords() == null) {
            log.warn("[权限查询] Shadow 组件适配器未实现 pageItems，无法装配纯派生模式组件全集. tenantId={} appCode={}",
                tenantId, appCode);
            return Collections.emptyList();
        }
        return pageResult.getRecords().stream()
            .map(DataItem::getCode)
            .filter(StringUtils::hasText)
            .map(String::trim)
            .distinct()
            .collect(Collectors.toList());
    }

    private List<String> sanitizeCodes(List<String> rawCodes) {
        if (rawCodes == null || rawCodes.isEmpty()) {
            return Collections.emptyList();
        }
        return rawCodes.stream()
            .filter(StringUtils::hasText)
            .map(String::trim)
            .distinct()
            .collect(Collectors.toList());
    }

    private List<MenuTreeNodeResult> buildMenuTree(List<SysResMenu> allMenus, List<String> accessibleMenuCodes) {
        if (allMenus == null || allMenus.isEmpty() || accessibleMenuCodes == null || accessibleMenuCodes.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, SysResMenu> menuIndex = allMenus.stream()
            .filter(menu -> menu.getMenuCode() != null && !menu.getMenuCode().isEmpty())
            .collect(Collectors.toMap(
                SysResMenu::getMenuCode,
                menu -> menu,
                (left, right) -> left,
                LinkedHashMap::new
            ));
        Set<String> includedMenuCodes = collectIncludedMenuCodes(menuIndex, accessibleMenuCodes);
        Comparator<SysResMenu> menuComparator = Comparator
            .comparing((SysResMenu menu) -> menu.getSortNo() == null ? Integer.MAX_VALUE : menu.getSortNo())
            .thenComparing(SysResMenu::getMenuCode, Comparator.nullsLast(String::compareTo));
        return allMenus.stream()
            .filter(menu -> isRootMenu(menu, includedMenuCodes, menuIndex))
            .sorted(menuComparator)
            .map(menu -> toMenuTreeNode(menu, includedMenuCodes, menuIndex, menuComparator))
            .collect(Collectors.toList());
    }

    private Set<String> collectIncludedMenuCodes(Map<String, SysResMenu> menuIndex, List<String> accessibleMenuCodes) {
        Set<String> includedMenuCodes = new HashSet<>();
        for (String accessibleMenuCode : accessibleMenuCodes) {
            String current = accessibleMenuCode;
            while (current != null && !current.isEmpty() && includedMenuCodes.add(current)) {
                SysResMenu currentMenu = menuIndex.get(current);
                current = currentMenu == null ? null : currentMenu.getParentMenuCode();
            }
        }
        return includedMenuCodes;
    }

    private boolean isRootMenu(SysResMenu menu, Set<String> includedMenuCodes, Map<String, SysResMenu> menuIndex) {
        if (!includedMenuCodes.contains(menu.getMenuCode())) {
            return false;
        }
        String parentMenuCode = menu.getParentMenuCode();
        return parentMenuCode == null || parentMenuCode.isEmpty()
            || !includedMenuCodes.contains(parentMenuCode)
            || !menuIndex.containsKey(parentMenuCode);
    }

    private MenuTreeNodeResult toMenuTreeNode(
        SysResMenu currentMenu,
        Set<String> includedMenuCodes,
        Map<String, SysResMenu> menuIndex,
        Comparator<SysResMenu> menuComparator
    ) {
        List<MenuTreeNodeResult> children = menuIndex.values().stream()
            .filter(menu -> includedMenuCodes.contains(menu.getMenuCode()))
            .filter(menu -> currentMenu.getMenuCode().equals(menu.getParentMenuCode()))
            .sorted(menuComparator)
            .map(menu -> toMenuTreeNode(menu, includedMenuCodes, menuIndex, menuComparator))
            .collect(Collectors.toList());
        return new MenuTreeNodeResult(
            currentMenu.getMenuCode(),
            currentMenu.getMenuName(),
            currentMenu.getRoutePath(),
            children
        );
    }

    // ─────────────────────────────────────────────────────────────────────
    // 内部工具：主体身份展开
    // ─────────────────────────────────────────────────────────────────────

    /**
     * 展开主体身份键集合。
     *
     * <p><b>当前范围</b>：展开 {@code authz_subject_relation} 表中的直接关联关系。
    * 组织树的向上递归（父节点授权下沉）仍留待后续迭代补全。
     *
     * @param tenantId     租户标识
     * @param appCode      应用标识
     * @param subjectId    主体标识
     * @param subjectModel 主体类型
     * @return 展开后的主体身份键列表
     */
    private List<SubjectKey> expandSubjectKeys(String tenantId, String appCode,
                                               String subjectId, String subjectModel) {
        List<SubjectKey> keys = new ArrayList<>();
        Set<String> deduplicatedKeys = new LinkedHashSet<>();
        // 主体本身（通常是用户）
        addSubjectKey(keys, deduplicatedKeys, subjectModel, subjectId);
        // 仅 SUB_USER 类型才通过关系表展开间接身份
        if ("SUB_USER".equals(subjectModel)) {
            List<AuthSubjectRelation> relations = subjectRepository.findRelationsByUserId(
                tenantId, appCode, subjectId);
            for (AuthSubjectRelation rel : relations) {
                addSubjectKey(keys, deduplicatedKeys, rel.getRelatedSubjectModel(), rel.getRelatedSubjectId());
            }
            log.debug(
                "[权限查询] 主体展开完成: subjectId={}, relatedSize={}, identities.size={}",
                subjectId,
                relations.size(),
                keys.size()
            );
        }
        return keys;
    }

    /**
     * 追加主体键时做去重，避免 Native/Shadow 双来源重复写入同一身份。
     */
    private void addSubjectKey(List<SubjectKey> keys, Set<String> deduplicatedKeys,
                               String subjectModel, String subjectId) {
        if (!StringUtils.hasText(subjectModel) || !StringUtils.hasText(subjectId)) {
            return;
        }
        String normalizedSubjectModel = subjectModel.trim();
        String normalizedSubjectId = subjectId.trim();
        if (deduplicatedKeys.add(normalizedSubjectModel + ":" + normalizedSubjectId)) {
            keys.add(new SubjectKey(normalizedSubjectModel, normalizedSubjectId));
        }
    }

    /**
     * 尝试解析 Shadow 元数据上下文；resolver 无效时返回 null，沿用 Native 路径。
     */
    private ShadowAdapterContext tryResolveShadow(String tenantId, String appCode, String modelCode) {
        AuthMetaModelDefinition metaDefinition = metaRepository.findAuthMetaModel(tenantId, appCode, modelCode);
        if (metaDefinition == null) {
            return null;
        }
        String resolver = metaDefinition.getResolver();
        if (!StringUtils.hasText(resolver) || "noopHook".equalsIgnoreCase(resolver.trim())) {
            return null;
        }
        AuthMetaModelAdapter adapter = authMetaResolverRouter.resolve(metaDefinition.getAdapterType(), resolver);
        if (adapter == null) {
            log.warn("[权限查询] 未找到有效 Shadow 元数据适配器. tenantId={} appCode={} modelCode={} resolver={}",
                tenantId, appCode, modelCode, resolver);
            return null;
        }
        ModelFieldSchema schema = ShadowDataMapper.resolveSchema(modelCode, metaDefinition.getSchemaView());
        return new ShadowAdapterContext(adapter, schema);
    }

    // ─────────────────────────────────────────────────────────────────────
    // 内部返回值对象
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Q4 合集查询结果值对象。
     */
    public static class UserContextResult {

        /** Q1：权限项编码列表。 */
        public final List<String> permCodes;

        /** Q2：可访问资源，key=资源类型，value=资源编码列表。 */
        public final Map<String, List<String>> accessibleResources;

        /** Q2 衍生：可直接渲染的菜单树。 */
        public final List<MenuTreeNodeResult> menuTree;

        /** Q3：UI 组件可见性，key=componentCode，value=是否可见。 */
        public final Map<String, Boolean> visibility;

        /** 引擎评估耗时（毫秒）。 */
        public final long evalTimeMs;

        public UserContextResult(List<String> permCodes,
                                 Map<String, List<String>> accessibleResources,
                                 List<MenuTreeNodeResult> menuTree,
                                 Map<String, Boolean> visibility,
                                 long evalTimeMs) {
            this.permCodes = permCodes;
            this.accessibleResources = accessibleResources;
            this.menuTree = menuTree;
            this.visibility = visibility;
            this.evalTimeMs = evalTimeMs;
        }
    }

    /**
     * user-context 菜单树节点结果。
     */
    public static class MenuTreeNodeResult {

        public final String menuCode;

        public final String menuName;

        public final String routePath;

        public final List<MenuTreeNodeResult> children;

        public MenuTreeNodeResult(String menuCode, String menuName, String routePath, List<MenuTreeNodeResult> children) {
            this.menuCode = menuCode;
            this.menuName = menuName;
            this.routePath = routePath;
            this.children = children == null ? Collections.emptyList() : children;
        }
    }

    /**
     * Shadow 菜单元数据上下文。
     */
    private static final class ShadowAdapterContext {

        private final AuthMetaModelAdapter adapter;

        private final ModelFieldSchema schema;

        private ShadowAdapterContext(AuthMetaModelAdapter adapter, ModelFieldSchema schema) {
            this.adapter = adapter;
            this.schema = schema;
        }
    }
}


