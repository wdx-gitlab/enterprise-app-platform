package com.ruijie.authzengine.api.controller;

import com.ruijie.authzengine.api.assembler.AssignmentAssembler;
import com.ruijie.authzengine.api.dto.request.AuthAssignmentRequest;
import com.ruijie.authzengine.api.dto.response.AuthAssignmentResponse;
import com.ruijie.authzengine.api.dto.response.PageResponse;
import com.ruijie.authzengine.api.dto.response.OperationAckResponse;
import com.ruijie.authzengine.application.service.AssignmentAppService;
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
 * 授权分配治理接口。
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/authz-engine/api/v1/governance/permissions/assignments")
@Tag(name = " Assignment", description = "授权分配治理 CRUD 接口")
public class AssignmentController {

    private final AssignmentAppService assignmentAppService;

    private final AssignmentAssembler assignmentAssembler;

    @GetMapping
    @Operation(summary = "分页查询授权分配")
    public ApiResponse<PageResponse<AuthAssignmentResponse>> pageAssignments(
        @RequestParam("tenantId") @NotBlank(message = "tenantId 不能为空") String tenantId,
        @RequestParam("appCode") @NotBlank(message = "appCode 不能为空") String appCode,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
        @RequestParam(value = "pageSize", defaultValue = "20") int pageSize
    ) {
        return ApiResponse.success(assignmentAssembler.toPageResponse(
            assignmentAppService.pageAssignments(tenantId, appCode, keyword, pageNo, pageSize),
            assignmentAssembler::toResponse
        ));
    }

    @GetMapping("/{recordId}")
    @Operation(summary = "查询授权分配详情")
    public ApiResponse<AuthAssignmentResponse> getAssignment(
        @RequestParam("tenantId") @NotBlank(message = "tenantId 不能为空") String tenantId,
        @RequestParam("appCode") @NotBlank(message = "appCode 不能为空") String appCode,
        @PathVariable("recordId") Long recordId
    ) {
        return ApiResponse.success(
            assignmentAssembler.toResponse(assignmentAppService.getAssignment(tenantId, appCode, recordId))
        );
    }

    @PostMapping
    @Operation(summary = "创建授权分配")
    public ApiResponse<OperationAckResponse> createAssignment(@Valid @RequestBody AuthAssignmentRequest request) {
        AuthAssignmentResponse response = assignmentAssembler.toResponse(
            assignmentAppService.createAssignment(
                request.getPolicyTemplateCode(),
                assignmentAssembler.toDefinition(request)
            )
        );
        return ApiResponse.success(ack(String.valueOf(response.getId()), "授权分配创建成功"));
    }

    @PutMapping("/{recordId}")
    @Operation(summary = "更新授权分配")
    public ApiResponse<OperationAckResponse> updateAssignment(
        @PathVariable("recordId") Long recordId,
        @Valid @RequestBody AuthAssignmentRequest request
    ) {
        assignmentAppService.updateAssignment(
            request.getTenantId(),
            request.getAppCode(),
            recordId,
            request.getPolicyTemplateCode(),
            assignmentAssembler.toDefinition(request)
        );
        return ApiResponse.success(ack(String.valueOf(recordId), "授权分配更新成功"));
    }

    @DeleteMapping("/{recordId}")
    @Operation(summary = "删除授权分配")
    public ApiResponse<OperationAckResponse> deleteAssignment(
        @RequestParam("tenantId") @NotBlank(message = "tenantId 不能为空") String tenantId,
        @RequestParam("appCode") @NotBlank(message = "appCode 不能为空") String appCode,
        @PathVariable("recordId") Long recordId
    ) {
        assignmentAppService.deleteAssignment(tenantId, appCode, recordId);
        return ApiResponse.success(ack(String.valueOf(recordId), "授权分配删除成功"));
    }

    private OperationAckResponse ack(String businessId, String note) {
        return OperationAckResponse.builder()
            .accepted(true)
            .businessId(businessId)
            .note(note)
            .build();
    }
}
