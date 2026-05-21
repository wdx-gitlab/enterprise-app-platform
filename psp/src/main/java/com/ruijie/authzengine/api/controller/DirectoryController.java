package com.ruijie.authzengine.api.controller;

import com.ruijie.authzengine.api.assembler.GovernanceAssembler;
import com.ruijie.authzengine.api.dto.request.ApiResourceUpsertRequest;
import com.ruijie.authzengine.api.dto.request.UserUpsertRequest;
import com.ruijie.authzengine.api.dto.response.OperationAckResponse;
import com.ruijie.authzengine.application.service.CatalogAppService;
import com.ruijie.authzengine.shared.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 治理主体与资源目录接口。
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/authz-engine/api/v1/governance")
@Tag(name = " Directory", description = "治理主体与资源目录骨架接口")
public class DirectoryController {

    private final CatalogAppService catalogAppService;

    private final GovernanceAssembler governanceAssembler;

    /**
     * 保存或更新用户目录。
     *
     * @param request 用户目录写入请求
     * @return 已保存用户目录
        * @deprecated 兼容入口，请迁移到分域标准 CRUD 路由
     */
    @PostMapping("/subjects/users/upsert")
    @Deprecated
    @Operation(summary = "保存或更新用户目录（兼容）", description = "兼容入口，建议迁移至 /authz-engine/api/v1/governance/subjects/users 的标准 CRUD 路由", deprecated = true)
    public ApiResponse<OperationAckResponse> upsertUser(@Valid @RequestBody UserUpsertRequest request) {
        log.info("收到用户目录写入请求 tenantId={}, appCode={}, staffNo={}, userId={}",
            request.getTenantId(), request.getAppCode(), request.getStaffNo(), request.getUserId());
        catalogAppService.upsertUser(governanceAssembler.toDefinition(request));
        return ApiResponse.success(governanceAssembler.toAckResponse(resolveUserBusinessId(request)));
    }

    /**
     * 保存或更新 API 资源目录。
     *
     * @param request API 资源目录写入请求
     * @return 已保存 API 资源目录
        * @deprecated 兼容入口，请迁移到分域标准 CRUD 路由
     */
    @PostMapping("/resources/apis/upsert")
    @Deprecated
    @Operation(summary = "保存或更新 API 资源目录（兼容）", description = "兼容入口，建议迁移至 /authz-engine/api/v1/governance/resources/apis 的标准 CRUD 路由", deprecated = true)
    public ApiResponse<OperationAckResponse> upsertApi(@Valid @RequestBody ApiResourceUpsertRequest request) {
        log.info("收到 API 资源写入请求 tenantId={}, appCode={}, apiCode={}",
            request.getTenantId(), request.getAppCode(), request.getApiCode());
        catalogAppService.upsertApi(governanceAssembler.toDefinition(request));
        return ApiResponse.success(governanceAssembler.toAckResponse(request.getApiCode()));
    }

    private String resolveUserBusinessId(UserUpsertRequest request) {
        if (request == null) {
            return null;
        }
        if (StringUtils.hasText(request.getStaffNo())) {
            return request.getStaffNo();
        }
        return request.getUserId();
    }
}