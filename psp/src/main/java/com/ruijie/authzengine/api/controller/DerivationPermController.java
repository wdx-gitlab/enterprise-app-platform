package com.ruijie.authzengine.api.controller;

import com.ruijie.authzengine.api.dto.request.ResourceDerivationPermRequest;
import com.ruijie.authzengine.api.dto.response.OperationAckResponse;
import com.ruijie.authzengine.api.dto.response.PageResponse;
import com.ruijie.authzengine.api.dto.response.ResourceDerivationPermResponse;
import com.ruijie.authzengine.application.service.DerivationPermissionAppService;
import com.ruijie.authzengine.domain.model.governance.PageResult;
import com.ruijie.authzengine.domain.model.governance.permission.ResourceDerivationPermission;
import com.ruijie.authzengine.shared.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 派生权限关联治理接口。
 *
 * <p>管理页面、组件、API 与 BO/直接授权 API 权限项之间的派生关系，
 * 对应 `authz_res_derivation_perm` 表的 CRUD 治理操作。
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/authz-engine/api/v1/governance/resources/derivation-perms")
@Tag(name = " DerivationPerm", description = "派生权限关联治理接口")
public class DerivationPermController {

    private final DerivationPermissionAppService derivationPermissionAppService;

    @GetMapping
    @Operation(summary = "分页查询派生权限关联")
    public ApiResponse<PageResponse<ResourceDerivationPermResponse>> pageBindings(
        @RequestParam("tenantId") @NotBlank(message = "tenantId 不能为空") String tenantId,
        @RequestParam("appCode") @NotBlank(message = "appCode 不能为空") String appCode,
        @RequestParam(value = "resType", required = false) String resType,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
        @RequestParam(value = "pageSize", defaultValue = "20") int pageSize
    ) {
        PageResult<ResourceDerivationPermission> result =
            derivationPermissionAppService.pageBindings(tenantId, appCode, resType, keyword, pageNo, pageSize);
        return ApiResponse.success(toPageResponse(result, this::toResponse));
    }

    @PostMapping
    @Operation(summary = "创建派生权限关联")
    public ApiResponse<OperationAckResponse> createBinding(@Valid @RequestBody ResourceDerivationPermRequest request) {
        ResourceDerivationPermission saved = derivationPermissionAppService.saveBinding(toDefinition(request));
        return ApiResponse.success(ack(String.valueOf(saved.getId()), "派生权限关联创建成功"));
    }

    @DeleteMapping("/{bindingId}")
    @Operation(summary = "删除派生权限关联")
    public ApiResponse<OperationAckResponse> deleteBinding(
        @RequestParam("tenantId") @NotBlank(message = "tenantId 不能为空") String tenantId,
        @RequestParam("appCode") @NotBlank(message = "appCode 不能为空") String appCode,
        @PathVariable("bindingId") Long bindingId
    ) {
        derivationPermissionAppService.deleteBinding(tenantId, appCode, bindingId);
        return ApiResponse.success(ack(String.valueOf(bindingId), "派生权限关联删除成功"));
    }

    // ---- 内部转换 ----

    private ResourceDerivationPermission toDefinition(ResourceDerivationPermRequest request) {
        return ResourceDerivationPermission.builder()
            .tenantId(request.getTenantId())
            .appCode(request.getAppCode())
            .resType(request.getResType())
            .resId(request.getResId())
            .permItemId(request.getPermItemId())
            .sortOrder(request.getSortOrder())
            .build();
    }

    private ResourceDerivationPermResponse toResponse(ResourceDerivationPermission binding) {
        return ResourceDerivationPermResponse.builder()
            .id(binding.getId())
            .tenantId(binding.getTenantId())
            .appCode(binding.getAppCode())
            .resType(binding.getResType())
            .resId(binding.getResId())
            .resCode(binding.getResCode())
            .resName(binding.getResName())
            .permItemId(binding.getPermItemId())
            .permCode(binding.getPermCode())
            .sortOrder(binding.getSortOrder())
            .build();
    }

    private <T, R> PageResponse<R> toPageResponse(PageResult<T> pageResult, Function<T, R> mapper) {
        List<R> records = pageResult.getRecords().stream().map(mapper).collect(Collectors.toList());
        return PageResponse.<R>builder()
            .pageNo(pageResult.getPageNo())
            .pageSize(pageResult.getPageSize())
            .total(pageResult.getTotal())
            .records(records)
            .build();
    }

    private OperationAckResponse ack(String businessId, String note) {
        return OperationAckResponse.builder()
            .accepted(true)
            .businessId(businessId)
            .note(note)
            .build();
    }
}
