package com.ruijie.authzengine.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 鉴权审计日志持久化实体。
 */
@Data
@TableName("authz_audit_log")
@EqualsAndHashCode(callSuper = true)
public class SysAuthzAuditLogEntity extends BaseEntity {

    @TableField("tenant_id")
    private String tenantId;

    @TableField("app_code")
    private String appCode;

    @TableField("request_id")
    private String requestId;

    @TableField("subject_model")
    private String subjectModel;

    @TableField("subject_id")
    private String subjectId;

    @TableField("resource_model")
    private String resourceModel;

    @TableField("res_id")
    private String resId;

    @TableField("action_code")
    private String actionCode;

    @TableField("decision")
    private String decision;

    @TableField("matched_permission_codes")
    private String matchedPermissionCodes;

    @TableField("matched_assignment_ids")
    private String matchedAssignmentIds;

    @TableField("matched_delegate_ids")
    private String matchedDelegateIds;

    @TableField("matched_policy_template_codes")
    private String matchedPolicyTemplateCodes;

    @TableField("failure_reason")
    private String failureReason;

    @TableField("cost_ms")
    private Long costMs;

    @TableField("hook_status")
    private String hookStatus;

    @TableField("hook_cost_ms")
    private Long hookCostMs;

    @TableField("attribute_snapshot")
    private String attributeSnapshot;
}