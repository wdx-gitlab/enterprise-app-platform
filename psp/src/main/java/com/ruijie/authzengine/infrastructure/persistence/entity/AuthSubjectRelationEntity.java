package com.ruijie.authzengine.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 涓讳綋鍏崇郴鎸佷箙鍖栧疄浣撱€?
 */
@Data
@TableName("authz_subject_relation")
@EqualsAndHashCode(callSuper = true)
public class AuthSubjectRelationEntity extends BaseEntity {

    @TableField("tenant_id")
    private String tenantId;

    @TableField("app_code")
    private String appCode;

    @TableField("subject_model")
    private String subjectModel;

    @TableField("subject_id")
    private String subjectId;

    @TableField("related_subject_model")
    private String relatedSubjectModel;

    @TableField("related_subject_id")
    private String relatedSubjectId;

    @TableField("relation_type")
    private String relationType;
}