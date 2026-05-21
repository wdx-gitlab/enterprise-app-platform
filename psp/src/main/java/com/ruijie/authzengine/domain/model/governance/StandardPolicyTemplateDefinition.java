package com.ruijie.authzengine.domain.model.governance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 标准策略模板定义，对应 authz_std_pol_template 表的一行记录。
 * <p>
 * 策略模板是可复用的权限约束规则，通过 authz_assignment.policy_tpl_id 绑定到具体授权记录。
 * PDP 评估时会执行 expressionScript 表达式，并将 paramSchema 作为参数校验依据。
 * </p>
 *
 * @see com.ruijie.authzengine.domain.model.common.PolicyTemplateType
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StandardPolicyTemplateDefinition {

    /** 主键。 */
    private Long id;

    /** 租户标识，全局模板用 __GLOBAL__。 */
    private String tenantId;

    /** 模板编码，唯一约束 uk_authz_std_pol_template(tenant_id, template_code)，如 DATA_SCOPE_DEPT。 */
    private String templateCode;

    /** 模板名称（显示用），如"按部门数据范围"。 */
    private String templateName;

    /** 策略类型，取值参照 PolicyTemplateType 枚举（DATA / ENV / STATE / FIELD）。 */
    private String polType;

    /**
     * 表达式脚本，PDP 评估时执行。
     * <p>示例："env.hour >= 9 && env.hour <= 18"、"res.status == \"ACTIVE\""。</p>
     */
    private String expressionScript;

    /** 参数 Schema（JSON），用于前端渲染参数表单和后端参数校验。 */
    private String paramSchema;

    /** 状态：ENABLED / DISABLED。 */
    private String status;
}