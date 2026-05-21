package com.ruijie.authzengine.domain.model.decision;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一鉴权决策结果，承载 PDP 评估后的完整输出。
 * <p>
 * 包含决策结论、命中的授权记录明细和附带义务（obligations）。
 * 决策完成后由 AuthzFacade 写入 authz_audit_log 并回填 auditLogId。
 * </p>
 *
 * @see DecisionType
 * @see AuthzRequest
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthzDecision {

    /** 决策结论：PERMIT（允许）、NOT_PERMIT（拒绝）、INDETERMINATE（不确定）。 */
    private DecisionType decision;

    /** 决策原因说明，拒绝或不确定时包含具体原因文本。 */
    private String reason;

    /**
     * 命中的权限项编码列表。
      * <p>对应 authz_permission_item.perm_code，标识本次鉴权匹配到了哪些权限定义。</p>
     */
    private List<String> matchedPermissions;

    /**
     * 命中的授权分配记录 ID 列表。
      * <p>对应 authz_assignment.id，标识哪些授权记录支撑了本次 PERMIT 决策。</p>
     */
    private List<String> matchedAssignmentIds;

    /**
     * 命中的委托授权记录 ID 列表。
     * <p>当权限来源于委托关系时填充，便于审计追溯委托链。</p>
     */
    private List<String> matchedDelegateIds;

    /**
     * 命中的策略模板编码列表。
      * <p>对应 authz_assignment.policy_tpl_id 关联的策略模板编码。</p>
     */
    private List<String> matchedPolicyTemplateCodes;

    /**
     * 附带义务（obligations），PDP 评估后额外输出的执行指令。
     * <p>例如数据范围过滤 SQL、脱敏规则等，由 PEP 在放行后执行。</p>
     */
    private Map<String, Object> obligations;

    /**
     * 审计日志 ID，鉴权链路结束后回填。
      * <p>对应 authz_audit_log.id，前端或调用方可据此查询审计明细。</p>
     */
    private String auditLogId;

    /**
     * 构造允许结果。
     *
     * @param matchedPermissions 命中的权限项编码列表
     * @return 允许结果
     */
    public static AuthzDecision permit(List<String> matchedPermissions) {
        return permit(
            matchedPermissions,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()
        );
    }

    /**
     * 构造允许结果。
     *
     * @param matchedPermissions 命中的权限项编码列表
     * @param matchedAssignmentIds 命中的授权记录标识
     * @param matchedDelegateIds 命中的委托记录标识
     * @param matchedPolicyTemplateCodes 命中的策略模板编码
     * @return 允许结果
     */
    public static AuthzDecision permit(
        List<String> matchedPermissions,
        List<String> matchedAssignmentIds,
        List<String> matchedDelegateIds,
        List<String> matchedPolicyTemplateCodes
    ) {
        return permit(
            matchedPermissions,
            matchedAssignmentIds,
            matchedDelegateIds,
            matchedPolicyTemplateCodes,
            Collections.emptyMap()
        );
    }

    /**
     * 构造允许结果。
     *
     * @param matchedPermissions 命中的权限项编码列表
     * @param matchedAssignmentIds 命中的授权记录标识
     * @param matchedDelegateIds 命中的委托记录标识
     * @param matchedPolicyTemplateCodes 命中的策略模板编码
     * @param obligations PDP 输出的 obligations
     * @return 允许结果
     */
    public static AuthzDecision permit(
        List<String> matchedPermissions,
        List<String> matchedAssignmentIds,
        List<String> matchedDelegateIds,
        List<String> matchedPolicyTemplateCodes,
        Map<String, Object> obligations
    ) {
        return AuthzDecision.builder()
            .decision(DecisionType.PERMIT)
            .reason("PERMIT")
            .matchedPermissions(matchedPermissions)
            .matchedAssignmentIds(matchedAssignmentIds)
            .matchedDelegateIds(matchedDelegateIds)
            .matchedPolicyTemplateCodes(matchedPolicyTemplateCodes)
            .obligations(obligations == null ? Collections.<String, Object>emptyMap() : obligations)
            .build();
    }

    /**
     * 构造拒绝结果。
     *
     * @param reason 拒绝原因
     * @return 拒绝结果
     */
    public static AuthzDecision notPermit(String reason) {
        return AuthzDecision.builder()
            .decision(DecisionType.NOT_PERMIT)
            .reason(reason)
            .matchedPermissions(Collections.emptyList())
            .matchedAssignmentIds(Collections.emptyList())
            .matchedDelegateIds(Collections.emptyList())
            .matchedPolicyTemplateCodes(Collections.emptyList())
            .obligations(Collections.emptyMap())
            .build();
    }

    /**
     * 构造不确定结果。
     *
     * @param reason 不确定原因
     * @return 不确定结果
     */
    public static AuthzDecision indeterminate(String reason) {
        return AuthzDecision.builder()
            .decision(DecisionType.INDETERMINATE)
            .reason(reason)
            .matchedPermissions(Collections.emptyList())
            .matchedAssignmentIds(Collections.emptyList())
            .matchedDelegateIds(Collections.emptyList())
            .matchedPolicyTemplateCodes(Collections.emptyList())
            .obligations(Collections.emptyMap())
            .build();
    }
}