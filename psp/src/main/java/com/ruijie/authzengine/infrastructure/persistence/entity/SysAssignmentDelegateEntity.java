package com.ruijie.authzengine.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 委托授权持久化实体。
 */
@Data
@TableName("authz_assignment_delegate")
@EqualsAndHashCode(callSuper = true)
public class SysAssignmentDelegateEntity extends BaseEntity {

    @TableField("tenant_id")
    private String tenantId;

    @TableField("app_code")
    private String appCode;

    @TableField("grantor_subject_model")
    private String grantorSubjectModel;

    @TableField("grantor_subject_id")
    private String grantorSubjectId;

    @TableField("delegate_subject_model")
    private String delegateSubjectModel;

    @TableField("delegate_subject_id")
    private String delegateSubjectId;

    @TableField("perm_item_id")
    private Long permItemId;

    @TableField("start_time")
    private LocalDateTime startTime;

    @TableField("end_time")
    private LocalDateTime endTime;

    @TableField("status")
    private String status;

    @TableField("reason")
    private String reason;
}