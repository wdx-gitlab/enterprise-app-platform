package com.ruijie.authzengine.domain.model.ops;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 鉴权审计记录领域对象，对应 authz_audit_log 表的一行记录。
 * <p>
 * 每次鉴权链路执行完毕后，由 AuthzFacade 将决策过程写入审计日志，
 * 记录主体、资源、动作、决策结果以及命中明细。
 * V4 迁移后增加了 Hook 执行跟踪字段（hookStatus、hookCostMs、attributeSnapshot）。
 * </p>
 *
 * @see com.ruijie.authzengine.domain.model.decision.AuthzDecision
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthzAuditRecord {

    /** 审计日志主键，对应 authz_audit_log.id。 */
    private Long auditLogId;

    /** 请求唯一标识，对应 authz_audit_log.request_id，与 AuthzRequest.traceId 保持一致。 */
    private String requestId;

    /** 租户标识。 */
    private String tenantId;

    /** 应用标识。 */
    private String appCode;

    /** 主体模型编码，对应 authz_audit_log.subject_model。 */
    private String subjectModel;

    /** 主体标识值，对应 authz_audit_log.subject_id。 */
    private String subjectId;

    /** 资源模型编码，对应 authz_audit_log.resource_model。 */
    private String resourceModel;

    /** 资源标识，对应 authz_audit_log.res_id。 */
    private String resId;

    /** 动作编码，对应 authz_audit_log.action_code。 */
    private String actionCode;

    /** 决策结果，值为 DecisionType.name()：PERMIT / NOT_PERMIT / INDETERMINATE。 */
    private String decision;

    /** 命中的权限项编码列表，对应 authz_audit_log.matched_permission_codes（JSON 存储）。 */
    private List<String> matchedPermissionCodes;

    /** 命中的授权分配记录 ID，对应 authz_audit_log.matched_assignment_ids。 */
    private List<String> matchedAssignmentIds;

    /** 命中的委托记录 ID，对应 authz_audit_log.matched_delegate_ids。 */
    private List<String> matchedDelegateIds;

    /** 命中的策略模板编码，对应 authz_audit_log.matched_policy_template_codes。 */
    private List<String> matchedPolicyTemplateCodes;

    /** 拒绝/不确定时的失败原因，对应 authz_audit_log.failure_reason。 */
    private String failureReason;

    /** 鉴权链路总耗时（毫秒），对应 authz_audit_log.cost_ms。 */
    private Long costMs;

    /** Hook 执行状态（对应 HookExecutionStatus.name()，可为 null） */
    private String hookStatus;

    /** Hook 执行耗时（毫秒，可为 null） */
    private Long hookCostMs;

    /** Hook 返回属性快照的 JSON 字符串（最长 2000 字符，可为 null） */
    private String attributeSnapshot;
}