package com.ruijie.authzengine.domain.model.governance.subject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 主体关系定义，对应 authz_subject_relation 表的一行记录。
 * <p>
 * 记录主体之间的关联关系，如用户→角色（ROLE）、用户→组织（ORG）、用户→岗位（POSITION）、用户→用户组（GROUP）。
 * PIP 阶段通过查询本表将用户的所有间接身份展开为 SubjectKey 集合，
 * 使 PDP 能匹配以角色、组织等为 subject_model 的授权记录。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthSubjectRelation {

    /** 主键。 */
    private Long id;

    /** 租户标识。 */
    private String tenantId;

    /** 应用标识。 */
    private String appCode;

    /** 源主体模型，通常为 SUB_USER。 */
    private String subjectModel;

    /** 源主体标识，如用户 ID。 */
    private String subjectId;

    /** 关联目标主体模型，如 SUB_ROLE、SUB_ORG、SUB_POSITION、SUB_GROUP。 */
    private String relatedSubjectModel;

    /** 关联目标主体标识，如角色编码、组织编码。 */
    private String relatedSubjectId;

    /** 关系类型：ROLE / ORG / POSITION / GROUP。 */
    private String relationType;
}