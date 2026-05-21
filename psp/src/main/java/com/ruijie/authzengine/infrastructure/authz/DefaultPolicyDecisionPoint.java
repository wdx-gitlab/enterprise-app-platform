package com.ruijie.authzengine.infrastructure.authz;

import com.ruijie.authzengine.domain.model.decision.AuthzContext;
import com.ruijie.authzengine.domain.model.decision.AuthzDecision;
import com.ruijie.authzengine.domain.model.decision.AuthzRequest;
import com.ruijie.authzengine.domain.model.decision.DecisionType;
import com.ruijie.authzengine.domain.model.decision.PermissionGrant;
import com.ruijie.authzengine.domain.repository.AuthorizationPolicyRepository;
import com.ruijie.authzengine.domain.service.PermissionDecisionService;
import com.ruijie.authzengine.domain.service.PolicyDecisionPoint;
import com.ruijie.authzengine.domain.service.PolicyInformationPoint;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * PDP（策略决策点）默认实现，当前支持 RBAC 级资源和动作命中逻辑。
 *
 * <p>核心流程：
 * <ol>
 *   <li>按需调用 PIP 补全上下文（主体展开、治理属性、委托记录）</li>
 *   <li>根据展开后的主体集合，从授权策略仓库加载命中的授权记录</li>
 *   <li>委托 {@link PermissionDecisionService} 做最终的规则匹配与决策归并</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultPolicyDecisionPoint implements PolicyDecisionPoint {

    /** PIP：策略信息点，负责补全主体、资源、环境上下文 */
    private final PolicyInformationPoint policyInformationPoint;

    /** 授权策略仓库，根据主体集合查询命中的授权记录 */
    private final AuthorizationPolicyRepository authorizationPolicyRepository;

    /** 权限决策领域服务，负责规则匹配与决策归并 */
    private final PermissionDecisionService permissionDecisionService;

    @Override
    public AuthzDecision decideWithGovernance(AuthzRequest request) {
        // 步骤 1：按需调用 PIP 补全上下文（主体展开、角色/组织归并、治理属性加载、委托记录查询）
        AuthzContext context = policyInformationPoint.loadContext(request);
        propagateNormalizedActionCode(request, context);

        // 步骤 2：根据展开后的主体集合，查询所有命中的授权记录（PermissionGrant）
        List<PermissionGrant> grants = authorizationPolicyRepository.findBySubjects(
            request.getTenantId(),
            request.getAppCode(),
            context.getSubjectKeys(),
            context
        );
        if (grants.isEmpty()) {
            log.info("PDP 未命中授权记录 tenantId={}, appCode={}", request.getTenantId(), request.getAppCode());
        }

        // 步骤 3：委托领域服务做最终的规则匹配与决策归并
        AuthzDecision decision = permissionDecisionService.evaluate(request, context, grants);
        if (DecisionType.PERMIT.equals(decision.getDecision())) {
            log.info("PDP 命中权限项 count={}", decision.getMatchedPermissions().size());
        }
        return decision;
    }

    private void propagateNormalizedActionCode(AuthzRequest request, AuthzContext context) {
        if (context == null || context.getGovernanceAttributes() == null) {
            return;
        }
        Object normalizedActionCode = context.getGovernanceAttributes().get("normalizedActionCode");
        if (normalizedActionCode == null || !StringUtils.hasText(String.valueOf(normalizedActionCode))) {
            return;
        }
        Map<String, Object> requestContext = request.getContext() == null
            ? new LinkedHashMap<>()
            : new LinkedHashMap<>(request.getContext());
        requestContext.put("normalizedActionCode", String.valueOf(normalizedActionCode).trim());
        request.setContext(requestContext);
    }
}