package com.ruijie.authzengine.application.service;

import com.ruijie.authzengine.domain.model.decision.AuthzDecision;
import com.ruijie.authzengine.domain.model.decision.AuthzRequest;
import com.ruijie.authzengine.domain.model.decision.AuthzResource;
import com.ruijie.authzengine.domain.model.decision.AuthzSubject;
import com.ruijie.authzengine.domain.model.decision.DecisionType;
import com.ruijie.authzengine.domain.model.governance.permission.AuthPermissionItem;
import com.ruijie.authzengine.domain.repository.PermissionRepository;
import com.ruijie.authzengine.domain.service.PolicyEnforcementPoint;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 统一鉴权入口（Facade），是整条鉴权链路的最外层收口。
 *
 * <p>完整调用链路：
 * <pre>
 *   Controller → AuthzDecisionAppService → <b>AuthzFacade</b>
 *     → PEP（拦截 + 异常兜底）
 *       → PDP（加载策略 + 裁决）
 *         → PDP 按需调用 PIP（补全主体 / 资源 / 环境上下文）
 *       ← 返回决策
 *     ← PEP 执行决策（放行 / 拒绝 / 降级）
 * </pre>
 *
 * <p>AuthzFacade 本身不做业务判断，只负责：
 * <ol>
 *   <li>为请求补全 traceId（用于审计 & 日志串联）</li>
 *   <li>把请求委托给 PEP 开始鉴权流程</li>
 * </ol>
 */
@Slf4j
@Service
public class AuthzFacade {

    private static final String SUBJECT_TYPE_USER = "SUB_USER";

    private static final String RESOURCE_TYPE_DATA_BO = "RES_DATA_BO";

    private static final String FAIL_STRATEGY_ALLOW = "ALLOW";

    private static final String FAIL_STRATEGY_DENY = "DENY";

    private final PolicyEnforcementPoint policyEnforcementPoint;

    private final PermissionRepository permissionRepository;

    public AuthzFacade(PolicyEnforcementPoint policyEnforcementPoint,
                       PermissionRepository permissionRepository) {
        this.policyEnforcementPoint = policyEnforcementPoint;
        this.permissionRepository = permissionRepository;
    }

    /**
     * 治理增强鉴权入口，后续可在 PEP 前后接入委托授权、审计编排等横切逻辑。
     *
     * @param request 鉴权请求
     * @return 鉴权结果
     */
    public AuthzDecision checkWithGovernance(AuthzRequest request) {
        // 步骤 1：确保请求携带 traceId，便于全链路日志追踪
        ensureTraceId(request);
        log.debug("[鉴权Facade] 鉴权请求: traceId={}, tenantId={}, subjectId={}, resId={}, action={}",
            request.getTraceId(),
            request.getTenantId(),
            request.getSubject() == null ? null : request.getSubject().getId(),
            request.getResource() == null ? null : request.getResource().getResId(),
            request.getAction());
        // 步骤 2：委托给 PEP，启动鉴权流程
        AuthzDecision decision = policyEnforcementPoint.checkWithGovernance(request);
        log.debug("[鉴权Facade] 鉴权完成: traceId={}, decision={}",
            request.getTraceId(), decision == null ? null : decision.getDecision());
        return decision;
    }

    /**
     * 若调用方未传 traceId，则自动生成，保证审计日志可串联。
     */
    private void ensureTraceId(AuthzRequest request) {
        if (request.getTraceId() == null || request.getTraceId().trim().isEmpty()) {
            request.setTraceId(UUID.randomUUID().toString());
        }
    }

    /**
     * 基于权限项编码执行鉴权。
     *
     * <p>根据 permCode 查询 authz_permission_item 获取资源模型编码、资源 ID、操作编码，
     * 然后组装 AuthzRequest 委托给现有鉴权链路。
     *
     * <p><b>resId 与 instanceId 语义分离：</b>
     * <ul>
     *   <li>{@code resId}：始终取自 authz_permission_item.res_id，对应 authz_bo_meta_model 表主键，
     *       用于 PIP 定位业务对象模型和 resolver，不可被 instanceId 覆盖。</li>
     *   <li>{@code instanceId}：业务实例标识（如订单号、合同 ID），由调用方通过注解 SpEL 传入，
     *       放入 context["instanceId"] 供 BO Hook 按需加载实例属性。</li>
     * </ul>
     *
     * @param tenantId   租户 ID
     * @param appCode    应用编码
     * @param userId     用户 ID
     * @param permCode   权限项编码
     * @param instanceId 业务实例 ID（可选），放入 context 供 BO Hook 使用，不覆盖 resId
     * @return 鉴权结果
     */
    public AuthzDecision checkByPermCode(String tenantId, String appCode,
                                         String userId, String permCode,
                                         String instanceId) {
        log.info("[鉴权Facade] 按permCode鉴权: userId={}, tenantId={}, appCode={}, permCode={}, instanceId={}",
            userId, tenantId, appCode, permCode, instanceId);
        AuthPermissionItem item = permissionRepository
                .findPermissionItem(tenantId, appCode, permCode);

        if (item == null) {
            log.warn("权限项不存在，鉴权拒绝 permCode={}, tenantId={}, appCode={}",
                    permCode, tenantId, appCode);
            return buildDecisionMetadata(AuthzDecision.builder()
                    .decision(DecisionType.INDETERMINATE)
                    .reason("权限项不存在: " + permCode)
                    .build(), null, permCode);
        }

        // resId 始终取自权限项配置，指向 authz_bo_meta_model 主键，用于 PIP 定位业务对象模型
        String resId = normalizeResId(item.getResId());
        log.debug("[鉴权Facade] permCode解析完成: resModelCode={}, actCode={}, resId={}, instanceId={}",
            item.getResModelCode(), item.getActCode(), resId,
            instanceId == null ? "(无)" : instanceId);

        // instanceId 放入 context，与 resId 严格分离，不做相互回退
        Map<String, Object> context = new LinkedHashMap<>();
        if (instanceId != null && !instanceId.trim().isEmpty()) {
            context.put("instanceId", instanceId.trim());
            log.debug("[鉴权Facade] instanceId 已写入 context: instanceId={}", instanceId.trim());
        } else {
            log.debug("[鉴权Facade] 无 instanceId，BO Hook 将仅使用 resId 定位模型");
        }

        AuthzRequest request = AuthzRequest.builder()
                .tenantId(tenantId)
                .appCode(appCode)
                .subject(AuthzSubject.builder()
                        .id(userId)
                        .type(SUBJECT_TYPE_USER)
                        .build())
                .resource(AuthzResource.builder()
                        .resourceType(item.getResModelCode())
                        .resId(resId)
                        .build())
                .action(item.getActCode())
                .context(context)
                .build();

        return buildDecisionMetadata(checkWithGovernance(request), item, permCode);
    }

    private AuthzDecision buildDecisionMetadata(AuthzDecision decision,
                                                AuthPermissionItem item,
                                                String permCode) {
        if (decision == null) {
            return null;
        }
        Map<String, Object> obligations = decision.getObligations() == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(decision.getObligations());
        obligations.put("permCode", permCode);
        obligations.put("failStrategy", normalizeFailStrategy(item == null ? null : item.getFailStrategy()));
        decision.setObligations(obligations);
        return decision;
    }

    /**
     * 标准化权限项 resId：空值转为空字符串，非空值保留原值（即 authz_bo_meta_model 表主键 ID）。
     */
    private String normalizeResId(String resId) {
        if (resId == null || resId.trim().isEmpty()) {
            return "";
        }
        return resId.trim();
    }

    private String normalizeFailStrategy(String failStrategy) {
        if (failStrategy == null || failStrategy.trim().isEmpty()) {
            return FAIL_STRATEGY_DENY;
        }
        if (FAIL_STRATEGY_ALLOW.equalsIgnoreCase(failStrategy.trim())) {
            return FAIL_STRATEGY_ALLOW;
        }
        return FAIL_STRATEGY_DENY;
    }
}