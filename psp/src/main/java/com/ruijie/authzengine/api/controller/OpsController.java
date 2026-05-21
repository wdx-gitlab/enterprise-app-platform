package com.ruijie.authzengine.api.controller;

import com.ruijie.authzengine.api.assembler.OpsAssembler;
import com.ruijie.authzengine.api.dto.request.DelegationRevokeRequest;
import com.ruijie.authzengine.api.dto.response.DelegationResponse;
import com.ruijie.authzengine.application.service.DelegationAppService;
import com.ruijie.authzengine.shared.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 兼容运营接口。
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/authz-engine/api/v1/governance")
@Tag(name = " Ops", description = "历史兼容运营接口")
public class OpsController {

    private final DelegationAppService delegationAppService;

    private final OpsAssembler opsAssembler;

    /**
     * 撤销委托授权。
     *
     * @param request 撤销请求
     * @return 操作结果
        * @deprecated 兼容入口，请迁移到委托分域标准路由
     */
    @Deprecated
    @PostMapping("/delegations/revoke")
    @Operation(summary = "撤销委托授权（兼容别名）", description = "按委托记录标识撤销委托授权", deprecated = true)
    public ApiResponse<DelegationResponse> revokeDelegation(@Valid @RequestBody DelegationRevokeRequest request) {
        log.info("收到兼容委托撤销请求 tenantId={}, appCode={}, delegationId={}",
            request.getTenantId(), request.getAppCode(), request.getDelegationId());
        return ApiResponse.success(opsAssembler.toResponse(
            delegationAppService.revokeDelegation(request.getTenantId(), request.getAppCode(), request.getDelegationId())
        ));
    }
}