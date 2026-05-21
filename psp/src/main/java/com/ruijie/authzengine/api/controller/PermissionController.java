package com.ruijie.authzengine.api.controller;

import com.ruijie.authzengine.api.assembler.PermissionAssembler;
import com.ruijie.authzengine.api.dto.request.PermissionItemRequest;
import com.ruijie.authzengine.api.dto.response.PageResponse;
import com.ruijie.authzengine.api.dto.response.OperationAckResponse;
import com.ruijie.authzengine.api.dto.response.PermissionItemResponse;
import com.ruijie.authzengine.application.service.PermissionAppService;
import com.ruijie.authzengine.shared.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 权限项治理接口。
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/authz-engine/api/v1/governance/permissions")
@Tag(name = " Permission", description = "权限项治理 CRUD 接口")
public class PermissionController {

    private final PermissionAppService permissionAppService;

    private final PermissionAssembler permissionAssembler;

    @GetMapping("/items")
    @Operation(summary = "分页查询权限项")
    public ApiResponse<PageResponse<PermissionItemResponse>> pagePermissionItems(
        @RequestParam("tenantId") @NotBlank(message = "tenantId 不能为空") String tenantId,
        @RequestParam("appCode") @NotBlank(message = "appCode 不能为空") String appCode,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "resModelCode", required = false) String resModelCode,
        @RequestParam(value = "resId", required = false) String resId,
        @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
        @RequestParam(value = "pageSize", defaultValue = "20") int pageSize
    ) {
        return ApiResponse.success(permissionAssembler.toPageResponse(
            permissionAppService.pagePermissionItems(tenantId, appCode, keyword, resModelCode, resId, pageNo, pageSize),
            permissionAssembler::toResponse
        ));
    }

    @GetMapping("/items/{permCode}")
    @Operation(summary = "查询权限项详情")
    public ApiResponse<PermissionItemResponse> getPermissionItem(
        @RequestParam("tenantId") @NotBlank(message = "tenantId 不能为空") String tenantId,
        @RequestParam("appCode") @NotBlank(message = "appCode 不能为空") String appCode,
        @PathVariable("permCode") String permCode
    ) {
        return ApiResponse.success(
            permissionAssembler.toResponse(
                permissionAppService.getPermissionItem(tenantId, appCode, permCode)
            )
        );
    }

    @PostMapping("/items")
    @Operation(summary = "创建权限项")
    public ApiResponse<OperationAckResponse> createPermissionItem(@Valid @RequestBody PermissionItemRequest request) {
        PermissionItemResponse response = permissionAssembler.toResponse(
            permissionAppService.createPermissionItem(permissionAssembler.toDefinition(request))
        );
        return ApiResponse.success(ack(response.getPermCode(), "权限项创建成功"));
    }

    @PutMapping("/items/{permCode}")
    @Operation(summary = "更新权限项")
    public ApiResponse<OperationAckResponse> updatePermissionItem(
        @PathVariable("permCode") String permCode,
        @Valid @RequestBody PermissionItemRequest request
    ) {
        permissionAppService.updatePermissionItem(
            request.getTenantId(),
            request.getAppCode(),
            permCode,
            permissionAssembler.toDefinition(request)
        );
        return ApiResponse.success(ack(permCode, "权限项更新成功"));
    }

    @DeleteMapping("/items/{permCode}")
    @Operation(summary = "删除权限项")
    public ApiResponse<OperationAckResponse> deletePermissionItem(
        @RequestParam("tenantId") @NotBlank(message = "tenantId 不能为空") String tenantId,
        @RequestParam("appCode") @NotBlank(message = "appCode 不能为空") String appCode,
        @PathVariable("permCode") String permCode
    ) {
        permissionAppService.deletePermissionItem(tenantId, appCode, permCode);
        return ApiResponse.success(ack(permCode, "权限项删除成功"));
    }

    private OperationAckResponse ack(String businessId, String note) {
        return OperationAckResponse.builder()
            .accepted(true)
            .businessId(businessId)
            .note(note)
            .build();
    }
}
