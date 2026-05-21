package com.ruijie.authzengine.application.sdk.model;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 授权分配创建命令。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAssignmentCommand {

    private String tenantId;

    private String appCode;

    @Builder.Default
    private GovernanceSubjectModel subjectModel = GovernanceSubjectModel.SUB_USER;

    private String subjectId;

    private Long permItemId;

    @Builder.Default
    private AssignmentBindingMode bindingMode = AssignmentBindingMode.DIRECT;

    private String policyTemplateCode;

    private Map<String, Object> policyParams;

    private LocalDateTime expireTime;
}