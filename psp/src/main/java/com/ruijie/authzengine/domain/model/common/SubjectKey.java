package com.ruijie.authzengine.domain.model.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 主体标识键，承载主体类型与主体 ID 的二元组。
 * <p>
 * PIP 阶段根据 AuthzRequest.subject 和 authz_subject_relation 表补全所有间接身份，
 * 每条身份以 SubjectKey 表示，最终汇入 AuthzContext.subjectKeys 供 PDP 匹配。
 * </p>
 * <p>示例：用户 demo-user 可能对应 {SUB_USER, demo-user}、{SUB_ROLE, contract-admin}、{SUB_ORG, ORG-ROOT} 等多条 SubjectKey。</p>
 *
 * @see com.ruijie.authzengine.domain.model.decision.AuthzContext#getSubjectKeys()
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubjectKey {

    /** 主体模型编码，取值参照 SubjectModelCode 枚举（如 SUB_USER、SUB_ROLE）。 */
    private String subjectType;

    /** 主体标识值，如用户 ID、角色编码、组织编码等，对应各主数据表的业务编码字段。 */
    private String subjectId;
}