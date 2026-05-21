package com.ruijie.authzengine.domain.model.common;

/**
 * 标准主体模型编码，与 authz_meta_model.model_code（category=SUBJECT）一一对应。
 * <p>
 * 枚举值决定 authz_assignment.subject_model 和 authz_subject_relation 中的身份维度。
 * PIP 阶段根据这些模型编码从不同主数据表（sys_user / sys_org / authz_position / authz_usergroup / authz_role）
 * 补全主体身份集合 SubjectKey。
 * </p>
 *
 * @see com.ruijie.authzengine.domain.model.common.SubjectKey
 */
public enum SubjectModelCode {

    /** 用户主体，对应 sys_user 表。 */
    SUB_USER,

    /** 组织主体，对应 sys_org 表。 */
    SUB_ORG,

    /** 岗位主体，对应 authz_position 表。 */
    SUB_POSITION,

    /** 用户组主体，对应 authz_usergroup 表。 */
    SUB_GROUP,

    /** 角色主体，对应 authz_role 表。 */
    SUB_ROLE
}