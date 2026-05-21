package com.ruijie.authzengine.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 鎺堟潈鍒嗛厤鎸佷箙鍖栧疄浣撱€?
 */
@Data
@TableName("authz_assignment")
@EqualsAndHashCode(callSuper = true)
public class SysAuthAssignmentEntity extends BaseEntity {

    @TableField("tenant_id")
    private String tenantId;

    @TableField("app_code")
    private String appCode;

    @TableField("subject_id")
    private String subjectId;

    @TableField("subject_model")
    private String subjectModel;

    @TableField("perm_item_id")
    private Long permItemId;

    @TableField(value = "policy_tpl_id", updateStrategy = FieldStrategy.IGNORED)
    private Long policyTplId;

    @TableField(value = "policy_params", updateStrategy = FieldStrategy.IGNORED)
    private String policyParams;

    @TableField(value = "expire_time", updateStrategy = FieldStrategy.IGNORED)
    private LocalDateTime expireTime;
}