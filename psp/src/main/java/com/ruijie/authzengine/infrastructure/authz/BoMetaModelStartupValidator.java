package com.ruijie.authzengine.infrastructure.authz;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruijie.authzengine.domain.model.governance.BoMetaModelDefinition;
import com.ruijie.authzengine.domain.model.governance.permission.AuthPermissionItem;
import com.ruijie.authzengine.domain.repository.MetaRepository;
import com.ruijie.authzengine.domain.repository.DerivationPermissionRepository;
import com.ruijie.authzengine.domain.repository.PermissionRepository;
import com.ruijie.authzengine.domain.repository.ResourceRepository;
import com.ruijie.authzengine.domain.model.governance.resource.SysResApi;
import com.ruijie.authzengine.domain.model.governance.permission.ResourceDerivationPermission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * BO 元模型与 API 资源启动期完整性校验器。
 *
 * <p>在 {@link ApplicationReadyEvent} 触发后（数据库已就绪）执行以下非阻断校验：
 * <ol>
 *   <li><b>BO 元模型完整性</b>：遍历所有 BO 元模型，对符合新协议（{@code entities[]}）的记录校验：
 *     <ul>
 *       <li>至少存在一个实体（§5.3.5 约束一）</li>
 *       <li>至多一个 {@code isPrimary=true} 实体（§5.3.5 约束一）</li>
 *       <li>主实体至少有一个 {@code isPk=true} 属性（§5.3.5 约束二）</li>
 *       <li>{@code operations[]} 非空（§5.3.5 约束三）</li>
 *     </ul>
 *   </li>
 *   <li><b>usp_api 路由唯一性</b>：同一应用下，相同 {@code method + concretePathOrPattern} 的 API 定义
 *       不允许重复（防止 PEP Filter 多命中歧义）。</li>
 *   <li><b>主键字段不受控约束</b>：BO 元模型字段中 {@code isPk=true} 的属性，{@code fieldControl}
 *       不得为 {@code true}（§5.3.5 约束二）。</li>
 * </ol>
 *
 * <p><b>校验策略</b>：所有问题仅输出 WARN 日志，不阻断启动。运维人员可据此修正配置数据。
 * 若未来需要阻断启动，可在此处改为抛 {@code IllegalStateException}。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BoMetaModelStartupValidator {

    private static final String RES_MODEL_CODE_API = "RES_API";

    private final MetaRepository metaRepository;
    private final ResourceRepository resourceRepository;
    private final PermissionRepository permissionRepository;
    private final DerivationPermissionRepository derivationPermissionRepository;

    @Qualifier("authzObjectMapper")
    private final ObjectMapper objectMapper;

    private final ApiRouteMatchSupport apiRouteMatchSupport = new ApiRouteMatchSupport();

    /**
     * 在 Spring Boot 应用就绪后执行启动期校验。
     *
     * @param event 应用就绪事件
     */
    @EventListener(ApplicationReadyEvent.class)
    public void validate(ApplicationReadyEvent event) {
        log.info("[BO-VALIDATOR] 开始执行 BO 元模型与 API 资源启动期完整性校验...");
        int boWarnCount = 0;
        int apiWarnCount = 0;

        try {
            boWarnCount = validateBoMetaModels();
        } catch (Exception e) {
            log.error("[BO-VALIDATOR] BO 元模型校验异常，已跳过该步骤，cause={}", e.getMessage(), e);
        }

        try {
            apiWarnCount = validateApiRouteUniqueness();
        } catch (Exception e) {
            log.error("[BO-VALIDATOR] API 路由唯一性校验异常，已跳过该步骤，cause={}", e.getMessage(), e);
        }

        if (boWarnCount == 0 && apiWarnCount == 0) {
            log.info("[BO-VALIDATOR] 启动期校验通过，未发现完整性问题");
        } else {
            log.warn("[BO-VALIDATOR] 启动期校验完成，发现 {} 条 BO 元模型警告，{} 条 API 路由警告，请及时修正配置数据",
                    boWarnCount, apiWarnCount);
        }
    }

    // -------------------------------------------------------------------------
    // 私有校验方法
    // -------------------------------------------------------------------------

    /**
     * 校验所有 BO 元模型定义的完整性。
     *
     * @return 警告数量
     */
    private int validateBoMetaModels() {
        Map<String, List<String>> tenantApps = metaRepository.listDistinctTenantApps();
        if (tenantApps.isEmpty()) {
            return 0;
        }
        int warnCount = 0;
        for (Map.Entry<String, List<String>> entry : tenantApps.entrySet()) {
            String tenantId = entry.getKey();
            for (String appCode : entry.getValue()) {
                List<BoMetaModelDefinition> models = loadAllBoMetaModels(tenantId, appCode);
                for (BoMetaModelDefinition model : models) {
                    warnCount += validateSingleBo(model);
                }
            }
        }
        return warnCount;
    }

    private int validateSingleBo(BoMetaModelDefinition model) {
        String boKey = model.getTenantId() + "/" + model.getAppCode() + "/" + model.getBoCode();
        if (!StringUtils.hasText(model.getSchemaJson())) {
            return 0;
        }
        int warnCount = 0;
        try {
            JsonNode root = objectMapper.readTree(model.getSchemaJson());

            // 新协议（entities[]）校验
            JsonNode entitiesNode = root.get("entities");
            if (entitiesNode == null || !entitiesNode.isArray()) {
                // 历史 fields[] 协议不在此处校验，跳过
                return 0;
            }

            // 约束一：至少一个实体
            if (entitiesNode.size() == 0) {
                log.warn("[BO-VALIDATOR] BO {} schemaJson.entities 为空，至少需要一个实体（§5.3.5 约束一）", boKey);
                warnCount++;
            }

            // 约束一：至多一个主实体（isPrimary=true）
            int primaryCount = 0;
            JsonNode primaryEntity = null;
            for (JsonNode entity : entitiesNode) {
                if (entity.path("isPrimary").asBoolean(false)) {
                    primaryCount++;
                    primaryEntity = entity;
                }
            }
            if (primaryCount > 1) {
                log.warn("[BO-VALIDATOR] BO {} 存在 {} 个 isPrimary=true 实体，至多允许 1 个（§5.3.5 约束一）", boKey, primaryCount);
                warnCount++;
            }

            // 约束二：主实体至少一个 isPk 属性 + 主键字段不受控
            if (primaryEntity != null) {
                JsonNode attrsNode = primaryEntity.get("attributes");
                if (attrsNode == null || !attrsNode.isArray() || attrsNode.size() == 0) {
                    log.warn("[BO-VALIDATOR] BO {} 主实体无 attributes 定义（§5.3.5 约束二）", boKey);
                    warnCount++;
                } else {
                    boolean hasPk = false;
                    for (JsonNode attr : attrsNode) {
                        if (attr.path("isPk").asBoolean(false)) {
                            hasPk = true;
                            // 主键字段不得开启 fieldControl
                            if (attr.path("fieldControl").asBoolean(false)) {
                                String attrCode = attr.path("code").asText("unknown");
                                log.warn("[BO-VALIDATOR] BO {} 主实体属性 {} 为主键（isPk=true）但开启了 fieldControl，主键字段不允许受控（§5.3.5 约束二）",
                                        boKey, attrCode);
                                warnCount++;
                            }
                        }
                    }
                    if (!hasPk) {
                        log.warn("[BO-VALIDATOR] BO {} 主实体 attributes 中无 isPk=true 属性，至少需要一个主键（§5.3.5 约束二）", boKey);
                        warnCount++;
                    }
                }
            }

            // 约束三：operations[] 非空
            JsonNode operationsNode = root.get("operations");
            if (operationsNode == null || !operationsNode.isArray() || operationsNode.size() == 0) {
                log.warn("[BO-VALIDATOR] BO {} schemaJson.operations 为空，至少需要一个操作（§5.3.5 约束三）", boKey);
                warnCount++;
            }

        } catch (Exception e) {
            log.warn("[BO-VALIDATOR] BO {} schemaJson 解析异常，跳过校验，cause={}", boKey, e.getMessage());
        }
        return warnCount;
    }

    /**
     * 校验 usp_api 中同一应用下路由的唯一性（相同 method + path 不允许重复）。
     *
     * @return 警告数量
     */
    int validateApiRouteUniqueness() {
        Map<String, List<String>> tenantApps = metaRepository.listDistinctTenantApps();
        if (tenantApps.isEmpty()) {
            return 0;
        }
        int warnCount = 0;
        for (Map.Entry<String, List<String>> entry : tenantApps.entrySet()) {
            String tenantId = entry.getKey();
            for (String appCode : entry.getValue()) {
                List<SysResApi> apis = resourceRepository.listApis(tenantId, appCode);
                if (apis == null || apis.isEmpty()) {
                    continue;
                }
                Set<String> directApiCodes = loadDirectApiCodes(tenantId, appCode);
                Map<Long, Boolean> derivedBindingPresence = loadDerivedBindingPresence(tenantId, appCode, apis);
                for (int i = 0; i < apis.size(); i++) {
                    SysResApi left = apis.get(i);
                    AuthorizationMode leftMode = resolveAuthorizationMode(left, directApiCodes, derivedBindingPresence);
                    if (leftMode == AuthorizationMode.MIXED) {
                        log.warn("[BO-VALIDATOR] 应用 {}/{} API {} 同时存在直接授权与间接授权，启动后运行时会拒绝放行，请收敛为单一授权模式",
                            tenantId, appCode, left.getApiCode());
                        warnCount++;
                    }
                    for (int j = i + 1; j < apis.size(); j++) {
                        SysResApi right = apis.get(j);
                        ApiRouteMatchSupport.RouteOverlap overlap = apiRouteMatchSupport.findOverlap(left, right);
                        if (overlap == null) {
                            continue;
                        }
                        AuthorizationMode rightMode = resolveAuthorizationMode(right, directApiCodes, derivedBindingPresence);
                        boolean ambiguous = apiRouteMatchSupport.compareApiMatchPriority(
                            left,
                            right,
                            overlap.getHttpMethod(),
                            overlap.getRequestUri()) == 0;
                        boolean mixedAcrossRoutes = leftMode.isDirectCapable() && rightMode.isDerivedCapable()
                            || leftMode.isDerivedCapable() && rightMode.isDirectCapable();
                        if (!ambiguous && !mixedAcrossRoutes) {
                            continue;
                        }
                        if (ambiguous) {
                            log.warn("[BO-VALIDATOR] 应用 {}/{} 路由 {} 与 {} 在样本请求 {} {} 上存在同优先级多命中，PEP 运行时会拒绝放行，请保证路由唯一解析",
                                tenantId,
                                appCode,
                                left.getApiCode(),
                                right.getApiCode(),
                                overlap.getHttpMethod(),
                                overlap.getRequestUri());
                            warnCount++;
                            continue;
                        }
                        log.warn("[BO-VALIDATOR] 应用 {}/{} 路由 {}({}) 与 {}({}) 在样本请求 {} {} 上存在跨授权模式重叠，PEP 运行时会拒绝放行，请调整路由或授权模式",
                            tenantId,
                            appCode,
                            left.getApiCode(),
                            leftMode,
                            right.getApiCode(),
                            rightMode,
                            overlap.getHttpMethod(),
                            overlap.getRequestUri());
                        warnCount++;
                    }
                }
            }
        }
        return warnCount;
    }

    private Set<String> loadDirectApiCodes(String tenantId, String appCode) {
        List<AuthPermissionItem> items = permissionRepository.findPermissionItemsByResModelCode(
            tenantId,
            appCode,
            RES_MODEL_CODE_API);
        Set<String> apiCodes = new HashSet<>();
        if (items == null) {
            return apiCodes;
        }
        for (AuthPermissionItem item : items) {
            if (item != null && StringUtils.hasText(item.getResId())) {
                apiCodes.add(item.getResId().trim());
            }
        }
        return apiCodes;
    }

    private Map<Long, Boolean> loadDerivedBindingPresence(String tenantId, String appCode, List<SysResApi> apis) {
        Map<Long, Boolean> presence = new HashMap<>();
        for (SysResApi api : apis) {
            if (api == null || api.getId() == null) {
                continue;
            }
            List<ResourceDerivationPermission> bindings = derivationPermissionRepository.listBindingsByResource(
                tenantId,
                appCode,
                RES_MODEL_CODE_API,
                api.getId());
            presence.put(api.getId(), bindings != null && !bindings.isEmpty());
        }
        return presence;
    }

    private AuthorizationMode resolveAuthorizationMode(
        SysResApi api,
        Set<String> directApiCodes,
        Map<Long, Boolean> derivedBindingPresence
    ) {
        boolean direct = api != null
            && StringUtils.hasText(api.getApiCode())
            && directApiCodes.contains(api.getApiCode().trim());
        boolean derived = api != null
            && api.getId() != null
            && Boolean.TRUE.equals(derivedBindingPresence.get(api.getId()));
        if (direct && derived) {
            return AuthorizationMode.MIXED;
        }
        if (direct) {
            return AuthorizationMode.DIRECT;
        }
        if (derived) {
            return AuthorizationMode.DERIVED;
        }
        return AuthorizationMode.UNBOUND;
    }

    private enum AuthorizationMode {
        UNBOUND,
        DIRECT,
        DERIVED,
        MIXED;

        private boolean isDirectCapable() {
            return this == DIRECT || this == MIXED;
        }

        private boolean isDerivedCapable() {
            return this == DERIVED || this == MIXED;
        }
    }

    /**
     * 加载指定应用下所有 BO 元模型（全量分页）。
     */
    private List<BoMetaModelDefinition> loadAllBoMetaModels(String tenantId, String appCode) {
        List<BoMetaModelDefinition> result = new ArrayList<>();
        int pageNo = 1;
        int pageSize = 50;
        while (true) {
            com.ruijie.authzengine.domain.model.governance.PageResult<BoMetaModelDefinition> page =
                    metaRepository.pageBoMetaModels(tenantId, appCode, null, pageNo, pageSize);
            if (page == null || page.getRecords() == null || page.getRecords().isEmpty()) {
                break;
            }
            result.addAll(page.getRecords());
            if (result.size() >= page.getTotal()) {
                break;
            }
            pageNo++;
        }
        return result;
    }
}
