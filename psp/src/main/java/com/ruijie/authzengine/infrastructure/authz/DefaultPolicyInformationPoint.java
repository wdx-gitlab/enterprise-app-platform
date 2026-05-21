package com.ruijie.authzengine.infrastructure.authz;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruijie.authzengine.application.spi.AuthMetaModelAdapter;
import com.ruijie.authzengine.application.spi.AuthMetaModelRuntimeContext;
import com.ruijie.authzengine.application.spi.AuthMetaModelRuntimeContextHolder;
import com.ruijie.authzengine.application.spi.BoMetaModelAdapter;
import com.ruijie.authzengine.application.spi.BoMetaModelRuntimeContext;
import com.ruijie.authzengine.application.spi.BoMetaModelRuntimeContextHolder;
import com.ruijie.authzengine.application.spi.ModelCode;
import com.ruijie.authzengine.application.spi.SubjectHookResult;
import com.ruijie.authzengine.domain.model.common.HookExecutionStatus;
import com.ruijie.authzengine.domain.model.common.SubjectKey;
import com.ruijie.authzengine.domain.model.decision.AuthzContext;
import com.ruijie.authzengine.domain.model.decision.AuthzRequest;
import com.ruijie.authzengine.domain.service.PolicyInformationPoint;
import com.ruijie.authzengine.domain.service.SubjectExpansionService;
import com.ruijie.authzengine.infrastructure.persistence.entity.AuthMetaModelEntity;
import com.ruijie.authzengine.infrastructure.persistence.entity.BoMetaModelEntity;
import com.ruijie.authzengine.infrastructure.persistence.service.AuthMetaModelPersistenceService;
import com.ruijie.authzengine.infrastructure.persistence.service.BoMetaModelPersistenceService;
import com.ruijie.authzengine.shared.exception.AuthzIntegrationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * PIP（策略信息点）默认实现，被 PDP 按需调用。
 *
 * <p>核心流程（{@link #loadContext}）：
 * <ol>
 *   <li>从 RuntimeLookupService 加载治理属性（主体/资源/动作注册状态、动作标准化编码）</li>
 *   <li>合并关联主体（角色、组织、岗位、用户组）到上下文属性</li>
 *   <li>展开主体集合（SubjectExpansionService）：用户 + 角色 + 组织 + 岗位 + 用户组</li>
 *   <li>加载委托授权记录标识</li>
 *   <li>组装并返回 AuthzContext，供 PDP 用于策略匹配</li>
 * </ol>
 */
@Slf4j
@Component
public class DefaultPolicyInformationPoint implements PolicyInformationPoint {

    /** 主体展开服务：负责把用户 + 上下文属性中的角色/组织/岗位/用户组展开为 SubjectKey 集合 */
    private final SubjectExpansionService subjectExpansionService;

    /** 治理运行时查询服务：对接数据库，提供主体关联、资源识别、动作标准化和委托命中能力 */
    private final RuntimeLookupService runtimeLookupService;

    /** 主体元模型持久化服务：按需读取主体 resolver 配置 */
    private final AuthMetaModelPersistenceService authMetaModelPersistenceService;

    /** Subject Resolver 路由器：把主体 resolver 解析到宿主业务应用内部 Bean */
    private final AuthMetaResolverRouter authMetaResolverRouter;

    /** BO 元模型持久化服务：按需读取业务对象 resolver 与 schemaJson 配置 */
    private final BoMetaModelPersistenceService boMetaModelPersistenceService;

    /** BO Resolver 路由器：把 resolver 解析到宿主业务应用内部 Bean */
    private final BoResolverRouter boResolverRouter;

    /** BO schemaJson 校验器：避免运行时加载非法协议 */
    private final BoSchemaJsonValidator boSchemaJsonValidator;

    /** Hook 执行审计记录器：将 Hook 状态写入请求上下文供审计服务持久化 */
    private final HookExecutionAuditRecorder hookExecutionAuditRecorder;

    @Autowired
    public DefaultPolicyInformationPoint(
        SubjectExpansionService subjectExpansionService,
        RuntimeLookupService runtimeLookupService,
        AuthMetaModelPersistenceService authMetaModelPersistenceService,
        AuthMetaResolverRouter authMetaResolverRouter,
        BoMetaModelPersistenceService boMetaModelPersistenceService,
        BoResolverRouter boResolverRouter,
        BoSchemaJsonValidator boSchemaJsonValidator,
        HookExecutionAuditRecorder hookExecutionAuditRecorder
    ) {
        this.subjectExpansionService = subjectExpansionService;
        this.runtimeLookupService = runtimeLookupService;
        this.authMetaModelPersistenceService = authMetaModelPersistenceService;
        this.authMetaResolverRouter = authMetaResolverRouter;
        this.boMetaModelPersistenceService = boMetaModelPersistenceService;
        this.boResolverRouter = boResolverRouter;
        this.boSchemaJsonValidator = boSchemaJsonValidator;
        this.hookExecutionAuditRecorder = hookExecutionAuditRecorder;
    }

    public DefaultPolicyInformationPoint(
        SubjectExpansionService subjectExpansionService,
        RuntimeLookupService runtimeLookupService
    ) {
        this(
            subjectExpansionService,
            runtimeLookupService,
            null,
            AuthMetaResolverRouter.noop(),
            null,
            BoResolverRouter.noop(),
            new BoSchemaJsonValidator(new ObjectMapper()),
            HookExecutionAuditRecorder.noop()
        );
    }

    public DefaultPolicyInformationPoint(SubjectExpansionService subjectExpansionService) {
        this(
            subjectExpansionService,
            RuntimeLookupService.noop(),
            null,
            AuthMetaResolverRouter.noop(),
            null,
            BoResolverRouter.noop(),
            new BoSchemaJsonValidator(new ObjectMapper()),
            HookExecutionAuditRecorder.noop()
        );
    }

    @Override
    public AuthzContext loadContext(AuthzRequest request) {
        // 步骤 1：加载治理属性（主体/资源/动作注册状态 + 动作标准化编码）
        Map<String, Object> governanceAttributes = loadGovernanceAttributes(request);

        // 步骤 2：合并请求携带的原始上下文 + Subject/BO Hook + 关联主体（角色、组织、岗位、用户组） + 治理属性
        Map<String, Object> attributes = request.getContext() == null
            ? new LinkedHashMap<>()
            : new LinkedHashMap<>(request.getContext());
        long subjectHookStartAt = System.currentTimeMillis();
        SubjectContextAttributes subjectContextAttributes;
        try {
            subjectContextAttributes = loadSubjectContextAttributes(request, attributes);
            if (subjectContextAttributes.isResolved()) {
                HookExecutionStatus subjectHookStatus = subjectContextAttributes.getAttributes().isEmpty()
                    ? HookExecutionStatus.EMPTY_RESULT
                    : HookExecutionStatus.SUCCESS;
                hookExecutionAuditRecorder.trace(
                    attributes, subjectHookStatus, System.currentTimeMillis() - subjectHookStartAt,
                    subjectContextAttributes.getAttributes()
                );
            }
        } catch (AuthzIntegrationException ex) {
            hookExecutionAuditRecorder.trace(attributes, HookExecutionStatus.ERROR, System.currentTimeMillis() - subjectHookStartAt, null);
            throw ex;
        }
        mergeSubjectAttributes(attributes, subjectContextAttributes);
        // sub 命名空间：镜像 subjectAttributes 并补充 roles/orgs/positions/groups，供策略表达式使用
        mergeSubNamespace(attributes, subjectContextAttributes, request);
        // 主体关联统一来自引擎库 authz_subject_relation；Subject Hook 仅补充主体属性。
        runtimeLookupService.mergeRelatedSubjects(request, attributes);
        BoContextAttributes boContextAttributes = loadBoInstanceAttributes(request);
        mergeBoAttributes(attributes, boContextAttributes);
        mergeBoGovernanceAttributes(governanceAttributes, boContextAttributes);
        // Subject Hook 解析成功时，将主体标记为已注册（覆盖 isSubjectRegistered 的原始判断）
        // 必须在 attributes.putAll(governanceAttributes) 之前更新 governanceAttributes，
        // 否则 putAll 会将 subjectRegistered 覆盖回 false，导致 PDP 误判为未注册主体
        if (subjectContextAttributes.isResolved()) {
            governanceAttributes.put("subjectRegistered", true);
        }
        attributes.putAll(governanceAttributes);
        // env 命名空间：组装环境上下文属性（时间、IP 等），供策略表达式使用
        mergeEnvNamespace(attributes);
        request.setContext(new LinkedHashMap<>(attributes));

        // 测试用模拟 Hook 异常场景
        if (Boolean.TRUE.equals(attributes.get("simulateHookError"))) {
            throw new AuthzIntegrationException("模拟 Hook 调用失败");
        }

        // 步骤 3：展开主体集合（用户 + 角色 + 组织链 + 岗位 + 用户组）
        Set<SubjectKey> subjectKeys = subjectExpansionService.expand(request, attributes);

        // 步骤 4：基于展开后的主体集合，加载当前生效的委托授权记录
        Set<String> delegationIds = loadDelegationIds(request, subjectKeys);

        log.debug("PIP 展开主体集合 size={}", subjectKeys.size());

        // 步骤 5：组装 AuthzContext 返回给 PDP，包含主体集合、属性、委托记录、治理属性、追踪标识
        return AuthzContext.builder()
            .subjectKeys(subjectKeys)
            .attributes(attributes)
            .delegationIds(delegationIds)
            .governanceAttributes(governanceAttributes)
            .traceId(resolveTraceId(request))
            .build();
    }

    @Override
    public Map<String, Object> loadGovernanceAttributes(AuthzRequest request) {
        return runtimeLookupService.loadGovernanceAttributes(request);
    }

    @Override
    public Set<String> loadDelegationIds(AuthzRequest request, Set<SubjectKey> subjectKeys) {
        return runtimeLookupService.loadDelegationIds(
            request.getTenantId(),
            request.getAppCode(),
            subjectKeys
        );
    }

    private String resolveTraceId(AuthzRequest request) {
        if (request.getTraceId() != null && !request.getTraceId().trim().isEmpty()) {
            return request.getTraceId().trim();
        }
        return UUID.randomUUID().toString();
    }

    private SubjectContextAttributes loadSubjectContextAttributes(
        AuthzRequest request,
        Map<String, Object> attributes
    ) {
        if (authMetaModelPersistenceService == null || request.getSubject() == null) {
            return SubjectContextAttributes.empty();
        }
        if (!StringUtils.hasText(request.getSubject().getType()) || !StringUtils.hasText(request.getSubject().getId())) {
            return SubjectContextAttributes.empty();
        }
        boolean shadowMode = isShadowModeRequested(request);
        // C1 修复：移除 nativeMode 短路返回，始终由 resolver 配置决定是否走宿主系统
        AuthMetaModelEntity authMetaModelEntity = authMetaModelPersistenceService.lambdaQuery()
            .eq(AuthMetaModelEntity::getTenantId, request.getTenantId())
            .eq(AuthMetaModelEntity::getAppCode, request.getAppCode())
            .eq(AuthMetaModelEntity::getModelCode, request.getSubject().getType())
            .one();
        if (authMetaModelEntity == null) {
            return SubjectContextAttributes.empty();
        }
        AuthMetaModelAdapter adapter = authMetaResolverRouter.resolve(
            authMetaModelEntity.getAdapterType(),
            authMetaModelEntity.getResolver()
        );
        if (adapter == null) {
            return SubjectContextAttributes.empty();
        }
        try {
            boolean nativeMode = Boolean.TRUE.equals(attributes.get("subjectRegistered"));
            AuthMetaModelRuntimeContextHolder.bind(AuthMetaModelRuntimeContext.builder()
                .tenantId(request.getTenantId())
                .appCode(request.getAppCode())
                .modelCode(authMetaModelEntity.getModelCode())
                .traceId(resolveTraceId(request))
                .resolver(authMetaModelEntity.getResolver())
                .nativeMode(nativeMode)
                .shadowMode(shadowMode)
                .build());
            SubjectHookResult hookResult = adapter.fetchInstanceAttributes(
                ModelCode.fromCode(authMetaModelEntity.getModelCode()),
                request.getSubject().getId(),
                request.getContext()
            );
            if (hookResult == null) {
                return SubjectContextAttributes.resolved(Collections.<String, Object>emptyMap());
            }
            Map<String, Object> subjectAttributes = sanitizeSubjectAttributes(hookResult.getAttributes());
            log.debug(
                "PIP 加载 Subject Hook 成功 tenantId={}, appCode={}, modelCode={}, subjectId={}, nativeMode={}, shadowMode={}, attributeSize={}",
                request.getTenantId(),
                request.getAppCode(),
                authMetaModelEntity.getModelCode(),
                request.getSubject().getId(),
                nativeMode,
                shadowMode,
                subjectAttributes.size()
            );
            return SubjectContextAttributes.resolved(subjectAttributes);
        } catch (AuthzIntegrationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AuthzIntegrationException(
                "加载主体 Shadow Hook 失败，resolver=" + authMetaModelEntity.getResolver(),
                exception
            );
        } finally {
            AuthMetaModelRuntimeContextHolder.clear();
        }
    }

    /**
     * 加载业务对象（BO）实例属性。
     * <p>
     * 通过 resId（authz_bo_meta_model 表主键 ID）定位业务对象模型，
     * 再由模型的 adapter 和 resolver 获取实例级属性，用于策略评估。
     * resId 为空时表示模型级权限，不涉及实例属性，直接跳过。
     * </p>
     */
    private BoContextAttributes loadBoInstanceAttributes(AuthzRequest request) {
        if (boMetaModelPersistenceService == null || request.getResource() == null) {
            return BoContextAttributes.empty();
        }
        if (!"RES_DATA_BO".equals(request.getResource().getResourceType())) {
            return BoContextAttributes.empty();
        }
        String resIdStr = request.getResource().getResId();
        if (!StringUtils.hasText(resIdStr)) {
            return BoContextAttributes.empty();
        }
        // resId 是 authz_bo_meta_model 表的主键 ID，通过 id 查找业务对象模型
        Long boModelId;
        try {
            boModelId = Long.parseLong(resIdStr.trim());
        } catch (NumberFormatException e) {
            return BoContextAttributes.empty();
        }
        BoMetaModelEntity boMetaModelEntity = boMetaModelPersistenceService.lambdaQuery()
            .eq(BoMetaModelEntity::getTenantId, request.getTenantId())
            .eq(BoMetaModelEntity::getAppCode, request.getAppCode())
            .eq(BoMetaModelEntity::getId, boModelId)
            .one();
        if (boMetaModelEntity == null) {
            return BoContextAttributes.empty();
        }
        BoSchemaJsonValidator.PolicyExposure policyExposure = boSchemaJsonValidator.resolvePolicyExposure(boMetaModelEntity.getSchemaJson());
        BoSchemaJsonValidator.GovernanceMetadata governanceMetadata =
            boSchemaJsonValidator.resolveGovernanceMetadata(boMetaModelEntity.getSchemaJson());
        BoMetaModelAdapter adapter = boResolverRouter.resolve(
            boMetaModelEntity.getAdapterType(),
            boMetaModelEntity.getResolver()
        );
        if (adapter == null) {
            return new BoContextAttributes(Collections.<String, Object>emptyMap(), policyExposure.getResourcePrefix(), governanceMetadata);
        }
        try {
            BoMetaModelRuntimeContextHolder.bind(BoMetaModelRuntimeContext.builder()
                .tenantId(request.getTenantId())
                .appCode(request.getAppCode())
                .boCode(boMetaModelEntity.getBoCode())
                .resolver(boMetaModelEntity.getResolver())
                .build());
            // instanceId 从 context 中获取（业务实例标识），与 resId（BO 模型 ID）严格分离
            String instanceId = resolveInstanceIdFromContext(request);
            if (!StringUtils.hasText(instanceId)) {
                log.debug("PIP BO Hook 无 instanceId，跳过实例属性加载: boCode={}, resId(modelId)={}",
                    boMetaModelEntity.getBoCode(), resIdStr);
                return new BoContextAttributes(Collections.<String, Object>emptyMap(), policyExposure.getResourcePrefix(), governanceMetadata);
            }
            Map<String, Object> attributes = adapter.fetchInstanceAttributes(
                instanceId,
                boMetaModelEntity.getSchemaJson(),
                request.getContext()
            );
            if (attributes == null || attributes.isEmpty()) {
                return new BoContextAttributes(Collections.<String, Object>emptyMap(), policyExposure.getResourcePrefix(), governanceMetadata);
            }
            Map<String, Object> projectedAttributes = policyExposure.projectAttributes(attributes);
            log.debug(
                "PIP 加载 BO 实例属性成功 tenantId={}, appCode={}, boCode={}, resId(modelId)={}, instanceId={}, rawSize={}, exposedSize={}",
                request.getTenantId(),
                request.getAppCode(),
                boMetaModelEntity.getBoCode(),
                resIdStr,
                instanceId,
                attributes.size(),
                projectedAttributes.size()
            );
            return new BoContextAttributes(projectedAttributes, policyExposure.getResourcePrefix(), governanceMetadata);
        } catch (AuthzIntegrationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AuthzIntegrationException(
                "加载业务对象实例属性失败，resolver=" + boMetaModelEntity.getResolver(),
                exception
            );
        } finally {
            BoMetaModelRuntimeContextHolder.clear();
        }
    }

    private void mergeBoAttributes(Map<String, Object> attributes, BoContextAttributes boContextAttributes) {
        if (boContextAttributes == null || boContextAttributes.getAttributes().isEmpty()) {
            return;
        }
        Map<String, Object> boAttributes = boContextAttributes.getAttributes();
        attributes.put("resourceAttributes", mergeNamedAttributes(attributes.get("resourceAttributes"), boAttributes));
        attributes.put("res", mergeNamedAttributes(attributes.get("res"), boAttributes));
        if (StringUtils.hasText(boContextAttributes.getResourcePrefix())
            && !"res".equals(boContextAttributes.getResourcePrefix())
            && !"resourceAttributes".equals(boContextAttributes.getResourcePrefix())) {
            attributes.put(
                boContextAttributes.getResourcePrefix(),
                mergeNamedAttributes(attributes.get(boContextAttributes.getResourcePrefix()), boAttributes)
            );
        }
    }

    private void mergeBoGovernanceAttributes(Map<String, Object> governanceAttributes, BoContextAttributes boContextAttributes) {
        if (governanceAttributes == null || boContextAttributes == null || boContextAttributes.getGovernanceMetadata().isEmpty()) {
            return;
        }
        BoSchemaJsonValidator.GovernanceMetadata governanceMetadata = boContextAttributes.getGovernanceMetadata();
        if (StringUtils.hasText(governanceMetadata.getTableName())) {
            governanceAttributes.put("tableName", governanceMetadata.getTableName());
        }
        if (!governanceMetadata.getAttributes().isEmpty()) {
            governanceAttributes.put("attributes", governanceMetadata.getAttributes());
        }
    }

    private void mergeSubjectAttributes(Map<String, Object> attributes, SubjectContextAttributes subjectContextAttributes) {
        if (subjectContextAttributes == null || subjectContextAttributes.getAttributes().isEmpty()) {
            return;
        }
        attributes.put(
            "subjectAttributes",
            mergeNamedAttributes(attributes.get("subjectAttributes"), subjectContextAttributes.getAttributes())
        );
    }

    /**
     * 组装 sub 命名空间，镜像 subjectAttributes 并补充 roles/orgs/positions/groups，
     * 供策略表达式通过 #sub['key'] 访问。
     */
    @SuppressWarnings("unchecked")
    private void mergeSubNamespace(Map<String, Object> attributes, SubjectContextAttributes subjectContextAttributes, AuthzRequest request) {
        Map<String, Object> sub = new LinkedHashMap<>();
        // 基础主体信息
        if (request.getSubject() != null) {
            sub.put("id", request.getSubject().getId());
            sub.put("type", request.getSubject().getType());
        }
        // 合并 Subject Hook 返回的属性
        if (subjectContextAttributes != null && !subjectContextAttributes.getAttributes().isEmpty()) {
            sub.putAll(subjectContextAttributes.getAttributes());
        }
        // 合并已解析的角色/组织/岗位/用户组
        copyIfPresent(attributes, sub, "roles");
        copyIfPresent(attributes, sub, "orgs");
        copyIfPresent(attributes, sub, "positions");
        copyIfPresent(attributes, sub, "groups");
        attributes.put("sub", sub);
    }

    private void copyIfPresent(Map<String, Object> source, Map<String, Object> target, String key) {
        Object value = source.get(key);
        if (value != null) {
            target.put(key, value);
        }
    }

    /**
     * 组装 env 命名空间，包含当前时间和 HTTP 请求信息，供策略表达式通过 #env['key'] 访问。
     * <p>非 HTTP 场景下仅包含时间信息。调用方可通过 context 预传 env 属性实现覆盖。</p>
     */
    @SuppressWarnings("unchecked")
    private void mergeEnvNamespace(Map<String, Object> attributes) {
        Map<String, Object> env;
        Object existing = attributes.get("env");
        if (existing instanceof Map) {
            env = new LinkedHashMap<>((Map<String, Object>) existing);
        } else {
            env = new LinkedHashMap<>();
        }
        // 时间维度
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        env.putIfAbsent("hour", now.getHour());
        env.putIfAbsent("minute", now.getMinute());
        env.putIfAbsent("dayOfWeek", now.getDayOfWeek().getValue());
        env.putIfAbsent("date", now.toLocalDate().toString());
        // HTTP 请求维度（非 HTTP 场景下跳过）
        try {
            org.springframework.web.context.request.ServletRequestAttributes requestAttributes =
                (org.springframework.web.context.request.ServletRequestAttributes)
                    org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (requestAttributes != null) {
                javax.servlet.http.HttpServletRequest httpRequest = requestAttributes.getRequest();
                env.putIfAbsent("ip", httpRequest.getRemoteAddr());
                env.putIfAbsent("uri", httpRequest.getRequestURI());
                env.putIfAbsent("method", httpRequest.getMethod());
            }
        } catch (Exception e) {
            log.debug("[PIP] 非 HTTP 场景，env 命名空间不包含请求信息");
        }
        attributes.put("env", env);
    }

    /**
     * 从请求上下文中提取 instanceId（业务实例标识）。
     * <p>instanceId 由 AOP 切面或调用方写入 context["instanceId"]，
     * 与 resId（BO 模型 ID）语义完全不同，不做相互回退。</p>
     */
    private String resolveInstanceIdFromContext(AuthzRequest request) {
        if (request.getContext() == null) {
            return null;
        }
        Object instanceId = request.getContext().get("instanceId");
        if (instanceId == null) {
            return null;
        }
        String value = String.valueOf(instanceId).trim();
        return value.isEmpty() ? null : value;
    }

    private Map<String, Object> mergeNamedAttributes(Object existingValue, Map<String, Object> boAttributes) {
        Map<String, Object> merged = new LinkedHashMap<>();
        if (existingValue instanceof Map) {
            Map<?, ?> rawMap = (Map<?, ?>) existingValue;
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                merged.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        merged.putAll(boAttributes);
        return merged;
    }

    private boolean isShadowModeRequested(AuthzRequest request) {
        if (request.getContext() == null) {
            return false;
        }
        Object rawValue = request.getContext().get("shadowMode");
        if (rawValue == null) {
            return false;
        }
        if (rawValue instanceof Boolean) {
            return (Boolean) rawValue;
        }
        return Boolean.parseBoolean(String.valueOf(rawValue));
    }

    private Map<String, Object> sanitizeSubjectAttributes(Map<String, Object> rawAttributes) {
        if (rawAttributes == null || rawAttributes.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Object> sanitized = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : rawAttributes.entrySet()) {
            String key = entry.getKey();
            if (!StringUtils.hasText(key)) {
                continue;
            }
            if (isReservedSubjectAttributeKey(key)) {
                throw new AuthzIntegrationException("主体 Shadow Hook 返回了保留字段: " + key);
            }
            sanitized.put(key, entry.getValue());
        }
        return sanitized;
    }

    private boolean isReservedSubjectAttributeKey(String key) {
        return "tenantId".equals(key)
            || "appCode".equals(key)
            || "subjectId".equals(key)
            || "modelCode".equals(key);
    }

    private static final class SubjectContextAttributes {

        private static final SubjectContextAttributes EMPTY = new SubjectContextAttributes(
            Collections.<String, Object>emptyMap(),
            false
        );

        private final Map<String, Object> attributes;

        private final boolean resolved;

        private SubjectContextAttributes(
            Map<String, Object> attributes,
            boolean resolved
        ) {
            this.attributes = attributes == null ? Collections.<String, Object>emptyMap() : new LinkedHashMap<>(attributes);
            this.resolved = resolved;
        }

        private static SubjectContextAttributes empty() {
            return EMPTY;
        }

        private static SubjectContextAttributes resolved(Map<String, Object> attributes) {
            return new SubjectContextAttributes(attributes, true);
        }

        private Map<String, Object> getAttributes() {
            return attributes;
        }

        private boolean isResolved() {
            return resolved;
        }
    }

    private static final class BoContextAttributes {

        private static final BoContextAttributes EMPTY = new BoContextAttributes(
            Collections.<String, Object>emptyMap(),
            "res",
            BoSchemaJsonValidator.GovernanceMetadata.empty()
        );

        private final Map<String, Object> attributes;

        private final String resourcePrefix;

        private final BoSchemaJsonValidator.GovernanceMetadata governanceMetadata;

        private BoContextAttributes(
            Map<String, Object> attributes,
            String resourcePrefix,
            BoSchemaJsonValidator.GovernanceMetadata governanceMetadata
        ) {
            this.attributes = attributes == null ? Collections.<String, Object>emptyMap() : new LinkedHashMap<>(attributes);
            this.resourcePrefix = resourcePrefix;
            this.governanceMetadata = governanceMetadata == null
                ? BoSchemaJsonValidator.GovernanceMetadata.empty()
                : governanceMetadata;
        }

        private static BoContextAttributes empty() {
            return EMPTY;
        }

        private Map<String, Object> getAttributes() {
            return attributes;
        }

        private String getResourcePrefix() {
            return resourcePrefix;
        }

        private BoSchemaJsonValidator.GovernanceMetadata getGovernanceMetadata() {
            return governanceMetadata;
        }
    }
}