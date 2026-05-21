package com.ruijie.authzengine.api.controller;

import com.ruijie.authzengine.api.assembler.DelegationAssembler;
import com.ruijie.authzengine.api.dto.request.DelegationCreateRequest;
import com.ruijie.authzengine.api.dto.response.DelegationResponse;
import com.ruijie.authzengine.api.dto.response.PageResponse;
import com.ruijie.authzengine.application.service.DelegationAppService;
import com.ruijie.authzengine.shared.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 委托授权治理接口。
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/authz-engine/api/v1/governance/delegations")
@Tag(name = " Delegation", description = "委托授权治理接口")
public class DelegationController {

    private final DelegationAppService delegationAppService;

    private final DelegationAssembler delegationAssembler;

    @GetMapping
    @Operation(summary = "分页查询委托记录")
    public ApiResponse<PageResponse<DelegationResponse>> pageDelegations(
        @RequestParam("tenantId") @NotBlank(message = "tenantId 不能为空") String tenantId,
        @RequestParam("appCode") @NotBlank(message = "appCode 不能为空") String appCode,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
        @RequestParam(value = "pageSize", defaultValue = "20") int pageSize
    ) {
        return ApiResponse.success(delegationAssembler.toPageResponse(
            delegationAppService.pageDelegations(tenantId, appCode, keyword, pageNo, pageSize),
            delegationAssembler::toResponse
        ));
    }

    @GetMapping("/{recordId}")
    @Operation(summary = "查询委托详情")
    public ApiResponse<DelegationResponse> getDelegation(
        @RequestParam("tenantId") @NotBlank(message = "tenantId 不能为空") String tenantId,
        @RequestParam("appCode") @NotBlank(message = "appCode 不能为空") String appCode,
        @PathVariable("recordId") Long recordId
    ) {
        return ApiResponse.success(
            delegationAssembler.toResponse(delegationAppService.getDelegation(tenantId, appCode, recordId))
        );
    }

    @GetMapping("/grantable-permissions")
    @Operation(summary = "查询委托人可委托权限")
    public ApiResponse<List<String>> listGrantablePermissionCodes(
        @RequestParam("tenantId") @NotBlank(message = "tenantId 不能为空") String tenantId,
        @RequestParam("appCode") @NotBlank(message = "appCode 不能为空") String appCode,
        @Parameter(description = "委托人主体模型")
        @RequestParam("grantorSubjectModel") @NotBlank(message = "grantorSubjectModel 不能为空") String grantorSubjectModel,
        @Parameter(description = "委托人主体标识")
        @RequestParam("grantorSubjectId") @NotBlank(message = "grantorSubjectId 不能为空") String grantorSubjectId,
        @Parameter(description = "委托生效判定时点，使用 ISO-8601 日期时间格式")
        @RequestParam("effectiveAt")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime effectiveAt
    ) {
        return ApiResponse.success(delegationAppService.listGrantablePermissionCodes(
            tenantId,
            appCode,
            grantorSubjectModel,
            grantorSubjectId,
            effectiveAt
        ));
    }

    @PostMapping
    @Operation(summary = "创建委托记录")
    public ApiResponse<DelegationResponse> createDelegation(@Valid @RequestBody DelegationCreateRequest request) {
        DelegationResponse response = delegationAssembler.toResponse(
            delegationAppService.createDelegation(delegationAssembler.toDefinition(request))
        );
        return ApiResponse.success(response);
    }

    @PostMapping("/{recordId}/revoke")
    @Operation(summary = "撤销委托")
    public ApiResponse<DelegationResponse> revokeDelegation(
        @PathVariable("recordId") Long recordId,
        @Parameter(description = "租户标识") @RequestParam("tenantId") @NotBlank(message = "tenantId 不能为空") String tenantId,
        @Parameter(description = "应用标识") @RequestParam("appCode") @NotBlank(message = "appCode 不能为空") String appCode
    ) {
        return ApiResponse.success(
            delegationAssembler.toResponse(delegationAppService.revokeDelegation(tenantId, appCode, recordId))
        );
    }
}
