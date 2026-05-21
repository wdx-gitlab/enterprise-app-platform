package com.ruijie.authzengine.api.assembler;

import com.ruijie.authzengine.api.dto.request.DelegationCreateRequest;
import com.ruijie.authzengine.api.dto.response.AuditLogItemResponse;
import com.ruijie.authzengine.api.dto.response.AuditLogPageResponse;
import com.ruijie.authzengine.api.dto.response.DelegationResponse;
import com.ruijie.authzengine.domain.model.ops.AssignmentDelegate;
import com.ruijie.authzengine.domain.model.ops.AuthzAuditPage;
import com.ruijie.authzengine.domain.model.ops.AuthzAuditRecord;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 委托授权与审计接口装配器。
 */
@Component
public class OpsAssembler {

    /**
     * 委托创建请求转领域对象。
     *
     * @param request 委托创建请求
     * @return 领域对象
     */
    public AssignmentDelegate toDefinition(DelegationCreateRequest request) {
        return AssignmentDelegate.builder()
            .tenantId(request.getTenantId())
            .appCode(request.getAppCode())
            .grantorSubjectModel(request.getGrantorSubjectModel())
            .grantorSubjectId(request.getGrantorSubjectId())
            .delegateSubjectModel(request.getDelegateSubjectModel())
            .delegateSubjectId(request.getDelegateSubjectId())
            .permissionCode(request.getPermissionCode())
            .startTime(request.getStartTime())
            .endTime(request.getEndTime())
            .reason(request.getReason())
            .build();
    }

    /**
     * 委托领域对象转响应。
     *
     * @param assignmentDelegate 委托领域对象
     * @return 委托响应
     */
    public DelegationResponse toResponse(AssignmentDelegate assignmentDelegate) {
        return DelegationResponse.builder()
            .delegationId(assignmentDelegate.getDelegationId())
            .tenantId(assignmentDelegate.getTenantId())
            .appCode(assignmentDelegate.getAppCode())
            .grantorSubjectModel(assignmentDelegate.getGrantorSubjectModel())
            .grantorSubjectId(assignmentDelegate.getGrantorSubjectId())
            .delegateSubjectModel(assignmentDelegate.getDelegateSubjectModel())
            .delegateSubjectId(assignmentDelegate.getDelegateSubjectId())
            .permissionCode(assignmentDelegate.getPermissionCode())
            .startTime(assignmentDelegate.getStartTime())
            .endTime(assignmentDelegate.getEndTime())
            .status(assignmentDelegate.getStatus())
            .reason(assignmentDelegate.getReason())
            .build();
    }

    /**
     * 审计分页领域对象转响应。
     *
     * @param authzAuditPage 审计分页结果
     * @return 审计分页响应
     */
    public AuditLogPageResponse toResponse(AuthzAuditPage authzAuditPage) {
        return AuditLogPageResponse.builder()
            .records(authzAuditPage.getRecords().stream().map(this::toResponse).collect(Collectors.toList()))
            .pageNo(authzAuditPage.getPageNo())
            .pageSize(authzAuditPage.getPageSize())
            .total(authzAuditPage.getTotal())
            .build();
    }

    private AuditLogItemResponse toResponse(AuthzAuditRecord authzAuditRecord) {
        return AuditLogItemResponse.builder()
            .auditLogId(authzAuditRecord.getAuditLogId())
            .requestId(authzAuditRecord.getRequestId())
            .subjectId(authzAuditRecord.getSubjectId())
            .subjectModel(authzAuditRecord.getSubjectModel())
            .resId(authzAuditRecord.getResId())
            .resourceModel(authzAuditRecord.getResourceModel())
            .actionCode(authzAuditRecord.getActionCode())
            .decision(authzAuditRecord.getDecision())
            .failureReason(authzAuditRecord.getFailureReason())
            .costMs(authzAuditRecord.getCostMs())
            .build();
    }
}