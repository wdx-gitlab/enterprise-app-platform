package com.ruijie.authzengine.domain.service;

import com.ruijie.authzengine.domain.model.decision.AuthzDecision;
import com.ruijie.authzengine.domain.model.decision.AuthzRequest;

/**
 * PEP（Policy Enforcement Point）—— 策略执行点。
 *
 * <p>职责：
 * <ul>
 *   <li>拦截鉴权请求，委托给 PDP 做决策</li>
 *   <li>统一异常兜底：业务异常透传、集成异常降级为 INDETERMINATE、未知异常包装为 SystemException</li>
 *   <li>执行决策结果（当前直接返回，后续可扩展为附加过滤条件、审计推送等）</li>
 * </ul>
 *
 * <p>在整条链路中的位置：AuthzFacade → <b>PEP</b> → PDP → (PIP)
 */
public interface PolicyEnforcementPoint {

    /**
     * 治理增强鉴权入口，在此处理 traceId 补全、审计编排等横切逻辑。
     *
     * @param request 鉴权请求
     * @return 鉴权结果
     */
    AuthzDecision checkWithGovernance(AuthzRequest request);
}