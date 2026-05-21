package com.ruijie.authzengine.domain.model.decision;

/**
 * 鉴权决策结果类型，参考 XACML 决策三态模型。
 *
 * @see AuthzDecision
 */
public enum DecisionType {

    /** 允许：主体对目标资源的操作被授权。 */
    PERMIT,

    /** 拒绝：主体对目标资源的操作未被授权。 */
    NOT_PERMIT,

    /**
     * 不确定：无法做出明确判断。
     * <p>常见场景：权限项不存在、PIP 信息补全失败、策略评估异常等。</p>
     */
    INDETERMINATE
}