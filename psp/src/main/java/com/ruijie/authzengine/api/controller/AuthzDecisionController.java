package com.ruijie.authzengine.api.controller;

import com.ruijie.authzengine.api.assembler.AuthzRequestAssembler;
import com.ruijie.authzengine.api.dto.request.AuthzCheckRequest;
import com.ruijie.authzengine.api.dto.request.BatchAuthzCheckRequest;
import com.ruijie.authzengine.api.dto.request.DataScopeResolveRequest;
import com.ruijie.authzengine.api.dto.response.AuthzCheckResponse;
import com.ruijie.authzengine.api.dto.response.BatchAuthzContractResponse;
import com.ruijie.authzengine.api.dto.response.DataScopeContractResponse;
import com.ruijie.authzengine.application.service.AuthzContractAppService;
import com.ruijie.authzengine.application.service.AuthzDecisionAppService;
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
 * 鉴权 API 入口（API 层），是整条鉴权链路的最外层。
 *
 * <p>职责：
 * <ol>
 *   <li>参数校验（通过 JSR-303 注解 + 全局异常处理）</li>
 *   <li>协议转换（DTO ↔ 领域模型，由 {@link AuthzRequestAssembler} 完成）</li>
 *   <li>统一响应封装（{@link ApiResponse}）</li>
 * </ol>
 *
 * <p>不做任何业务判断，业务逻辑全部下沉到 application / domain 层。
 *
 * <p>完整调用链路：
 * <pre>
 *   <b>Controller</b> → AuthzDecisionAppService → AuthzFacade → PEP → PDP → (PDP 按需调 PIP)
 * </pre>
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/authz-engine/api/v1/authz")
@Tag(name = "Authz Decision", description = "鉴权执行与 CONTRACT_ONLY 占位接口")
public class AuthzDecisionController {

    private final AuthzDecisionAppService authzDecisionAppService;

    private final AuthzContractAppService authzContractAppService;

    private final AuthzRequestAssembler authzRequestAssembler;

    /**
     * 执行单次鉴权。
     *
     * @param request 鉴权请求
     * @return 统一响应包装后的鉴权结果
     */
    @PostMapping("/check")
    @Operation(summary = "执行单次鉴权", description = "通过 AuthzFacade 统一入口收口到 PEP/PDP/PIP")
    public ApiResponse<AuthzCheckResponse> check(@Valid @RequestBody AuthzCheckRequest request) {
        log.info("收到鉴权请求 tenantId={}, appCode={}, subjectId={}, action={}",
            request.getTenantId(), request.getAppCode(), request.getSubject().getSubjectId(), request.getAction());
        return ApiResponse.success(
            authzRequestAssembler.toResponse(
                authzDecisionAppService.checkWithGovernance(authzRequestAssembler.toDomain(request))
            )
        );
    }

    /**
     * 执行批量鉴权占位响应。
     *
     * @param request 批量鉴权请求
     * @return CONTRACT_ONLY 占位结果
     */
    @PostMapping("/batch-check")
    @Operation(summary = "批量鉴权占位接口", description = "本轮只保留契约，不实现批量调度、结果聚合和性能优化")
    public ApiResponse<BatchAuthzContractResponse> batchCheck(@Valid @RequestBody BatchAuthzCheckRequest request) {
        log.info("收到批量鉴权占位请求 size={}", request.getRequests().size());
        return ApiResponse.success(authzContractAppService.batchCheckContract(request));
    }

    /**
     * 执行数据权限查询（标准路径，对齐实施文档 §10.4.3）。
     *
     * @param request data-scope 请求
     * @return 首版 SQL 翻译结果或 CONTRACT_ONLY 占位结果
     */
    @PostMapping("/data-scope")
    @Operation(summary = "数据权限查询", description = "返回结构化 DataScope AST + maskedFields；BO Hook 已配置时输出 SQL 片段，否则回退 CONTRACT_ONLY")
    public ApiResponse<DataScopeContractResponse> dataScope(@Valid @RequestBody DataScopeResolveRequest request) {
        log.info("收到 data-scope 请求（标准路径）tenantId={}, appCode={}",
            request.getTenantId(), request.getAppCode());
        return ApiResponse.success(authzContractAppService.resolveDataScopeContract(request));
    }

    /**
     * 执行 data-scope 响应（兼容路径，已弃用，请迁移到 /data-scope）。
     *
     * @param request data-scope 请求
     * @return 首版 SQL 翻译结果或 CONTRACT_ONLY 占位结果
     * @deprecated 请迁移到 POST /authz-engine/api/v1/authz/data-scope
     */
    @Deprecated
    @PostMapping("/data-scope/resolve")
    @Operation(summary = "data-scope 接口（兼容，已弃用）", description = "兼容旧路径，请迁移到 /authz-engine/api/v1/authz/data-scope", deprecated = true)
    public ApiResponse<DataScopeContractResponse> resolveDataScope(@Valid @RequestBody DataScopeResolveRequest request) {
        log.info("收到 data-scope 请求 tenantId={}, appCode={}, policyTemplateCode={}, hasSemanticCondition={}",
            request.getTenantId(), request.getAppCode(), request.getPolicyTemplateCode(),
            StringUtils.hasText(request.getSemanticCondition()));
        return ApiResponse.success(authzContractAppService.resolveDataScopeContract(request));
    }
}