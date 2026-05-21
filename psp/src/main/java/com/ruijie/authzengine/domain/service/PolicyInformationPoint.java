package com.ruijie.authzengine.domain.service;

import com.ruijie.authzengine.domain.model.decision.AuthzContext;
import com.ruijie.authzengine.domain.model.decision.AuthzRequest;
import com.ruijie.authzengine.domain.model.common.SubjectKey;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * PIP（Policy Information Point）—— 策略信息点。
 *
 * <p>职责：被 PDP 按需调用，为决策过程提供所需的属性和上下文。
 * <ul>
 *   <li>加载并展开主体（用户 → 角色、组织、岗位、用户组）</li>
 *   <li>加载治理属性（主体注册状态、资源注册状态、动作标准化编码）</li>
 *   <li>加载委托授权记录标识</li>
 * </ul>
 *
 * <p>在整条链路中的位置：AuthzFacade → PEP → PDP → <b>PDP 按需调 PIP</b>
 */
public interface PolicyInformationPoint {

    /**
     * 加载鉴权上下文。
     *
     * @param request 鉴权请求
     * @return 上下文对象
     */
    AuthzContext loadContext(AuthzRequest request);

    /**
     * 为治理骨架保留的额外主数据与资源注册信息扩展口。
     *
     * @param request 鉴权请求
     * @return 治理扩展属性
     */
    default Map<String, Object> loadGovernanceAttributes(AuthzRequest request) {
        return Collections.emptyMap();
    }

    /**
     * 为委托授权扩展预留的命中记录装载口。
     *
     * @param request 鉴权请求
     * @return 委托记录标识集合
     */
    default Set<String> loadDelegationIds(AuthzRequest request) {
        return Collections.emptySet();
    }

    /**
     * 基于已展开主体加载委托记录标识。
     *
     * @param request 鉴权请求
     * @param subjectKeys 已展开主体集合
     * @return 委托记录标识集合
     */
    default Set<String> loadDelegationIds(AuthzRequest request, Set<SubjectKey> subjectKeys) {
        return loadDelegationIds(request);
    }
}