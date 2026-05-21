package com.ruijie.authzengine.api.assembler;

import com.ruijie.authzengine.api.dto.request.AuthAssignmentRequest;
import com.ruijie.authzengine.api.dto.response.AuthAssignmentResponse;
import com.ruijie.authzengine.api.dto.response.PageResponse;
import com.ruijie.authzengine.domain.model.governance.PageResult;
import com.ruijie.authzengine.domain.model.governance.assignment.SysAuthAssignment;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 授权分配装配器。
 */
@Component
public class AssignmentAssembler {

	public SysAuthAssignment toDefinition(AuthAssignmentRequest request) {
		return SysAuthAssignment.builder()
			.tenantId(request.getTenantId())
			.appCode(request.getAppCode())
			.subjectModel(request.getSubjectModel())
			.subjectId(request.getSubjectId())
			.permItemId(request.getPermItemId())
			.policyParams(request.getPolicyParams())
			.expireTime(request.getExpireTime())
			.build();
	}

	public AuthAssignmentResponse toResponse(SysAuthAssignment assignment) {
		return AuthAssignmentResponse.builder()
			.id(assignment.getId())
			.tenantId(assignment.getTenantId())
			.appCode(assignment.getAppCode())
			.subjectModel(assignment.getSubjectModel())
			.subjectId(assignment.getSubjectId())
			.permItemId(assignment.getPermItemId())
			.policyTplId(assignment.getPolicyTplId())
			.policyParams(assignment.getPolicyParams())
			.expireTime(assignment.getExpireTime())
			.build();
	}

	public <T, R> PageResponse<R> toPageResponse(PageResult<T> pageResult, Function<T, R> mapper) {
		List<R> records = pageResult.getRecords().stream().map(mapper).collect(Collectors.toList());
		return PageResponse.<R>builder()
			.pageNo(pageResult.getPageNo())
			.pageSize(pageResult.getPageSize())
			.total(pageResult.getTotal())
			.records(records)
			.build();
	}
}