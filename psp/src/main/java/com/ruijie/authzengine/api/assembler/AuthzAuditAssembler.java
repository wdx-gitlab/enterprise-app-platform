package com.ruijie.authzengine.api.assembler;

import com.ruijie.authzengine.api.dto.response.AuditLogDetailResponse;
import com.ruijie.authzengine.api.dto.response.AuditLogItemResponse;
import com.ruijie.authzengine.api.dto.response.AuditLogPageResponse;
import com.ruijie.authzengine.domain.model.ops.AuthzAuditPage;
import com.ruijie.authzengine.domain.model.ops.AuthzAuditRecord;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 审计查询装配器。
 */
@Component
public class AuthzAuditAssembler {

	/**
	 * 审计分页领域对象转响应。
	 *
	 * @param authzAuditPage 审计分页结果
	 * @return 审计分页响应
	 */
	public AuditLogPageResponse toPageResponse(AuthzAuditPage authzAuditPage) {
		return AuditLogPageResponse.builder()
			.records(authzAuditPage.getRecords().stream().map(this::toItemResponse).collect(Collectors.toList()))
			.pageNo(authzAuditPage.getPageNo())
			.pageSize(authzAuditPage.getPageSize())
			.total(authzAuditPage.getTotal())
			.build();
	}

	/**
	 * 审计领域对象转详情响应。
	 *
	 * @param authzAuditRecord 审计记录
	 * @return 详情响应
	 */
	public AuditLogDetailResponse toDetailResponse(AuthzAuditRecord authzAuditRecord) {
		return AuditLogDetailResponse.builder()
			.auditLogId(authzAuditRecord.getAuditLogId())
			.requestId(authzAuditRecord.getRequestId())
			.tenantId(authzAuditRecord.getTenantId())
			.appCode(authzAuditRecord.getAppCode())
			.subjectModel(authzAuditRecord.getSubjectModel())
			.subjectId(authzAuditRecord.getSubjectId())
			.resourceModel(authzAuditRecord.getResourceModel())
			.resId(authzAuditRecord.getResId())
			.actionCode(authzAuditRecord.getActionCode())
			.decision(authzAuditRecord.getDecision())
			.matchedPermissionCodes(authzAuditRecord.getMatchedPermissionCodes())
			.matchedAssignmentIds(authzAuditRecord.getMatchedAssignmentIds())
			.matchedDelegateIds(authzAuditRecord.getMatchedDelegateIds())
			.matchedPolicyTemplateCodes(authzAuditRecord.getMatchedPolicyTemplateCodes())
			.failureReason(authzAuditRecord.getFailureReason())
			.costMs(authzAuditRecord.getCostMs())
			.build();
	}

	private AuditLogItemResponse toItemResponse(AuthzAuditRecord authzAuditRecord) {
		return AuditLogItemResponse.builder()
			.auditLogId(authzAuditRecord.getAuditLogId())
			.requestId(authzAuditRecord.getRequestId())
			.tenantId(authzAuditRecord.getTenantId())
			.appCode(authzAuditRecord.getAppCode())
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