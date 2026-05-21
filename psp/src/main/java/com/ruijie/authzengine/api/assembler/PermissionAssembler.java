package com.ruijie.authzengine.api.assembler;

import com.ruijie.authzengine.api.dto.request.PermissionItemRequest;
import com.ruijie.authzengine.api.dto.response.PageResponse;
import com.ruijie.authzengine.api.dto.response.PermissionItemResponse;
import com.ruijie.authzengine.domain.model.governance.PageResult;
import com.ruijie.authzengine.domain.model.governance.permission.AuthPermissionItem;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 权限项装配器。
 */
@Component
public class PermissionAssembler {

	public AuthPermissionItem toDefinition(PermissionItemRequest request) {
		return AuthPermissionItem.builder()
			.tenantId(request.getTenantId())
			.appCode(request.getAppCode())
			.resModelCode(request.getResourceModel())
			.resId(request.getResourceCode())
			.actCode(request.getActionCode())
			.failStrategy(request.getFailStrategy())
			.build();
	}

	public PermissionItemResponse toResponse(AuthPermissionItem item) {
		return PermissionItemResponse.builder()
			.id(item.getId())
			.tenantId(item.getTenantId())
			.appCode(item.getAppCode())
			.permCode(item.getPermCode())
			.resourceModel(item.getResModelCode())
			.resourceCode(item.getResId())
			.actionCode(item.getActCode())
			.failStrategy(item.getFailStrategy())
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