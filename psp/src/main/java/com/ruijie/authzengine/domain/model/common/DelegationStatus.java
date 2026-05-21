package com.ruijie.authzengine.domain.model.common;

/**
 * 委托授权状态，对应 authz_assignment_delegate.status 字段。
 * <p>
 * 委托授权允许主体 A 将自己的部分权限临时委托给主体 B，
 * 引擎仅在状态为 ACTIVE 且在有效期内时将委托权限纳入 PDP 匹配范围。
 * </p>
 *
 * @see com.ruijie.authzengine.domain.model.ops.AssignmentDelegate
 */
public enum DelegationStatus {

    /** 活跃：委托关系有效，被委托主体可行使对应权限。 */
    ACTIVE,

    /** 已撤销：委托方主动撤销，被委托方不再拥有该权限。 */
    REVOKED,

    /** 已过期：超过 end_time，自动失效。 */
    EXPIRED
}