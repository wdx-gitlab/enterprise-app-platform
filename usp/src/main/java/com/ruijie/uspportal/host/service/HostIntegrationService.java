package com.ruijie.uspportal.host.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruijie.uspportal.appregistry.entity.AppRegistryEntity;
import com.ruijie.uspportal.appregistry.mapper.AppRegistryMapper;
import com.ruijie.uspportal.auth.dto.CurrentUserResponse;
import com.ruijie.uspportal.auth.entity.LoginConfigEntity;
import com.ruijie.uspportal.auth.service.LoginConfigResolver;
import com.ruijie.uspportal.common.BusinessException;
import com.ruijie.uspportal.context.AppRuntimeContext;
import com.ruijie.uspportal.context.CurrentUserSnapshot;
import com.ruijie.uspportal.context.OrgSnapshot;
import com.ruijie.uspportal.context.PortalRuntimeContext;
import com.ruijie.uspportal.context.TenantSnapshot;
import com.ruijie.uspportal.context.USPRequestContext;
import com.ruijie.uspportal.context.USPRequestContextAssembler;
import com.ruijie.uspportal.context.USPRequestContextHolder;
import com.ruijie.uspportal.host.dto.AppCatalogItemResponse;
import com.ruijie.uspportal.host.dto.AppContextResponse;
import com.ruijie.uspportal.host.dto.CurrentContextResponse;
import com.ruijie.uspportal.host.dto.FeatureFlagEvaluateBatchRequest;
import com.ruijie.uspportal.host.dto.FeatureFlagEvaluateBatchResponse;
import com.ruijie.uspportal.host.dto.HostPortalParamResponse;
import com.ruijie.uspportal.host.dto.IntegrationCapabilitiesResponse;
import com.ruijie.uspportal.host.dto.NavigationResolveTargetRequest;
import com.ruijie.uspportal.host.dto.NavigationResolveTargetResponse;
import com.ruijie.uspportal.integration.psp.PspUiVisibilityClient;
import com.ruijie.uspportal.navigation.dto.NavigationNode;
import com.ruijie.uspportal.navigation.entity.MenuItemEntity;
import com.ruijie.uspportal.navigation.mapper.MenuItemMapper;
import com.ruijie.uspportal.portalconfig.entity.FeatureFlagEntity;
import com.ruijie.uspportal.portalconfig.entity.FeatureFlagRuleEntity;
import com.ruijie.uspportal.portalconfig.entity.PortalParamEntity;
import com.ruijie.uspportal.portalconfig.mapper.FeatureFlagMapper;
import com.ruijie.uspportal.portalconfig.mapper.FeatureFlagRuleMapper;
import com.ruijie.uspportal.portalconfig.mapper.PortalConfigMapper;
import com.ruijie.uspportal.security.CurrentUserContext;
import com.ruijie.uspportal.tenant.entity.TenantEntity;
import com.ruijie.uspportal.tenant.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 宿主集成服务。
 *
 * <p>聚合导航、应用目录、门户参数、灰度开关与运行时上下文等能力，对外提供统一宿主集成入口。</p>
 */
@Service
public class HostIntegrationService {

    private final MenuItemMapper menuItemMapper;

    private final AppRegistryMapper appRegistryMapper;

    private final PortalConfigMapper portalConfigMapper;

    private final FeatureFlagMapper featureFlagMapper;

    private final FeatureFlagRuleMapper featureFlagRuleMapper;

    private final TenantRepository tenantRepository;

    private final PspUiVisibilityClient pspUiVisibilityClient;

    private final LoginConfigResolver loginConfigResolver;

    private final USPRequestContextAssembler uspRequestContextAssembler;

    @Autowired
    public HostIntegrationService(MenuItemMapper menuItemMapper,
                                  AppRegistryMapper appRegistryMapper,
                                  PortalConfigMapper portalConfigMapper,
                                  FeatureFlagMapper featureFlagMapper,
                                  FeatureFlagRuleMapper featureFlagRuleMapper,
                                  TenantRepository tenantRepository,
                                  PspUiVisibilityClient pspUiVisibilityClient,
                                  LoginConfigResolver loginConfigResolver,
                                  USPRequestContextAssembler uspRequestContextAssembler) {
        this.menuItemMapper = menuItemMapper;
        this.appRegistryMapper = appRegistryMapper;
        this.portalConfigMapper = portalConfigMapper;
        this.featureFlagMapper = featureFlagMapper;
        this.featureFlagRuleMapper = featureFlagRuleMapper;
        this.tenantRepository = tenantRepository;
        this.pspUiVisibilityClient = pspUiVisibilityClient;
        this.loginConfigResolver = loginConfigResolver;
        this.uspRequestContextAssembler = uspRequestContextAssembler;
    }

    @Value("${usp.portal.default-tenant-code:_PLATFORM_}")
    private String defaultTenantCode;

    @Value("${usp.portal.home-route:/usp-overview}")
    private String defaultHomeRoute;

    @Value("${usp.portal.module-version:1.0-SNAPSHOT}")
    private String moduleVersion;

    /**
     * 查询当前宿主用户可见的导航树。
     *
     * @param tenantCode 租户编码
     * @param appCode 应用编码
     * @return 导航树节点列表
     */
    public List<NavigationNode> navigationTree(String tenantCode, String appCode) {
        return buildTree(filterVisibleMenus(resolveTenantCode(tenantCode), appCode));
    }

    /**
     * 解析菜单目标地址。
     *
     * @param request 目标解析请求
     * @return 导航目标解析结果
     */
    public NavigationResolveTargetResponse resolveTarget(NavigationResolveTargetRequest request) {
        MenuItemEntity menu = findMenuByReference(request.getMenuId(), request.getTenantCode(), request.getAppCode());
        String targetPath = menu.resolvedTargetPath();
        String targetType = menu.resolvedTargetType();
        return NavigationResolveTargetResponse.builder()
                .targetType(targetType)
                .targetPath(targetPath)
            .openMode(menu.resolvedOpenMode())
                .appCode(menu.getAppCode())
                .menuId(String.valueOf(menu.getId()))
                .routeMeta(NavigationResolveTargetResponse.RouteMeta.builder()
                        .title(menu.getMenuName())
                        .keepAlive(!"EXTERNAL_URL".equals(targetType))
                        .build())
                .build();
    }

    /**
        * 查询宿主可消费的应用目录。
        *
        * @param tenantCode 租户编码
        * @return 应用目录项列表
     */
    public List<AppCatalogItemResponse> appCatalog(String tenantCode) {
        QueryWrapper<AppRegistryEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("is_deleted", 0)
                .eq("publish_status", "PUBLISHED")
                .orderByAsc("app_name");
        return appRegistryMapper.selectList(wrapper).stream()
                .map(this::toCatalogItem)
                .collect(Collectors.toList());
    }

    /**
        * 查询门户参数列表。
        *
        * @param group 参数分组
        * @param scope 参数读取范围
        * @return 门户参数响应列表
     */
    public List<HostPortalParamResponse> listPortalParams(String group, String scope) {
        QueryWrapper<PortalParamEntity> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(group)) {
            wrapper.eq("param_group", group.trim());
        }
        wrapper.orderByAsc("param_group", "param_key");
        return portalConfigMapper.selectList(wrapper).stream()
                .map(entity -> HostPortalParamResponse.builder()
                        .id(entity.getId())
                        .paramKey(entity.getParamKey())
                        .paramName(entity.getParamName())
                        .paramValue(entity.getParamValue())
                        .paramGroup(entity.getParamGroup())
                        .valueType(entity.getValueType())
                        .frontendReadable(!"backend".equalsIgnoreCase(scope))
                        .defaultValue(entity.getDefaultValue())
                        .status(entity.getStatus())
                        .description(entity.getDescription())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 批量评估功能开关。
     *
     * @param request 批量评估请求
     * @return 功能开关批量评估结果
     */
    public FeatureFlagEvaluateBatchResponse evaluateFlags(FeatureFlagEvaluateBatchRequest request) {
        if (request.getFlagKeys() == null || request.getFlagKeys().isEmpty()) {
            throw new BusinessException(422, "flagKeys 不能为空");
        }
        Map<String, String> context = new LinkedHashMap<>();
        context.put("tenantcode", request.getTenantCode());
        context.put("orgcode", request.getOrgCode());
        context.put("userid", request.getUserId());
        context.put("appcode", request.getAppCode());
        List<FeatureFlagEvaluateBatchResponse.FlagResult> results = new ArrayList<>();
        for (String flagKey : request.getFlagKeys()) {
            FeatureFlagEntity flag = findFlag(flagKey);
            if (flag == null) {
                results.add(FeatureFlagEvaluateBatchResponse.FlagResult.builder()
                        .flagKey(flagKey)
                        .enabled(false)
                        .matchedRule("flag_not_found")
                    .reason("未找到开关定义")
                        .build());
                continue;
            }
            if (!"ENABLED".equalsIgnoreCase(flag.getStatus())) {
                results.add(FeatureFlagEvaluateBatchResponse.FlagResult.builder()
                        .flagKey(flagKey)
                        .enabled(false)
                        .matchedRule("default_off")
                        .reason("功能开关当前未启用")
                        .build());
                continue;
            }
            List<FeatureFlagRuleEntity> rules = listRules(flag.getId());
            FeatureFlagRuleEntity matchedRule = matchRule(rules, context);
            results.add(FeatureFlagEvaluateBatchResponse.FlagResult.builder()
                    .flagKey(flagKey)
                    .enabled(matchedRule != null || rules.isEmpty())
                    .matchedRule(matchedRule == null ? (rules.isEmpty() ? "global_on" : "default_off") : matchedRule.getRuleType())
                        .reason(matchedRule == null
                            ? (rules.isEmpty() ? "全局开启" : "未命中任何规则")
                            : matchedRule.getRuleType() + "=" + matchedRule.getRuleValue() + " 命中灰度名单")
                    .build());
        }
        return FeatureFlagEvaluateBatchResponse.builder()
                .tenantCode(request.getTenantCode())
                .userId(request.getUserId())
                .results(results)
                .build();
    }

    /**
        * 构建指定应用的上下文快照。
        *
        * @param appCode 应用编码
        * @param tenantCode 租户编码
        * @param unionSessionTicket 宿主透传的联合会话票据
        * @return 应用上下文响应
     */
    public AppContextResponse appContext(String appCode, String tenantCode, String unionSessionTicket) {
        CurrentUserContext.CurrentUser currentUser = requireCurrentUser();
        AppRegistryEntity app = findAppByCode(appCode);
        if (!"PUBLISHED".equalsIgnoreCase(app.getPublishStatus())) {
            throw new BusinessException(403, "应用当前不可访问");
        }
        return uspRequestContextAssembler.toAppContextResponse(buildRequestContext(currentUser, tenantCode, app, unionSessionTicket));
    }

    /**
     * 查询当前请求上下文快照。
     *
     * @return 当前上下文响应
     */
    public CurrentContextResponse currentContext() {
        return uspRequestContextAssembler.toCurrentContextResponse(buildRequestContext(requireCurrentUser(), null, null, null));
    }

    /**
     * 查询当前模块对宿主开放的能力说明。
     *
     * @return 集成能力说明
     */
    public IntegrationCapabilitiesResponse capabilities() {
        LoginConfigEntity config = currentLoginConfig();
        List<String> authModes = new ArrayList<>();
        if (Boolean.TRUE.equals(config.getSsoLoginEnabled())) {
            authModes.add("SID");
        }
        if (Boolean.TRUE.equals(config.getInternalLoginEnabled())) {
            authModes.add("USP");
        }
        return IntegrationCapabilitiesResponse.builder()
                .module("usp-portal")
                .version(moduleVersion)
                .authModes(authModes)
                .supportsFeatureFlags(true)
                .supportsNavigationTree(true)
                .supportsAppCatalog(true)
                .supportsContextSnapshot(true)
                .supportsEventReplay(false)
                .build();
    }

    /**
        * 过滤当前用户有权访问的菜单列表。
        *
        * @param tenantCode 租户编码
        * @param appCode 应用编码
        * @return 过滤后的菜单列表
     */
    private List<MenuItemEntity> filterVisibleMenus(String tenantCode, String appCode) {
        QueryWrapper<MenuItemEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("is_deleted", 0)
                .eq("tenant_code", tenantCode)
                .eq("publish_status", "PUBLISHED")
                .eq("status", "ENABLED")
                .orderByAsc("sort_no");
        if (StringUtils.hasText(appCode)) {
            wrapper.eq("app_code", appCode.trim());
        }
        List<MenuItemEntity> entities = menuItemMapper.selectList(wrapper);
        Map<String, Boolean> visibility = pspUiVisibilityClient.batchCheck(entities.stream()
                .map(MenuItemEntity::getPermissionCode)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList()));
        return entities.stream()
                .filter(item -> !StringUtils.hasText(item.getPermissionCode()) || Boolean.TRUE.equals(visibility.get(item.getPermissionCode())))
                .collect(Collectors.toList());
    }

    /**
        * 将菜单实体列表构造成树形导航结构。
        *
        * @param entities 菜单实体列表
        * @return 导航树节点列表
     */
    private List<NavigationNode> buildTree(List<MenuItemEntity> entities) {
        Map<Long, NavigationNode> nodeMap = new LinkedHashMap<>();
        for (MenuItemEntity entity : entities) {
            nodeMap.put(entity.getId(), NavigationNode.from(entity));
        }
        List<NavigationNode> roots = new ArrayList<>();
        entities.sort(Comparator.comparing(item -> item.getSortNo() == null ? 0 : item.getSortNo()));
        for (MenuItemEntity entity : entities) {
            NavigationNode current = nodeMap.get(entity.getId());
            if (entity.getParentId() == null || !nodeMap.containsKey(entity.getParentId())) {
                roots.add(current);
            } else {
                nodeMap.get(entity.getParentId()).getChildren().add(current);
            }
        }
        return roots;
    }

    /**
     * 按菜单主键或菜单编码解析菜单实体。
     *
     * @param menuReference 菜单主键或编码
     * @param tenantCode 租户编码
     * @param appCode 应用编码
     * @return 菜单实体
     */
    private MenuItemEntity findMenuByReference(String menuReference, String tenantCode, String appCode) {
        if (!StringUtils.hasText(menuReference)) {
            throw new BusinessException(422, "menuId 不能为空");
        }
        MenuItemEntity menu = null;
        if (menuReference.chars().allMatch(Character::isDigit)) {
            menu = menuItemMapper.selectById(Long.valueOf(menuReference));
        }
        if (menu == null) {
            QueryWrapper<MenuItemEntity> wrapper = new QueryWrapper<>();
            wrapper.eq("menu_code", menuReference.trim())
                    .eq("tenant_code", resolveTenantCode(tenantCode))
                    .eq("is_deleted", 0)
                    .last("LIMIT 1");
            if (StringUtils.hasText(appCode)) {
                wrapper.eq("app_code", appCode.trim());
            }
            menu = menuItemMapper.selectOne(wrapper);
        }
        if (menu == null || Objects.equals(menu.getDeleted(), 1)) {
            throw new BusinessException(404, "菜单不存在");
        }
        return menu;
    }

    /**
     * 按应用编码查询应用实体。
     *
     * @param appCode 应用编码
     * @return 应用实体
     */
    private AppRegistryEntity findAppByCode(String appCode) {
        QueryWrapper<AppRegistryEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("app_code", appCode)
                .eq("is_deleted", 0)
                .last("LIMIT 1");
        AppRegistryEntity app = appRegistryMapper.selectOne(wrapper);
        if (app == null) {
            throw new BusinessException(404, "应用不存在");
        }
        return app;
    }

    /**
     * 按功能开关键查询功能开关实体。
     *
     * @param flagKey 功能开关键
     * @return 功能开关实体
     */
    private FeatureFlagEntity findFlag(String flagKey) {
        QueryWrapper<FeatureFlagEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("flag_key", flagKey).last("LIMIT 1");
        return featureFlagMapper.selectOne(wrapper);
    }

    /**
     * 查询指定功能开关下的规则列表。
     *
     * @param flagId 功能开关主键
     * @return 规则列表
     */
    private List<FeatureFlagRuleEntity> listRules(Long flagId) {
        QueryWrapper<FeatureFlagRuleEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("flag_id", flagId).orderByAsc("priority_no");
        return featureFlagRuleMapper.selectList(wrapper);
    }

    /**
     * 按上下文匹配首个命中的灰度规则。
     *
     * @param rules 规则列表
     * @param context 评估上下文
     * @return 命中的规则，未命中则返回 {@code null}
     */
    private FeatureFlagRuleEntity matchRule(List<FeatureFlagRuleEntity> rules, Map<String, String> context) {
        for (FeatureFlagRuleEntity rule : rules) {
            String value = context.get(rule.getRuleType().toLowerCase());
            if (!StringUtils.hasText(value)) {
                continue;
            }
            if ("EQ".equalsIgnoreCase(rule.getRuleOperator()) && value.equalsIgnoreCase(rule.getRuleValue())) {
                return rule;
            }
            if ("IN".equalsIgnoreCase(rule.getRuleOperator())) {
                List<String> values = Arrays.stream(rule.getRuleValue().split(","))
                        .map(String::trim)
                        .filter(StringUtils::hasText)
                        .collect(Collectors.toList());
                if (values.stream().anyMatch(item -> item.equalsIgnoreCase(value))) {
                    return rule;
                }
            }
        }
        return null;
    }

    /**
     * 将应用实体转换为目录项响应。
     *
     * @param entity 应用实体
     * @return 应用目录项响应
     */
    private AppCatalogItemResponse toCatalogItem(AppRegistryEntity entity) {
        return AppCatalogItemResponse.builder()
                .appCode(entity.getAppCode())
                .appName(entity.getAppName())
                .icon(entity.getAppIcon())
                .entryUrl(entity.getEntryUrl())
                .routePrefix(entity.getRoutePrefix())
                .appType(entity.getAppType())
                .openMode(resolveOpenMode(entity))
                .visible(true)
                .publishStatus(entity.getPublishStatus())
                .build();
    }

    /**
     * 解析应用打开方式。
     *
     * @param entity 应用实体
     * @return 打开方式
     */
    private String resolveOpenMode(AppRegistryEntity entity) {
        return "EXTERNAL".equalsIgnoreCase(entity.getAppType()) ? "NEW_TAB" : "CURRENT_TAB";
    }

    /**
     * 解析当前请求应使用的租户编码。
     *
     * @param requestedTenantCode 请求传入的租户编码
     * @return 生效的租户编码
     */
    private String resolveTenantCode(String requestedTenantCode) {
        if (StringUtils.hasText(requestedTenantCode)) {
            return requestedTenantCode.trim();
        }
        CurrentUserContext.CurrentUser currentUser = CurrentUserContext.get();
        if (currentUser != null && StringUtils.hasText(currentUser.getTenantCode())) {
            return currentUser.getTenantCode();
        }
        return defaultTenantCode;
    }

    /**
     * 读取并校验当前登录用户。
     *
     * @return 当前登录用户
     */
    private CurrentUserContext.CurrentUser requireCurrentUser() {
        CurrentUserContext.CurrentUser currentUser = CurrentUserContext.get();
        if (currentUser == null) {
            throw new BusinessException(428, "当前未登录");
        }
        return currentUser;
    }

    /**
     * 构建统一请求上下文。
     *
     * @param currentUser 当前登录用户
     * @param requestedTenantCode 请求中的租户编码
     * @param currentApp 当前应用实体
     * @param unionSessionTicket 宿主透传票据
     * @return 统一请求上下文
     */
    private USPRequestContext buildRequestContext(CurrentUserContext.CurrentUser currentUser,
                                                  String requestedTenantCode,
                                                  AppRegistryEntity currentApp,
                                                  String unionSessionTicket) {
        String effectiveTenantCode = resolveTenantCode(requestedTenantCode);
        TenantEntity tenant = tenantRepository.findByCode(effectiveTenantCode);
        AppRegistryEntity defaultApp = findDefaultApp();
        USPRequestContext existing = USPRequestContextHolder.get();
        return (existing == null ? USPRequestContext.builder() : existing.toBuilder())
                .user(buildCurrentUserSnapshot(currentUser))
                .tenant(buildTenantSnapshot(currentUser, tenant, effectiveTenantCode))
                .org(OrgSnapshot.builder()
                        .orgCode(null)
                        .orgName(null)
                        .build())
                .portal(PortalRuntimeContext.builder()
                        .homeRoute(resolvePortalParam("portal.defaultHome", defaultHomeRoute))
                        .defaultAppCode(defaultApp == null ? null : defaultApp.getAppCode())
                        .menuVersion(resolveMenuVersion())
                        .build())
                .app(buildAppRuntimeContext(currentApp, unionSessionTicket))
                .build();
    }

    /**
        * 构建当前用户快照。
        *
        * @param currentUser 当前登录用户
        * @return 当前用户快照
     */
    private CurrentUserSnapshot buildCurrentUserSnapshot(CurrentUserContext.CurrentUser currentUser) {
        return CurrentUserSnapshot.builder()
                .userId(currentUser.getUserId())
                .loginName(currentUser.getLoginName())
                .displayName(currentUser.getDisplayName())
                .tenantCode(currentUser.getTenantCode())
                .sessionId(currentUser.getSessionId())
                .authMode(currentUser.getAuthMode())
                .admin(currentUser.getAdmin())
                .build();
    }

    /**
     * 构建租户快照。
     *
     * @param currentUser 当前登录用户
     * @param tenant 租户实体
     * @param effectiveTenantCode 生效租户编码
     * @return 租户快照
     */
    private TenantSnapshot buildTenantSnapshot(CurrentUserContext.CurrentUser currentUser,
                                               TenantEntity tenant,
                                               String effectiveTenantCode) {
        return TenantSnapshot.builder()
                .tenantId(tenant == null ? null : tenant.getId())
                .tenantCode(tenant == null ? fallbackTenantCode(currentUser, effectiveTenantCode) : tenant.getTenantCode())
                .tenantName(tenant == null ? fallbackTenantCode(currentUser, effectiveTenantCode) : tenant.getTenantName())
                .tenantStatus(tenant == null ? "ACTIVE" : tenant.getStatus())
                .build();
    }

    /**
        * 构建应用运行时上下文。
        *
        * @param app 应用实体
        * @param unionSessionTicket 宿主透传票据
        * @return 应用运行时上下文
     */
    private AppRuntimeContext buildAppRuntimeContext(AppRegistryEntity app, String unionSessionTicket) {
        if (app == null) {
            return null;
        }
        return AppRuntimeContext.builder()
                .appId(app.getId())
                .appCode(app.getAppCode())
                .appName(app.getAppName())
                .entryUrl(app.getEntryUrl())
                .routePrefix(app.getRoutePrefix())
                .accessMode("ALLOWED")
                .openMode(resolveOpenMode(app))
                .contextHeaders(StringUtils.hasText(unionSessionTicket)
                        ? Collections.singletonMap("union_session_ticket", unionSessionTicket)
                        : Collections.emptyMap())
                .build();
    }

    /**
        * 为缺失租户信息的场景兜底租户编码。
        *
        * @param currentUser 当前登录用户
        * @param effectiveTenantCode 已解析的租户编码
        * @return 兜底后的租户编码
     */
    private String fallbackTenantCode(CurrentUserContext.CurrentUser currentUser, String effectiveTenantCode) {
        if (StringUtils.hasText(effectiveTenantCode)) {
            return effectiveTenantCode;
        }
        return currentUser == null ? defaultTenantCode : currentUser.getTenantCode();
    }

    /**
     * 查询门户参数值，不存在时返回默认值。
     *
     * @param key 参数键
     * @param fallback 默认值
     * @return 实际参数值
     */
    private String resolvePortalParam(String key, String fallback) {
        QueryWrapper<PortalParamEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("param_key", key).last("LIMIT 1");
        PortalParamEntity entity = portalConfigMapper.selectOne(wrapper);
        return entity == null || !StringUtils.hasText(entity.getParamValue()) ? fallback : entity.getParamValue();
    }

    /**
     * 查询默认应用编码。
     *
     * @return 默认应用编码
     */
    private String resolveDefaultAppCode() {
        AppRegistryEntity entity = findDefaultApp();
        return entity == null ? null : entity.getAppCode();
    }

    /**
     * 查询默认应用实体。
     *
     * @return 默认应用实体
     */
    private AppRegistryEntity findDefaultApp() {
        QueryWrapper<AppRegistryEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("is_deleted", 0)
                .eq("publish_status", "PUBLISHED")
                .orderByAsc("app_name")
                .last("LIMIT 1");
        return appRegistryMapper.selectOne(wrapper);
    }

    /**
     * 计算菜单版本号。
     *
     * @return 以日期格式表示的菜单版本号
     */
    private String resolveMenuVersion() {
        QueryWrapper<MenuItemEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("is_deleted", 0)
                .orderByDesc("updated_time")
                .last("LIMIT 1");
        MenuItemEntity entity = menuItemMapper.selectOne(wrapper);
        LocalDateTime time = entity == null ? LocalDateTime.now() : (entity.getUpdatedTime() == null ? entity.getCreatedTime() : entity.getUpdatedTime());
        if (time == null) {
            time = LocalDateTime.now();
        }
        return time.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
    }

    /**
     * 查询当前生效的登录配置。
     *
     * @return 登录配置实体
     */
    private LoginConfigEntity currentLoginConfig() {
        return loginConfigResolver.getEnabledLoginConfig();
    }

    /**
     * 递归统计导航节点数量。
     *
     * @param nodes 导航节点列表
     * @return 菜单数量
     */
    private int countMenus(List<NavigationNode> nodes) {
        int total = 0;
        for (NavigationNode node : nodes) {
            total += 1 + countMenus(node.getChildren());
        }
        return total;
    }
}
