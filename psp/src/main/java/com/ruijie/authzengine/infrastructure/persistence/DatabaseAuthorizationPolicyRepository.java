package com.ruijie.authzengine.infrastructure.persistence;

import com.ruijie.authzengine.domain.model.common.SubjectKey;
import com.ruijie.authzengine.domain.model.decision.AuthzContext;
import com.ruijie.authzengine.domain.model.decision.PermissionGrant;
import com.ruijie.authzengine.domain.repository.AuthorizationPolicyRepository;
import com.ruijie.authzengine.infrastructure.persistence.entity.AuthPermissionItemEntity;
import com.ruijie.authzengine.infrastructure.persistence.entity.SysAssignmentDelegateEntity;
import com.ruijie.authzengine.infrastructure.persistence.entity.SysAuthAssignmentEntity;
import com.ruijie.authzengine.infrastructure.persistence.service.AuthPermissionItemPersistenceService;
import com.ruijie.authzengine.infrastructure.persistence.service.SysAssignmentDelegatePersistenceService;
import com.ruijie.authzengine.infrastructure.persistence.service.SysAuthAssignmentPersistenceService;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import lombok.extern.slf4j.Slf4j;

/**
 * 数据库驱动的授权仓储实现。
 * <p>这是运行时鉴权的核心数据查询层，负责为 PDP 提供应当主体的授权授权 (PermissionGrant) 列表。</p>
 * <p>流程：查直接分配 -> 查委托分配 -> 批量加载权限项 -> 加载策略模板代码 -> 拼装 PermissionGrant。</p>
 */
@Slf4j
@Repository
public class DatabaseAuthorizationPolicyRepository implements AuthorizationPolicyRepository {

    private final SysAuthAssignmentPersistenceService sysAuthAssignmentPersistenceService;

    private final SysAssignmentDelegatePersistenceService sysAssignmentDelegatePersistenceService;

    private final AuthPermissionItemPersistenceService authPermissionItemPersistenceService;

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    public DatabaseAuthorizationPolicyRepository(
        SysAuthAssignmentPersistenceService sysAuthAssignmentPersistenceService,
        SysAssignmentDelegatePersistenceService sysAssignmentDelegatePersistenceService,
        AuthPermissionItemPersistenceService authPermissionItemPersistenceService,
        @org.springframework.context.annotation.Lazy @org.springframework.beans.factory.annotation.Qualifier("authzNamedParameterJdbcTemplate") NamedParameterJdbcTemplate namedParameterJdbcTemplate
    ) {
        this.sysAuthAssignmentPersistenceService = sysAuthAssignmentPersistenceService;
        this.sysAssignmentDelegatePersistenceService = sysAssignmentDelegatePersistenceService;
        this.authPermissionItemPersistenceService = authPermissionItemPersistenceService;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    /**
     * 根据主体集合查询有效的授权授受列表（不带鉴权上下文）。
     */
    @Override
    public List<PermissionGrant> findBySubjects(String tenantId, String appCode, Set<SubjectKey> subjectKeys) {
        return findBySubjects(tenantId, appCode, subjectKeys, null);
    }

    /**
     * 根据主体集合查询授权授受列表，支持通过 AuthzContext 传入委托 ID 则同时查委托分配。
     * <p>步骤：
     * <ol>
     *   <li>查直接分配（前提：未过期）并按 id 建索引 map</li>
     *   <li>查委托分配（前提：有效期内且 ACTIVE）</li>
     *   <li>抒重收集所有 permItemId，批量查询权限项</li>
     *   <li>批量查策略模板代码（原生 SQL）</li>
     *   <li>拼装 PermissionGrant 列表并返回</li>
     * </ol>
     * </p>
     */
    @Override
    public List<PermissionGrant> findBySubjects(
        String tenantId,
        String appCode,
        Set<SubjectKey> subjectKeys,
        AuthzContext context
    ) {
        if (subjectKeys == null || subjectKeys.isEmpty()) {
            log.debug("[授权查询] subjectKeys 为空，直接返回空授受: tenantId={}, appCode={}", tenantId, appCode);
            return Collections.emptyList();
        }
        log.debug("[授权查询] 开始查询: tenantId={}, appCode={}, subjectCount={}, hasDelegationContext={}",
            tenantId, appCode, subjectKeys.size(), context != null && context.getDelegationIds() != null && !context.getDelegationIds().isEmpty());

        // 第一步：查询直接分配（每个主体-属性对各查一次）
        Map<Long, SysAuthAssignmentEntity> assignmentMap = findDirectAssignments(tenantId, appCode, subjectKeys);
        // 第二步：查询委托分配（展开 context 中的 delegationIds）
        List<SysAssignmentDelegateEntity> delegationEntities = findDelegations(tenantId, appCode, context);
        if (assignmentMap.isEmpty() && delegationEntities.isEmpty()) {
            log.debug("[授权查询] 未找到任何分配或委托: tenantId={}, appCode={}", tenantId, appCode);
            return Collections.emptyList();
        }
        log.debug("[授权查询] 分配初步结果: directAssignments={}, delegations={}",
            assignmentMap.size(), delegationEntities.size());

        // 第三步：收集所有待加载的 permItemId（去重）
        List<Long> permItemIds = new ArrayList<>();
        assignmentMap.values().stream()
            .map(SysAuthAssignmentEntity::getPermItemId)
            .filter(Objects::nonNull)
            .forEach(permItemIds::add);
        delegationEntities.stream()
            .map(SysAssignmentDelegateEntity::getPermItemId)
            .filter(Objects::nonNull)
            .forEach(permItemIds::add);
        permItemIds = permItemIds.stream().distinct().collect(Collectors.toList());
        if (permItemIds.isEmpty()) {
            log.debug("[授权查询] 分配记录中 permItemId 均为空，返回空授受");
            return Collections.emptyList();
        }

        // 第四步：批量加载权限项（IN 查询）
        Map<Long, AuthPermissionItemEntity> permissionItemMap = authPermissionItemPersistenceService.lambdaQuery()
            .eq(AuthPermissionItemEntity::getTenantId, tenantId)
            .eq(AuthPermissionItemEntity::getAppCode, appCode)
            .in(AuthPermissionItemEntity::getId, permItemIds)
            .list()
            .stream()
            .collect(Collectors.toMap(AuthPermissionItemEntity::getId, entity -> entity, (left, right) -> left, LinkedHashMap::new));
        log.debug("[授权查询] 加载权限项: requested={}, loaded={}", permItemIds.size(), permissionItemMap.size());

        // 第五步：批量加载策略模板运行时数据（原生 SQL）
        Map<Long, PolicyTemplateRuntime> policyTemplateRuntimeMap = findPolicyTemplateRuntimes(tenantId, assignmentMap.values());

        // 第六步：拼装 PermissionGrant 列表
        List<PermissionGrant> grants = assignmentMap.values().stream()
            .map(assignment -> toGrant(
                assignment,
                permissionItemMap.get(assignment.getPermItemId()),
                policyTemplateRuntimeMap.get(assignment.getPolicyTplId())
            ))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        grants.addAll(
            delegationEntities.stream()
                .map(delegate -> toGrant(delegate, permissionItemMap.get(delegate.getPermItemId())))
                .filter(Objects::nonNull)
                .collect(Collectors.toList())
        );
        log.debug("[授权查询] 最终授受: grants={} (direct={}, delegated={})",
            grants.size(), assignmentMap.size(), delegationEntities.size());
        return grants;
    }

    /**
     * 查询直接分配：遍历每个 SubjectKey，筛除已过期的分配。
     * <p>过期判断： expireTime 为空表示永不过期；不为空时要求 expireTime > now。</p>
     */
    private Map<Long, SysAuthAssignmentEntity> findDirectAssignments(String tenantId, String appCode, Set<SubjectKey> subjectKeys) {
        Map<Long, SysAuthAssignmentEntity> assignmentMap = new LinkedHashMap<>();
        LocalDateTime now = LocalDateTime.now();
        for (SubjectKey subjectKey : subjectKeys) {
            log.debug("[授权查询] 查直接分配: subjectType={}, subjectId={}",
                subjectKey.getSubjectType(), subjectKey.getSubjectId());
            sysAuthAssignmentPersistenceService.lambdaQuery()
                .eq(SysAuthAssignmentEntity::getTenantId, tenantId)
                .eq(SysAuthAssignmentEntity::getAppCode, appCode)
                .eq(SysAuthAssignmentEntity::getSubjectId, subjectKey.getSubjectId())
                .eq(SysAuthAssignmentEntity::getSubjectModel, subjectKey.getSubjectType())
                // expireTime 为空（永不过期）或 expireTime > now
                .and(wrapper -> wrapper.isNull(SysAuthAssignmentEntity::getExpireTime)
                    .or()
                    .gt(SysAuthAssignmentEntity::getExpireTime, now))
                .list()
                .forEach(entity -> assignmentMap.put(entity.getId(), entity));
        }
        log.debug("[授权查询] 直接分配查询完成: found={}", assignmentMap.size());
        return assignmentMap;
    }

    /**
     * 查询委托分配：从 AuthzContext 中取 delegationIds，按 id + 时间范围 + ACTIVE 状态匹配。
     * <p>context 为 null 或 delegationIds 为空时返回空列表，普通鉴权请求不会走此分支。</p>
     */
    private List<SysAssignmentDelegateEntity> findDelegations(String tenantId, String appCode, AuthzContext context) {
        if (context == null || context.getDelegationIds() == null || context.getDelegationIds().isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> delegationIds = context.getDelegationIds().stream()
            .map(this::toLong)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        if (delegationIds.isEmpty()) {
            log.debug("[授权查询] delegationIds 解析后为空，跳过委托查询");
            return Collections.emptyList();
        }
        LocalDateTime now = LocalDateTime.now();
        log.debug("[授权查询] 查委托分配: delegationIds={}", delegationIds);
        List<SysAssignmentDelegateEntity> result = sysAssignmentDelegatePersistenceService.lambdaQuery()
            .eq(SysAssignmentDelegateEntity::getTenantId, tenantId)
            .eq(SysAssignmentDelegateEntity::getAppCode, appCode)
            .in(SysAssignmentDelegateEntity::getId, delegationIds)
            // 委托生效时间范围：startTime <= now <= endTime
            .le(SysAssignmentDelegateEntity::getStartTime, now)
            .ge(SysAssignmentDelegateEntity::getEndTime, now)
            .eq(SysAssignmentDelegateEntity::getStatus, "ACTIVE")
            .list();
        log.debug("[授权查询] 委托查询完成: found={}", result.size());
        return result;
    }

    /**
     * 策略模板运行时数据，包含模板编码和表达式等评估所需字段。
     */
    private static final class PolicyTemplateRuntime {
        final String templateCode;
        final String status;
        final String polType;
        final String expressionScript;
        final String paramSchema;

        PolicyTemplateRuntime(String templateCode, String status, String polType,
                              String expressionScript, String paramSchema) {
            this.templateCode = templateCode;
            this.status = status;
            this.polType = polType;
            this.expressionScript = expressionScript;
            this.paramSchema = paramSchema;
        }
    }

    /**
     * 批量查询策略模板运行时数据（原生 SQL）。
     * <p>查询范围包含全局模板（tenant_id = '__GLOBAL__'）和当前租户的模板。
     * 加载 template_code、status、pol_type、expression_script、param_schema，
     * 供 PDP 策略评估使用。</p>
     */
    private Map<Long, PolicyTemplateRuntime> findPolicyTemplateRuntimes(String tenantId, Iterable<SysAuthAssignmentEntity> assignments) {
        List<Long> policyTemplateIds = StreamSupport.stream(assignments.spliterator(), false)
            .map(SysAuthAssignmentEntity::getPolicyTplId)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
        if (policyTemplateIds.isEmpty()) {
            return Collections.emptyMap();
        }
        log.debug("[授权查询] 加载策略模板运行时数据: policyTplIds={}", policyTemplateIds);
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("tenantIds", Arrays.asList("__GLOBAL__", tenantId))
            .addValue("ids", policyTemplateIds);
        List<Map<String, Object>> rows = namedParameterJdbcTemplate.queryForList(
            "SELECT id, template_code, status, pol_type, expression_script, param_schema "
                + "FROM authz_std_pol_template WHERE is_deleted = 0 AND tenant_id IN (:tenantIds) AND id IN (:ids)",
            params
        );
        Map<Long, PolicyTemplateRuntime> result = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            result.put(
                ((Number) row.get("id")).longValue(),
                new PolicyTemplateRuntime(
                    asString(row.get("template_code")),
                    asString(row.get("status")),
                    asString(row.get("pol_type")),
                    asString(row.get("expression_script")),
                    asString(row.get("param_schema"))
                )
            );
        }
        log.debug("[授权查询] 策略模板运行时数据加载完成: loaded={}", result.size());
        return result;
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private PermissionGrant toGrant(
        SysAuthAssignmentEntity assignment,
        AuthPermissionItemEntity permissionItem,
        PolicyTemplateRuntime templateRuntime
    ) {
        if (permissionItem == null) {
            return null;
        }
        PermissionGrant.PermissionGrantBuilder builder = PermissionGrant.builder()
            .assignmentId(assignment.getId())
            .permItemId(permissionItem.getId())
            .tenantId(assignment.getTenantId())
            .appCode(assignment.getAppCode())
            .subjectType(assignment.getSubjectModel())
            .subjectId(assignment.getSubjectId())
            .resourceType(permissionItem.getResModelCode())
            .resId(normalizeGrantResId(permissionItem.getResId()))
            .action(permissionItem.getActCode())
            .permissionCode(permissionItem.getPermCode())
            .failStrategy(permissionItem.getFailStrategy());
        // 策略模板运行时字段
        if (templateRuntime != null) {
            builder.policyTemplateCode(templateRuntime.templateCode)
                .policyTemplateId(assignment.getPolicyTplId())
                .policyTemplateStatus(templateRuntime.status)
                .policyTemplateType(templateRuntime.polType)
                .expressionScript(templateRuntime.expressionScript)
                .paramSchema(templateRuntime.paramSchema);
        }
        // 授权分配绑定的策略参数
        if (assignment.getPolicyParams() != null && !assignment.getPolicyParams().trim().isEmpty()) {
            builder.policyParams(assignment.getPolicyParams().trim());
        }
        return builder.build();
    }

    private PermissionGrant toGrant(
        SysAssignmentDelegateEntity delegation,
        AuthPermissionItemEntity permissionItem
    ) {
        if (permissionItem == null) {
            return null;
        }
        return PermissionGrant.builder()
            .permItemId(permissionItem.getId())
            .tenantId(delegation.getTenantId())
            .appCode(delegation.getAppCode())
            .subjectType(delegation.getDelegateSubjectModel())
            .subjectId(delegation.getDelegateSubjectId())
            .resourceType(permissionItem.getResModelCode())
            .resId(normalizeGrantResId(permissionItem.getResId()))
            .action(permissionItem.getActCode())
            .permissionCode(permissionItem.getPermCode())
            .failStrategy(permissionItem.getFailStrategy())
            .delegateId(String.valueOf(delegation.getId()))
            .build();
    }

    private Long toLong(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(rawValue.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    /**
     * 兼容历史数据：将数据库中遗留的空值或旧哨兵值统一归一化为空字符串，表示模型级权限。
     */
    private String normalizeGrantResId(String rawResId) {
        if (rawResId == null || rawResId.trim().isEmpty()) {
            return "";
        }
        return rawResId.trim();
    }

}