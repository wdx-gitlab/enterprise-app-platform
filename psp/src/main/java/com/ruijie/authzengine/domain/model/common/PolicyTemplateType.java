package com.ruijie.authzengine.domain.model.common;

/**
 * 策略模板类型，对应 authz_std_pol_template.pol_type 字段。
 * <p>
 * 不同类型的策略模板在 PDP 评估阶段通过不同的表达式引擎/规则逻辑执行。
 * </p>
 *
 * @see com.ruijie.authzengine.domain.model.governance.StandardPolicyTemplateDefinition
 */
public enum PolicyTemplateType {

    /** 数据策略：基于数据范围的过滤条件，如"只看本部门数据"。 */
    DATA,

    /** 环境策略：基于运行时环境的约束条件，如"仅工作时间可操作"。 */
    ENV,

    /** 状态策略：基于资源状态的约束条件，如"仅有效状态可操作"。 */
    STATE,

    /** 字段策略：基于字段级控制指令生成 obligations，如 OPEN/MASK/HIDE。 */
    FIELD
}