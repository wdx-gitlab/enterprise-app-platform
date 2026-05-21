package com.ruijie.authzengine.application.sdk.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 主体关系创建命令。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSubjectRelationCommand {

    private String tenantId;

    private String appCode;

    @Builder.Default
    private GovernanceSubjectModel subjectModel = GovernanceSubjectModel.SUB_USER;

    private String subjectId;

    private GovernanceSubjectModel relatedSubjectModel;

    private String relatedSubjectId;

    private GovernanceRelationType relationType;
}