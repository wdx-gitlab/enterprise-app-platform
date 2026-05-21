package com.ruijie.authzengine.domain.model.decision;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * PDP 匹配阶段的最小授权记录模型，表示一条"已展开"的有效授权。
 * <p>
 * 由 PDP 从 authz_assignment + authz_permission_item 联合查询后组装，
 * 包含主体、资源、动作三元组以及授权来源（委托、策略模板）等审计信息。
 * PDP 通过逐条比对 PermissionGrant 与 AuthzRequest 来得出 PERMIT / NOT_PERMIT 结论。
 * </p>
 *
 * @see AuthzDecision
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionGrant {

    /** 授权分配记录 ID，对应 authz_assignment.id。 */
    private Long assignmentId;


    /** 租户标识。 */
    private String tenantId;

    /** 应用标识。 */
    private String appCode;

    /** 主体类型，如 SUB_USER、SUB_ROLE 等，对应 authz_assignment.subject_model。 */
    private String subjectType;

    /** 主体标识，对应 authz_assignment.subject_id。 */
    private String subjectId;

    
    /** 动作编码，对应 authz_permission_item.act_code。 */
    private String action;

     /** 权限项 ID，对应 authz_permission_item.id / authz_assignment.perm_item_id。 */
    private Long permItemId;

    /** 权限项编码，对应 authz_permission_item.perm_code。 */
    private String permissionCode;

    /** 资源大类，如 RES_UI_MENU、RES_API、RES_DATA_BO 等。对应 authz_permission_item.res_model_code。 */
    private String resourceType;

    /**
     * 资源标识，对应资源表主键 ID。空表示类别级权限。
     */
    private String resId;

    /** 委托授权来源 ID，为空表示非委托获得的权限。 */
    private String delegateId;

    /** 策略模板编码，对应 authz_assignment.policy_tpl_id 关联的模板。 */
    private String policyTemplateCode;

    // ── 策略模板运行时字段（由 DatabaseAuthorizationPolicyRepository 加载） ──

    /** 策略模板 ID，对应 authz_std_pol_template.id。 */
    private Long policyTemplateId;

    /** 策略模板状态，ENABLED 才参与运行时评估。 */
    private String policyTemplateStatus;

    /** 策略模板类型，如 ENV（环境条件）、STATE（状态条件）等。 */
    private String policyTemplateType;

    /** 策略模板表达式脚本，SpEL 表达式，运行时评估返回布尔值。 */
    private String expressionScript;

    /** 策略模板参数 schema（JSON），描述模板期望的参数声明。 */
    private String paramSchema;

    /** 授权分配绑定的策略参数（JSON），对应 authz_assignment.policy_params。 */
    private String policyParams;

    /** 权限项失败策略，对应 authz_permission_item.fail_strategy。 */
    private String failStrategy;
}