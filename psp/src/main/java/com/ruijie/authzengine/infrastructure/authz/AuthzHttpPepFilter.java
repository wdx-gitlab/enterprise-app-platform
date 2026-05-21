package com.ruijie.authzengine.infrastructure.authz;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruijie.authzengine.application.service.AuthzFacade;
import com.ruijie.authzengine.domain.model.decision.AuthzDecision;
import com.ruijie.authzengine.domain.model.decision.DecisionType;
import com.ruijie.authzengine.domain.model.governance.permission.AuthPermissionItem;
import com.ruijie.authzengine.domain.model.governance.permission.ResourceDerivationPermission;
import com.ruijie.authzengine.domain.model.governance.resource.SysResApi;
import com.ruijie.authzengine.domain.repository.DerivationPermissionRepository;
import com.ruijie.authzengine.domain.repository.PermissionRepository;
import com.ruijie.authzengine.domain.repository.ResourceRepository;
import com.ruijie.authzengine.domain.spi.AuthzSubjectProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * HTTP 全量拦截 PEP（策略执行点）过滤器。
 *
 * <p>继承 {@link OncePerRequestFilter}，对所有进入宿主应用的 HTTP 请求执行鉴权。
 *
 * <h3>拦截决策流程</h3>
 * <pre>
 *   HTTP 请求
 *     ├─ 命中 engineWhitelist（引擎内部路径）→ 直接放行
 *     ├─ 命中 excludePatterns（宿主配置排除路径）→ 直接放行
 *     ├─ 未命中 includePatterns（若有配置）→ 直接放行
 *     ├─ subjectProvider.getCurrentUserId() == null → 返回 HTTP 401
 *     ├─ 按 httpMethod + URI 在 usp_api 中查找匹配的 API 资源：
 *     │   ├─ 未找到匹配记录 → 按 undeclaredResourceStrategy（ALLOW/DENY）
 *     │   └─ 找到 API 资源 → 查 authz_permission_item（resModelCode=RES_API, resId=apiCode）
 *     │       ├─ 无对应权限项 → 按 undeclaredResourceStrategy（ALLOW/DENY）
 *     │       └─ 找到权限项 → AuthzFacade.checkByPermCode → PERMIT 放行 / 否则 403
 *     └─ 鉴权通过 → chain.doFilter
 * </pre>
 *
 * <h3>配置属性</h3>
 * <ul>
 *   <li>{@code authz.engine.pep.enabled}：是否启用（默认 true）</li>
 *   <li>{@code authz.engine.pep.http-filter-enabled}：是否启用 HTTP Filter（默认 true）</li>
 *   <li>{@code authz.engine.pep.undeclared-resource-strategy}：未声明资源策略，ALLOW/DENY（默认 ALLOW）</li>
 *   <li>{@code authz.engine.pep.include-patterns}：仅拦截指定路径（Ant 风格，空则拦截全部）</li>
 *   <li>{@code authz.engine.pep.exclude-patterns}：排除路径（Ant 风格）</li>
 * </ul>
 */
@Slf4j
public class AuthzHttpPepFilter extends OncePerRequestFilter {

    /** API 资源模型编码常量，与 authz_meta_model.model_code 对应。 */
    private static final String RES_MODEL_CODE_API = "RES_API";

    /** BO 资源模型编码常量，与 authz_meta_model.model_code 对应。 */
    private static final String RES_MODEL_CODE_DATA_BO = "RES_DATA_BO";

    private static final String OBLIGATION_ROW_FILTER = "rowFilter";

    private static final String OBLIGATION_FIELD_CONTROLS = "fieldControls";

    /** 未声明资源策略：放行。 */
    private static final String STRATEGY_ALLOW = "ALLOW";

    /** 未声明资源策略：拒绝。 */
    private static final String STRATEGY_DENY = "DENY";

    private final AuthzFacade authzFacade;
    private final AuthzSubjectProvider subjectProvider;
    private final ResourceRepository resourceRepository;
    private final PermissionRepository permissionRepository;
    private final DerivationPermissionRepository derivationPermissionRepository;
    private final String tenantId;
    private final String appCode;
    private final List<String> includePatterns;
    private final List<String> excludePatterns;
    private final List<String> engineWhitelist;
    private final String undeclaredResourceStrategy;
    private final AntPathMatcher pathMatcher;
    private final ObjectMapper objectMapper;

    /**
     * 构造 HTTP PEP Filter。
     *
     * @param authzFacade                统一鉴权入口
     * @param subjectProvider            主体身份提供者 SPI
     * @param resourceRepository         资源仓储（查 usp_api）
     * @param permissionRepository       权限项仓储（查 authz_permission_item）
    * @param derivationPermissionRepository 派生权限仓储（查 API -> BO 绑定）
     * @param tenantId                   当前租户 ID
     * @param appCode                    当前应用编码
     * @param includePatterns            只拦截这些路径（Ant 风格，空则拦截全部）
     * @param excludePatterns            排除路径（Ant 风格，优先于 includePatterns）
     * @param engineWhitelist            引擎内部白名单，始终放行
     * @param undeclaredResourceStrategy 未声明资源处理策略：ALLOW / DENY
     */
    public AuthzHttpPepFilter(AuthzFacade authzFacade,
                               AuthzSubjectProvider subjectProvider,
                               ResourceRepository resourceRepository,
                               PermissionRepository permissionRepository,
                               DerivationPermissionRepository derivationPermissionRepository,
                               String tenantId,
                               String appCode,
                               List<String> includePatterns,
                               List<String> excludePatterns,
                               List<String> engineWhitelist,
                               String undeclaredResourceStrategy) {
        this.authzFacade = authzFacade;
        this.subjectProvider = subjectProvider;
        this.resourceRepository = resourceRepository;
        this.permissionRepository = permissionRepository;
        this.derivationPermissionRepository = derivationPermissionRepository == null
            ? new DerivationPermissionRepository() { }
            : derivationPermissionRepository;
        this.tenantId = tenantId;
        this.appCode = appCode;
        this.includePatterns = includePatterns;
        this.excludePatterns = excludePatterns;
        this.engineWhitelist = engineWhitelist;
        this.undeclaredResourceStrategy = undeclaredResourceStrategy != null
                ? undeclaredResourceStrategy.toUpperCase()
                : STRATEGY_ALLOW;
        this.pathMatcher = new AntPathMatcher();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String requestUri = request.getRequestURI();
        String httpMethod = request.getMethod();

        // 步骤 1：引擎白名单 / 排除模式 / 包含模式路径放行检查
        if (shouldSkipByPath(requestUri)) {
            chain.doFilter(request, response);
            return;
        }

        // 步骤 2：按 URI + HTTP 方法匹配 usp_api
        // 注意：认证检查必须在确认"资源已声明且有权限项"之后再执行，
        //       否则 ALLOW 策略下未登录用户访问未声明资源（如管理 UI、登录页）会被误返回 401。
        List<SysResApi> matchingApis = findMatchingApis(httpMethod, requestUri);
        if (hasAmbiguousBestMatch(matchingApis, httpMethod, requestUri)) {
            log.warn("[HTTP-PEP] 请求命中多个同等优先级 API 资源，拒绝放行 method={} uri={} apiCodes={}",
                httpMethod, requestUri,
                matchingApis.stream().map(SysResApi::getApiCode).collect(Collectors.toList()));
            writeErrorResponse(response, HttpServletResponse.SC_CONFLICT, "请求命中多个同等优先级 API 资源，请调整 API 路由配置");
            return;
        }
        if (hasMixedAuthorizationModeConflict(matchingApis)) {
            log.warn("[HTTP-PEP] 请求命中的 API 候选存在跨授权模式冲突，拒绝放行 method={} uri={} apiCodes={}",
                httpMethod,
                requestUri,
                matchingApis.stream().map(this::describeAuthorizationMode).collect(Collectors.toList()));
            writeErrorResponse(response, HttpServletResponse.SC_CONFLICT, "请求命中的 API 候选存在直接授权与间接授权混用，请调整 API 路由配置");
            return;
        }
        SysResApi matchedApi = matchingApis.isEmpty() ? null : matchingApis.get(0);

        if (matchedApi == null) {
            // 步骤 3：未在 usp_api 中声明的路径，按策略处理
            log.debug("[HTTP-PEP] 未在 usp_api 中找到匹配的 API 资源 method={} uri={}, 策略={}",
                    httpMethod, requestUri, undeclaredResourceStrategy);
            if (STRATEGY_DENY.equalsIgnoreCase(undeclaredResourceStrategy)) {
                // DENY 策略下未声明资源仍需认证，无认证则 401，有认证则 403
                String userId = subjectProvider.getCurrentUserId();
                if (userId == null || userId.trim().isEmpty()) {
                    writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "未登录或登录已失效，请重新登录");
                    return;
                }
                writeErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "接口未注册授权配置，访问被拒绝");
                return;
            }
            // ALLOW 策略：无需认证，直接放行
            chain.doFilter(request, response);
            return;
        }

        // 步骤 4：按 apiCode 查找对应的权限项
        List<AuthPermissionItem> apiPermItems = findPermissionItemsByApi(matchedApi);
        List<AuthPermissionItem> derivedBoPermItems = findDerivedBoPermissionItems(matchedApi);
        if (!apiPermItems.isEmpty() && !derivedBoPermItems.isEmpty()) {
            log.warn("[HTTP-PEP] API 同时配置直接授权与间接授权，拒绝放行 apiCode={} directPermCodes={} derivedPermCodes={}",
                matchedApi == null ? null : matchedApi.getApiCode(),
                apiPermItems.stream().map(AuthPermissionItem::getPermCode).collect(Collectors.toList()),
                derivedBoPermItems.stream().map(AuthPermissionItem::getPermCode).collect(Collectors.toList()));
            writeErrorResponse(response, HttpServletResponse.SC_CONFLICT, "API 同时配置了直接授权和间接授权，请收敛为单一路径");
            return;
        }
        if (apiPermItems.size() > 1) {
            log.warn("[HTTP-PEP] API 直接授权命中多个权限项，拒绝放行 apiCode={} permCodes={}",
                matchedApi == null ? null : matchedApi.getApiCode(),
                apiPermItems.stream().map(AuthPermissionItem::getPermCode).collect(Collectors.toList()));
            writeErrorResponse(response, HttpServletResponse.SC_CONFLICT, "API 直接授权匹配到多个权限项，请收敛为唯一权限项");
            return;
        }
        if (derivedBoPermItems.size() > 1) {
            log.warn("[HTTP-PEP] API 间接授权命中多个 BO 权限项，拒绝放行 apiCode={} permCodes={}",
                matchedApi == null ? null : matchedApi.getApiCode(),
                derivedBoPermItems.stream().map(AuthPermissionItem::getPermCode).collect(Collectors.toList()));
            writeErrorResponse(response, HttpServletResponse.SC_CONFLICT, "同一 API 只允许绑定一个 BO 权限项");
            return;
        }
        AuthPermissionItem apiPermItem = apiPermItems.isEmpty() ? null : apiPermItems.get(0);
        AuthPermissionItem derivedBoPermItem = derivedBoPermItems.isEmpty() ? null : derivedBoPermItems.get(0);

        if (apiPermItem == null && derivedBoPermItem == null) {
            // 步骤 5：API 已注册但无关联权限项，按策略处理
            log.debug("[HTTP-PEP] API 已注册但无关联权限项 apiCode={}, 策略={}",
                    matchedApi.getApiCode(), undeclaredResourceStrategy);
            if (STRATEGY_DENY.equalsIgnoreCase(undeclaredResourceStrategy)) {
                String userId = subjectProvider.getCurrentUserId();
                if (userId == null || userId.trim().isEmpty()) {
                    writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "未登录或登录已失效，请重新登录");
                    return;
                }
                writeErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "接口未配置权限项，访问被拒绝");
                return;
            }
            // ALLOW 策略：无需认证，直接放行
            chain.doFilter(request, response);
            return;
        }

        // 步骤 6：找到权限项，此时才需要认证
        String userId = subjectProvider.getCurrentUserId();
        if (userId == null || userId.trim().isEmpty()) {
            log.warn("[HTTP-PEP] 访问受保护接口但未认证，返回 401 method={} uri={} permCode={}",
                httpMethod, requestUri, apiPermItem != null ? apiPermItem.getPermCode()
                : derivedBoPermItem != null ? derivedBoPermItem.getPermCode() : null);
            writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "未登录或登录已失效，请重新登录");
            return;
        }

        // 步骤 7：调用 AuthzFacade 执行鉴权
        AuthPermissionItem accessPermItem = apiPermItem != null ? apiPermItem : derivedBoPermItem;
        String permCode = accessPermItem.getPermCode();
        log.debug("[HTTP-PEP] 开始鉴权 userId={} permCode={} method={} uri={}",
                userId, permCode, httpMethod, requestUri);

        AuthzDecision decision = authzFacade.checkByPermCode(tenantId, appCode, userId, permCode, null);

        if (!isEffectivePermit(decision, permCode, userId)) {
            log.warn("[HTTP-PEP] 鉴权拒绝 userId={} permCode={} method={} uri={} reason={}",
                    userId, permCode, httpMethod, requestUri,
                    decision != null ? decision.getReason() : "null");
            writeErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "无操作权限");
            return;
        }

        log.debug("[HTTP-PEP] 鉴权通过 userId={} permCode={} method={} uri={}",
                userId, permCode, httpMethod, requestUri);

        // 步骤 8：将鉴权决策结果（obligations）写入请求属性，供下游业务层（BO Hook 等）使用
        if (decision != null && decision.getObligations() != null) {
            request.setAttribute("authzDecision", decision);
            // 同时写入 ThreadLocal，供 MyBatis 行过滤拦截器自动应用 rowFilter
            AuthzDecisionHolder.set(decision);
        }

        try {
            chain.doFilter(request, response);
        } finally {
            // 请求完成同步清理 ThreadLocal，防止线程池复用时内存泄漏
            AuthzDecisionHolder.clear();
        }
    }

    /**
     * 判断请求路径是否应被跳过（白名单 / 排除模式 / 不在包含模式内）。
     *
     * @param requestUri 请求 URI
     * @return true 表示跳过鉴权
     */
    private boolean shouldSkipByPath(String requestUri) {
        // 引擎内部白名单始终放行
        for (String pattern : engineWhitelist) {
            if (pathMatcher.match(pattern, requestUri)) {
                log.trace("[HTTP-PEP] 命中引擎白名单放行 uri={} pattern={}", requestUri, pattern);
                return true;
            }
        }

        // excludePatterns 匹配则放行（优先于 includePatterns）
        for (String pattern : excludePatterns) {
            if (pathMatcher.match(pattern, requestUri)) {
                log.trace("[HTTP-PEP] 命中排除模式放行 uri={} pattern={}", requestUri, pattern);
                return true;
            }
        }

        // includePatterns 非空时，只拦截匹配的路径，其余全部放行
        if (!includePatterns.isEmpty()) {
            boolean matched = false;
            for (String pattern : includePatterns) {
                if (pathMatcher.match(pattern, requestUri)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                log.trace("[HTTP-PEP] 未命中任何包含模式，放行 uri={}", requestUri);
                return true;
            }
        }

        return false;
    }

    /**
     * 按 HTTP 方法和请求 URI 在 usp_api 中查找匹配的 API 资源定义。
     *
     * <p>匹配逻辑：
     * <ol>
     *   <li>先过滤 httpMethod 相同（大小写不敏感）或 httpMethod 为 "*"（通配）的记录</li>
     *   <li>再用 AntPathMatcher 匹配 uriPattern 与请求 URI</li>
     *   <li>多条匹配时取最精确的（路径最长或通配符最少的）</li>
     * </ol>
     *
     * @param httpMethod HTTP 方法，如 GET、POST
     * @param requestUri 请求 URI
     * @return 匹配的 SysResApi，未找到返回 null
     */
    private List<SysResApi> findMatchingApis(String httpMethod, String requestUri) {
        List<SysResApi> apis = resourceRepository.listApis(tenantId, appCode);
        if (apis == null || apis.isEmpty()) {
            return Collections.emptyList();
        }
        List<SysResApi> matches = new ArrayList<>();

        for (SysResApi api : apis) {
            // 过滤 httpMethod：支持精确匹配（GET/POST...）和通配符（*）
            String apiMethod = api.getHttpMethod();
            boolean methodMatches = isMethodMatch(httpMethod, apiMethod);
            if (!methodMatches) {
                continue;
            }

            String pattern = api.getUriPattern();
            if (pattern == null || pattern.isEmpty()) {
                continue;
            }

            if (!pathMatcher.match(pattern, requestUri)) {
                continue;
            }
            matches.add(api);
        }
        matches.sort((left, right) -> compareApiMatchPriority(left, right, httpMethod, requestUri));
        return matches;
    }

    /**
     * 按 API 资源定义查找对应的权限项。
     *
        * <p>标准约定是 {@code authz_permission_item.res_id = apiCode}，
        * 且 API 直接授权权限项编码统一为四段式。
     *
     * @param api API 资源定义
     * @return 匹配的权限项，未找到返回 null
     */
    private List<AuthPermissionItem> findPermissionItemsByApi(SysResApi api) {
        if (api == null || api.getApiCode() == null || api.getApiCode().isEmpty()) {
            return Collections.emptyList();
        }
        String apiCode = api.getApiCode();
        // 一阶：按 RES_API 类型查找（标准 API 层权限）
        List<AuthPermissionItem> items = permissionRepository.findPermissionItemsByResModelCode(
                tenantId, appCode, RES_MODEL_CODE_API);
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        return items.stream()
            .filter(Objects::nonNull)
            .filter(item -> apiCode.equals(item.getResId()))
            .collect(Collectors.toList());
    }

    private List<AuthPermissionItem> findDerivedBoPermissionItems(SysResApi api) {
        if (api == null || api.getId() == null) {
            return Collections.emptyList();
        }
        List<ResourceDerivationPermission> bindings = derivationPermissionRepository.listBindingsByResource(
                tenantId, appCode, RES_MODEL_CODE_API, api.getId());
        if (bindings == null || bindings.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> permItemIds = bindings.stream()
            .filter(Objects::nonNull)
            .map(ResourceDerivationPermission::getPermItemId)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
        if (permItemIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<AuthPermissionItem> items = permissionRepository.findPermissionItemsByIds(
                tenantId, appCode, permItemIds);
        if (items == null || items.isEmpty()) {
            log.warn("[HTTP-PEP] API 派生绑定指向的权限项不存在: apiId={}, apiCode={}, permItemIds={}",
                    api.getId(), api.getApiCode(), permItemIds);
            return Collections.emptyList();
        }
        return items.stream()
            .filter(Objects::nonNull)
            .filter(item -> {
                boolean valid = RES_MODEL_CODE_DATA_BO.equalsIgnoreCase(item.getResModelCode());
                if (!valid) {
                    log.warn("[HTTP-PEP] API 派生绑定权限项不是 RES_DATA_BO，忽略该绑定: apiId={}, apiCode={}, permItemId={}, resModelCode={}",
                        api.getId(), api.getApiCode(), item.getId(), item.getResModelCode());
                }
                return valid;
            })
            .collect(Collectors.toList());
    }

    private boolean hasAmbiguousBestMatch(List<SysResApi> matchingApis, String httpMethod, String requestUri) {
        return matchingApis != null
            && matchingApis.size() > 1
            && compareApiMatchPriority(matchingApis.get(0), matchingApis.get(1), httpMethod, requestUri) == 0;
    }

    private boolean hasMixedAuthorizationModeConflict(List<SysResApi> matchingApis) {
        if (matchingApis == null || matchingApis.size() <= 1) {
            return false;
        }
        boolean hasDirect = false;
        boolean hasDerived = false;
        for (SysResApi api : matchingApis) {
            List<AuthPermissionItem> directItems = findPermissionItemsByApi(api);
            List<AuthPermissionItem> derivedItems = findDerivedBoPermissionItems(api);
            if (!directItems.isEmpty() && !derivedItems.isEmpty()) {
                return true;
            }
            if (!directItems.isEmpty()) {
                hasDirect = true;
            }
            if (!derivedItems.isEmpty()) {
                hasDerived = true;
            }
            if (hasDirect && hasDerived) {
                return true;
            }
        }
        return false;
    }

    private String describeAuthorizationMode(SysResApi api) {
        List<AuthPermissionItem> directItems = findPermissionItemsByApi(api);
        List<AuthPermissionItem> derivedItems = findDerivedBoPermissionItems(api);
        String mode;
        if (!directItems.isEmpty() && !derivedItems.isEmpty()) {
            mode = "MIXED";
        } else if (!directItems.isEmpty()) {
            mode = "DIRECT";
        } else if (!derivedItems.isEmpty()) {
            mode = "DERIVED";
        } else {
            mode = "UNBOUND";
        }
        return api.getApiCode() + "(" + mode + ")";
    }

    private int compareApiMatchPriority(SysResApi left, SysResApi right, String httpMethod, String requestUri) {
        boolean leftExactMethod = isExactMethodMatch(httpMethod, left == null ? null : left.getHttpMethod());
        boolean rightExactMethod = isExactMethodMatch(httpMethod, right == null ? null : right.getHttpMethod());
        if (leftExactMethod != rightExactMethod) {
            return leftExactMethod ? -1 : 1;
        }
        String leftPattern = left == null ? null : left.getUriPattern();
        String rightPattern = right == null ? null : right.getUriPattern();
        return pathMatcher.getPatternComparator(requestUri).compare(leftPattern, rightPattern);
    }

    private boolean isMethodMatch(String requestMethod, String apiMethod) {
        return "*".equals(apiMethod) || requestMethod.equalsIgnoreCase(apiMethod);
    }

    private boolean isExactMethodMatch(String requestMethod, String apiMethod) {
        return StringUtils.hasText(apiMethod)
            && !"*".equals(apiMethod.trim())
            && requestMethod.equalsIgnoreCase(apiMethod);
    }

    private boolean isSamePermissionItem(AuthPermissionItem left, AuthPermissionItem right) {
        if (left == null || right == null) {
            return false;
        }
        if (left.getId() != null && right.getId() != null) {
            return left.getId().equals(right.getId());
        }
        return Objects.equals(left.getPermCode(), right.getPermCode());
    }

    private AuthzDecision mergeDecision(AuthzDecision primary, AuthzDecision secondary) {
        if (primary == null) {
            return secondary;
        }
        if (secondary == null) {
            return primary;
        }
        return AuthzDecision.builder()
                .decision(primary.getDecision())
                .reason(primary.getReason())
                .matchedPermissions(mergeList(primary.getMatchedPermissions(), secondary.getMatchedPermissions()))
                .matchedAssignmentIds(mergeList(primary.getMatchedAssignmentIds(), secondary.getMatchedAssignmentIds()))
                .matchedDelegateIds(mergeList(primary.getMatchedDelegateIds(), secondary.getMatchedDelegateIds()))
                .matchedPolicyTemplateCodes(mergeList(primary.getMatchedPolicyTemplateCodes(), secondary.getMatchedPolicyTemplateCodes()))
                .obligations(mergeObligations(primary.getObligations(), secondary.getObligations()))
                .auditLogId(primary.getAuditLogId() != null ? primary.getAuditLogId() : secondary.getAuditLogId())
                .build();
    }

    private List<String> mergeList(List<String> primary, List<String> secondary) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        if (primary != null) {
            merged.addAll(primary);
        }
        if (secondary != null) {
            merged.addAll(secondary);
        }
        return new ArrayList<>(merged);
    }

    private Map<String, Object> mergeObligations(Map<String, Object> primary, Map<String, Object> secondary) {
        Map<String, Object> merged = new LinkedHashMap<>();
        if (primary != null && !primary.isEmpty()) {
            merged.putAll(primary);
        }
        if (secondary == null || secondary.isEmpty()) {
            return merged;
        }
        for (Map.Entry<String, Object> entry : secondary.entrySet()) {
            if (OBLIGATION_ROW_FILTER.equals(entry.getKey()) || OBLIGATION_FIELD_CONTROLS.equals(entry.getKey())) {
                merged.put(entry.getKey(), entry.getValue());
                continue;
            }
            merged.putIfAbsent(entry.getKey(), entry.getValue());
        }
        return merged;
    }

    /**
     * 判断鉴权结果是否为有效放行。
     *
     * <p>PERMIT 放行；DENY 拒绝；INDETERMINATE 按权限项 failStrategy 决定（默认 DENY）。
     *
     * @param decision 鉴权结果
     * @param permCode 权限项编码（用于日志）
     * @param userId   用户 ID（用于日志）
     * @return true 表示放行
     */
    private boolean isEffectivePermit(AuthzDecision decision, String permCode, String userId) {
        if (decision == null || decision.getDecision() == null) {
            log.warn("[HTTP-PEP] 鉴权结果为空，按拒绝处理 permCode={} userId={}", permCode, userId);
            return false;
        }
        if (decision.getDecision() == DecisionType.PERMIT) {
            return true;
        }
        if (decision.getDecision() == DecisionType.INDETERMINATE) {
            // 读取 obligations 中的 failStrategy
            Map<String, Object> obligations = decision.getObligations();
            if (obligations != null) {
                Object strategy = obligations.get("failStrategy");
                if (STRATEGY_ALLOW.equalsIgnoreCase(String.valueOf(strategy))) {
                    log.warn("[HTTP-PEP] INDETERMINATE 按 ALLOW 降级放行 permCode={} userId={} reason={}",
                            permCode, userId, decision.getReason());
                    return true;
                }
            }
            log.warn("[HTTP-PEP] INDETERMINATE 按 DENY 降级拒绝 permCode={} userId={} reason={}",
                    permCode, userId, decision.getReason());
            return false;
        }
        return false;
    }

    /**
     * 统计路径模式中通配符字符数量，用于评估匹配精确度。
     *
     * @param pattern Ant 路径模式
     * @return 通配符字符数量（{@code ?}、{@code *} 各计 1）
     */
    private int countWildcards(String pattern) {
        int count = 0;
        for (char c : pattern.toCharArray()) {
            if (c == '*' || c == '?') {
                count++;
            }
        }
        return count;
    }

    /**
     * 向响应写入 JSON 格式的错误信息。
     *
     * @param response   HTTP 响应
     * @param statusCode HTTP 状态码
     * @param message    错误信息
     */
    private void writeErrorResponse(HttpServletResponse response, int statusCode, String message)
            throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", statusCode);
        body.put("message", message);
        body.put("data", null);

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
