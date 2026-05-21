package com.ruijie.authzengine.infrastructure.authz;

import com.ruijie.authzengine.domain.model.decision.AuthzDecision;
import com.ruijie.authzengine.domain.model.decision.AuthzRequest;
import com.ruijie.authzengine.domain.service.PolicyDecisionPoint;
import com.ruijie.authzengine.domain.service.PolicyEnforcementPoint;
import com.ruijie.authzengine.shared.exception.AuthzIntegrationException;
import com.ruijie.authzengine.shared.exception.BusinessException;
import com.ruijie.authzengine.shared.exception.ErrorCode;
import com.ruijie.authzengine.shared.exception.SystemException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * PEP（策略执行点）默认实现。
 *
 * <p>核心流程：
 * <ol>
 *   <li>接收鉴权请求</li>
 *   <li>委托给 PDP 做决策（PDP 内部会按需调用 PIP 补全上下文）</li>
 *   <li>统一异常兜底：业务异常透传，集成异常降级为 INDETERMINATE，其他异常包装为 SystemException</li>
 * </ol>
 *
 * <p>注意：PEP 不直接依赖 PIP，只依赖 PDP，符合标准 XACML 链路。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultPolicyEnforcementPoint implements PolicyEnforcementPoint {

    /** PDP：策略决策点，PEP 唯一依赖 */
    private final PolicyDecisionPoint policyDecisionPoint;

    @Override
    public AuthzDecision checkWithGovernance(AuthzRequest request) {
        try {
            // 核心：委托 PDP 做决策，PDP 内部会按需调 PIP
            return policyDecisionPoint.decideWithGovernance(request);
        } catch (BusinessException | SystemException exception) {
            // 业务异常 / 系统异常直接透传，由全局异常处理器统一响应
            throw exception;
        } catch (AuthzIntegrationException exception) {
            // 外部依赖异常（如 PIP 调用 Hook 失败）降级为 INDETERMINATE，不抛出
            log.error("PEP 执行过程中命中外部依赖异常", exception);
            return AuthzDecision.indeterminate("HOOK_ERROR");
        } catch (Exception exception) {
            // 未预期异常包装为 SystemException，确保不会泄漏原始异常栈
            log.error("PEP 执行过程中发生未预期异常", exception);
            throw new SystemException(ErrorCode.SYSTEM_ERROR, "鉴权执行失败", exception);
        }
    }
}