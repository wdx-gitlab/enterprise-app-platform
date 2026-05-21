package com.ruijie.authzengine.api.controller;

import com.ruijie.authzengine.api.assembler.AuthzAuditAssembler;
import com.ruijie.authzengine.api.dto.response.AuditLogDetailResponse;
import com.ruijie.authzengine.api.dto.response.AuditLogPageResponse;
import com.ruijie.authzengine.application.service.AuthzAuditAppService;
import com.ruijie.authzengine.domain.model.ops.AuthzAuditQuery;
import com.ruijie.authzengine.shared.exception.BusinessException;
import com.ruijie.authzengine.shared.exception.ErrorCode;
import com.ruijie.authzengine.shared.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 鉴权审计查询接口。
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/authz-engine/api/v1/governance/audit-logs")
@Tag(name = " Audit", description = "鉴权审计查询接口")
public class AuthzAuditController {

    private final AuthzAuditAppService authzAuditAppService;

    private final AuthzAuditAssembler authzAuditAssembler;

    @GetMapping
    @Operation(summary = "分页查询鉴权审计日志")
    public ApiResponse<AuditLogPageResponse> queryAuditLogs(
        @RequestParam("tenantId") @NotBlank(message = "tenantId 不能为空") String tenantId,
        @RequestParam("appCode") @NotBlank(message = "appCode 不能为空") String appCode,
        @RequestParam(value = "subjectModel", required = false) String subjectModel,
        @RequestParam(value = "subjectId", required = false) String subjectId,
        @RequestParam(value = "resourceModel", required = false) String resourceModel,
        @RequestParam(value = "resId", required = false) String resId,
        @RequestParam(value = "actionCode", required = false) String actionCode,
        @RequestParam(value = "decision", required = false) String decision,
        @RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo,
        @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize
    ) {
        validatePageArguments(pageNo, pageSize);
        AuthzAuditQuery query = AuthzAuditQuery.builder()
            .tenantId(tenantId)
            .appCode(appCode)
            .subjectModel(subjectModel)
            .subjectId(subjectId)
            .resourceModel(resourceModel)
            .resId(resId)
            .actionCode(actionCode)
            .decision(decision)
            .pageNo(pageNo)
            .pageSize(pageSize)
            .build();
        return ApiResponse.success(authzAuditAssembler.toPageResponse(authzAuditAppService.queryAuditLogs(query)));
    }

    @GetMapping("/{recordId}")
    @Operation(summary = "查询鉴权审计详情")
    public ApiResponse<AuditLogDetailResponse> getAuditLog(
        @RequestParam("tenantId") @NotBlank(message = "tenantId 不能为空") String tenantId,
        @RequestParam("appCode") @NotBlank(message = "appCode 不能为空") String appCode,
        @PathVariable("recordId") @Parameter(description = "审计记录标识") Long recordId
    ) {
        com.ruijie.authzengine.domain.model.ops.AuthzAuditRecord auditLog = authzAuditAppService.getAuditLog(tenantId, appCode, recordId);
        if (auditLog == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "审计记录不存在");
        }
        return ApiResponse.success(authzAuditAssembler.toDetailResponse(auditLog));
    }

    private void validatePageArguments(Integer pageNo, Integer pageSize) {
        if (pageNo == null || pageNo < 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "pageNo 必须大于等于 1");
        }
        if (pageSize == null || pageSize < 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "pageSize 必须大于等于 1");
        }
    }
}
