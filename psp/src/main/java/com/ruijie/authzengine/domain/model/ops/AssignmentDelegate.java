package com.ruijie.authzengine.domain.model.ops;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 委托授权领域对象，对应 authz_assignment_delegate 表的一行记录。
 * <p>
 * 表示主体 A（grantor）将某权限项在指定时间窗口内委托给主体 B（delegate）。
 * PIP 阶段查询 ACTIVE 状态且在有效期内的委托记录，
 * 将被委托主体的权限纳入 PDP 匹配范围，
 * 命中结果记录在 AuthzDecision.matchedDelegateIds 中。
 * </p>
 *
 * @see com.ruijie.authzengine.domain.model.common.DelegationStatus
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentDelegate {

    /** 委托记录主键，对应 authz_assignment_delegate.id。 */
    private Long delegationId;

    /** 租户标识。 */
    private String tenantId;

    /** 应用标识。 */
    private String appCode;

    /** 委托方（授权人）主体模型，如 SUB_USER。 */
    private String grantorSubjectModel;

    /** 委托方主体标识。 */
    private String grantorSubjectId;

    /** 被委托方主体模型，如 SUB_USER。 */
    private String delegateSubjectModel;

    /** 被委托方主体标识。 */
    private String delegateSubjectId;

    /** 委托的权限项编码，对应 authz_permission_item.perm_code。 */
    private String permissionCode;

    /** 委托生效时间，对应 authz_assignment_delegate.start_time。 */
    private LocalDateTime startTime;

    /** 委托失效时间，对应 authz_assignment_delegate.end_time。 */
    private LocalDateTime endTime;

    /** 委托状态，取值参照 DelegationStatus 枚举（ACTIVE / REVOKED / EXPIRED）。 */
    private String status;

    /** 委托原因说明。 */
    private String reason;
}