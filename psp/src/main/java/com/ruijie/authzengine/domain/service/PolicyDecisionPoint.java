package com.ruijie.authzengine.domain.service;

import com.ruijie.authzengine.domain.model.decision.AuthzDecision;
import com.ruijie.authzengine.domain.model.decision.AuthzRequest;

/**
 * PDP（Policy Decision Point）—— 策略决策点。
 *
 * <p>职责：
 * <ul>
 *   <li>按需调用 PIP 补全主体 / 资源 / 环境上下文</li>
 *   <li>加载适用的策略规则（从 PAP 或缓存）</li>
 *   <li>逐条评估规则，必要时短路返回</li>
 *   <li>冒突裁决后生成最终决策（PERMIT / NOT_PERMIT / INDETERMINATE）</li>
 * </ul>
 *
 * <p>在整条链路中的位置：AuthzFacade → PEP → <b>PDP</b> → (PDP 按需调 PIP)
 */
public interface PolicyDecisionPoint {

    /**
     * 执行鉴权决策，PDP 内部按需调用 PIP 补全上下文。
     *
     * @param request 鉴权请求
     * @return 鉴权结果
     */
    AuthzDecision decideWithGovernance(AuthzRequest request);
}