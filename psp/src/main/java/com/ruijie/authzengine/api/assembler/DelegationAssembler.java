package com.ruijie.authzengine.api.assembler;

import com.ruijie.authzengine.api.dto.request.DelegationCreateRequest;
import com.ruijie.authzengine.api.dto.response.DelegationResponse;
import com.ruijie.authzengine.api.dto.response.PageResponse;
import com.ruijie.authzengine.domain.model.governance.PageResult;
import com.ruijie.authzengine.domain.model.ops.AssignmentDelegate;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 委托授权装配器。
 */
@Component
public class DelegationAssembler {

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
	 * @return 响应对象
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
	 * 领域分页对象转 API 分页对象。
	 *
	 * @param result 领域分页结果
	 * @param mapper 记录转换函数
	 * @param <T> 领域类型
	 * @param <R> 响应类型
	 * @return API 分页响应
	 */
	public <T, R> PageResponse<R> toPageResponse(PageResult<T> result, Function<T, R> mapper) {
		return PageResponse.<R>builder()
			.pageNo(result.getPageNo())
			.pageSize(result.getPageSize())
			.total(result.getTotal())
			.records(result.getRecords().stream().map(mapper).collect(Collectors.toList()))
			.build();
	}
}