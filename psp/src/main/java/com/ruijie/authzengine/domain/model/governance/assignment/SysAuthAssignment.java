package com.ruijie.authzengine.domain.model.governance.assignment;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 授权分配骨架领域对象，对应 authz_assignment 表的一行记录。
 * <p>
 * 授权分配是权限引擎的核心数据，建立"主体→权限项"的映射关系。
 * PDP 通过联合查询 authz_permission_item + authz_assignment 得到 PermissionGrant，
 * 再与 AuthzRequest 进行三元匹配。
 * 可选绑定策略模板（policyTplId + policyParams）和过期时间（expireTime）。
 * </p>
 *
 * @see com.ruijie.authzengine.domain.model.governance.permission.AuthPermissionItem
 * @see com.ruijie.authzengine.domain.model.decision.PermissionGrant
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SysAuthAssignment {

    /** 主键。 */
    private Long id;

    /** 租户标识。 */
    private String tenantId;

    /** 应用标识。 */
    private String appCode;

    /** 主体模型编码，如 SUB_USER、SUB_ROLE，取值参照 SubjectModelCode。 */
    private String subjectModel;

    /** 主体标识，如用户 ID、角色编码。 */
    private String subjectId;

    /** 关联权限项 ID，对应 authz_permission_item.id。 */
    private Long permItemId;

    /** 策略模板 ID，可为空，关联 authz_std_pol_template.id。绑定后 PDP 会额外评估策略表达式。 */
    private Long policyTplId;

    /** 策略参数（JSON），与 policyTplId 配合使用，传递给策略表达式评估上下文。 */
    private String policyParams;

    /** 授权过期时间，为空表示永不过期。 */
    private LocalDateTime expireTime;
}