package com.ruijie.authzengine.domain.model.decision;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 鉴权主体，描述"谁"在发起授权请求。
 * <p>
 * 对应 authz_assignment 中的 subject_id / subject_model 维度。
 * PIP 阶段会根据主体信息补全角色、组织、岗位等关联身份集合（SubjectKey）。
 * </p>
 *
 * @see AuthzRequest
 * @see AuthzContext#getSubjectKeys()
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthzSubject {

    /**
     * 主体唯一标识，通常为用户 ID。
     * <p>对应 authz_assignment.subject_id。</p>
     */
    private String id;

    /**
     * 主体类型，如 SUB_USER、SUB_ROLE、SUB_ORG 等。
     * <p>对应 authz_assignment.subject_model，与 authz_meta_model.category 中的主体类别对齐。</p>
     */
    private String type;
}