package com.ruijie.authzengine.api.controller;

import com.ruijie.authzengine.api.assembler.GovernanceAssembler;
import com.ruijie.authzengine.api.assembler.MetaAssembler;
import com.ruijie.authzengine.api.dto.request.BoMetaModelRequest;
import com.ruijie.authzengine.api.dto.request.BoMetaModelRegisterRequest;
import com.ruijie.authzengine.api.dto.request.MetaModelRequest;
import com.ruijie.authzengine.api.dto.request.MetaModelRegisterRequest;
import com.ruijie.authzengine.api.dto.request.PolicyTemplateRequest;
import com.ruijie.authzengine.api.dto.request.StandardActionRequest;
import com.ruijie.authzengine.api.dto.response.BoMetaModelResponse;
import com.ruijie.authzengine.api.dto.response.BoSchemaColumnResponse;
import com.ruijie.authzengine.api.dto.response.PageResponse;
import com.ruijie.authzengine.api.dto.response.MetaModelResponse;
import com.ruijie.authzengine.api.dto.response.OperationAckResponse;
import com.ruijie.authzengine.api.dto.response.StandardActionResponse;
import com.ruijie.authzengine.api.dto.response.StandardPolicyTemplateResponse;
import com.ruijie.authzengine.api.dto.response.TenantAppResponse;
import com.ruijie.authzengine.application.service.CatalogAppService;
import com.ruijie.authzengine.application.service.MetaAppService;
import com.ruijie.authzengine.shared.web.ApiResponse;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.env.Environment;
import java.util.List;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 治理元模型与标准库接口。
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/authz-engine/api/v1/governance")
@Tag(name = " Meta", description = "治理元模型与标准库骨架接口")
public class MetaController {

    private final CatalogAppService catalogAppService;

    private final MetaAppService metaAppService;

    private final MetaAssembler metaAssembler;

    private final GovernanceAssembler assembler;
    
    private final Environment environment;

    /**
     * 查询数据库中已配置的所有租户-应用组合。
     */
    @GetMapping("/tenant-apps")
    @Operation(summary = "查询租户-应用组合列表", description = "返回 authz_meta_model 中已配置的去重 tenantId + appCode 列表")
    public ApiResponse<List<TenantAppResponse>> listTenantApps() {
        // 优先从运行时配置读取 tenantId/appCode（支持 camelCase 与 kebab-case），
        // 若配置存在且非空则返回单条配置记录；否则回退到数据库去重查询。
        String cfgTenant = environment.getProperty("authz.engine.tenantId",
            environment.getProperty("authz.engine.tenant-id"));
        String cfgApp = environment.getProperty("authz.engine.appCode",
            environment.getProperty("authz.engine.app-code"));
        if (cfgTenant != null && !cfgTenant.trim().isEmpty() && cfgApp != null && !cfgApp.trim().isEmpty()) {
            return ApiResponse.success(java.util.Collections.singletonList(
                TenantAppResponse.builder().tenantId(cfgTenant).appCode(cfgApp).build()
            ));
        }

        return ApiResponse.success(
            metaAppService.listTenantApps().entrySet().stream()
                .flatMap(entry -> entry.getValue().stream()
                    .map(aCode -> TenantAppResponse.builder()
                        .tenantId(entry.getKey())
                        .appCode(aCode)
                        .build()))
                .collect(java.util.stream.Collectors.toList())
        );
    }

    /**
     * 注册权限元模型。
     *
     * @param request 元模型注册请求
     * @return 已保存元模型
        * @deprecated 兼容入口，请迁移到标准 CRUD 路由
     */
    @PostMapping("/meta-models/register")
    @Deprecated
    @Operation(summary = "注册权限元模型", description = "最小骨架：支持元模型登记与统一响应包装", deprecated = true)
    public ApiResponse<OperationAckResponse> registerMetaModel(@Valid @RequestBody MetaModelRegisterRequest request) {
        log.info("收到元模型注册请求 tenantId={}, appCode={}, modelCode={}",
            request.getTenantId(), request.getAppCode(), request.getModelCode());
        catalogAppService.registerMetaModel(assembler.toDefinition(request));
        return ApiResponse.success(assembler.toAckResponse(request.getModelCode()));
    }

    /**
     * 分页查询权限元模型。
     */
    @GetMapping("/meta-models")
    @Operation(summary = "分页查询权限元模型")
    public ApiResponse<PageResponse<MetaModelResponse>> pageMetaModels(
        @RequestParam("tenantId") String tenantId,
        @RequestParam("appCode") String appCode,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
        @RequestParam(value = "pageSize", defaultValue = "20") int pageSize
    ) {
        return ApiResponse.success(
            metaAssembler.toPageResponse(
                metaAppService.pageMetaModels(tenantId, appCode, keyword, pageNo, pageSize),
                metaAssembler::toResponse
            )
        );
    }

    /**
     * 查询权限元模型详情。
     */
    @GetMapping("/meta-models/{modelCode}")
    @Operation(summary = "查询权限元模型详情")
    public ApiResponse<MetaModelResponse> getMetaModel(
        @RequestParam("tenantId") String tenantId,
        @RequestParam("appCode") String appCode,
        @PathVariable("modelCode") String modelCode
    ) {
        return ApiResponse.success(
            metaAssembler.toResponse(metaAppService.getMetaModel(tenantId, appCode, modelCode))
        );
    }

    /**
     * 创建权限元模型。
     */
    @PostMapping("/meta-models")
    @Operation(summary = "创建权限元模型")
    public ApiResponse<OperationAckResponse> createMetaModel(@Valid @RequestBody MetaModelRequest request) {
        log.info("创建权限元模型 tenantId={}, appCode={}, modelCode={}",
            request.getTenantId(), request.getAppCode(), request.getModelCode());
        MetaModelResponse response = metaAssembler.toResponse(
            metaAppService.createMetaModel(metaAssembler.toDefinition(request))
        );
        return ApiResponse.success(ack(response.getModelCode(), "权限元模型创建成功"));
    }

    /**
     * 更新权限元模型。
     */
    @PutMapping("/meta-models/{modelCode}")
    @Operation(summary = "更新权限元模型")
    public ApiResponse<OperationAckResponse> updateMetaModel(
        @PathVariable("modelCode") String modelCode,
        @Valid @RequestBody MetaModelRequest request
    ) {
        log.info("更新权限元模型 tenantId={}, appCode={}, modelCode={}",
            request.getTenantId(), request.getAppCode(), modelCode);
        metaAppService.updateMetaModel(
            request.getTenantId(),
            request.getAppCode(),
            modelCode,
            metaAssembler.toDefinition(request)
        );
        return ApiResponse.success(ack(modelCode, "权限元模型更新成功"));
    }

    /**
     * 删除权限元模型。
     */
    @DeleteMapping("/meta-models/{modelCode}")
    @Operation(summary = "删除权限元模型")
    public ApiResponse<OperationAckResponse> deleteMetaModel(
        @RequestParam("tenantId") String tenantId,
        @RequestParam("appCode") String appCode,
        @PathVariable("modelCode") String modelCode
    ) {
        log.info("删除权限元模型 tenantId={}, appCode={}, modelCode={}", tenantId, appCode, modelCode);
        metaAppService.deleteMetaModel(tenantId, appCode, modelCode);
        return ApiResponse.success(ack(modelCode, "权限元模型删除成功"));
    }

    /**
     * 注册业务对象元模型。
     *
     * @param request 业务对象元模型注册请求
     * @return 已保存业务对象元模型
     */
    @PostMapping("/bo-models/register")
    @Deprecated
    @Operation(summary = "注册业务对象元模型", description = "最小骨架：支持 BO 元模型登记与统一响应包装", deprecated = true)
    public ApiResponse<OperationAckResponse> registerBoMetaModel(@Valid @RequestBody BoMetaModelRegisterRequest request) {
        log.info("收到业务对象元模型注册请求 tenantId={}, appCode={}, boCode={}",
            request.getTenantId(), request.getAppCode(), request.getBoCode());
        catalogAppService.registerBoMetaModel(assembler.toDefinition(request));
        return ApiResponse.success(assembler.toAckResponse(request.getBoCode()));
    }

    /**
     * 分页查询业务对象元模型。
     */
    @GetMapping("/bo-models")
    @Operation(summary = "分页查询业务对象元模型")
    public ApiResponse<PageResponse<BoMetaModelResponse>> pageBoMetaModels(
        @RequestParam("tenantId") String tenantId,
        @RequestParam("appCode") String appCode,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
        @RequestParam(value = "pageSize", defaultValue = "20") int pageSize
    ) {
        return ApiResponse.success(
            metaAssembler.toPageResponse(
                metaAppService.pageBoMetaModels(tenantId, appCode, keyword, pageNo, pageSize),
                metaAssembler::toResponse
            )
        );
    }

    /**
     * 查询业务对象元模型详情。
     */
    @GetMapping("/bo-models/{boCode}")
    @Operation(summary = "查询业务对象元模型详情")
    public ApiResponse<BoMetaModelResponse> getBoMetaModel(
        @RequestParam("tenantId") String tenantId,
        @RequestParam("appCode") String appCode,
        @PathVariable("boCode") String boCode
    ) {
        return ApiResponse.success(
            metaAssembler.toResponse(metaAppService.getBoMetaModel(tenantId, appCode, boCode))
        );
    }

    /**
     * 创建业务对象元模型。
     */
    @PostMapping("/bo-models")
    @Operation(summary = "创建业务对象元模型")
    public ApiResponse<OperationAckResponse> createBoMetaModel(@Valid @RequestBody BoMetaModelRequest request) {
        log.info("创建业务对象元模型 tenantId={}, appCode={}, boCode={}",
            request.getTenantId(), request.getAppCode(), request.getBoCode());
        BoMetaModelResponse response = metaAssembler.toResponse(
            metaAppService.createBoMetaModel(metaAssembler.toDefinition(request))
        );
        return ApiResponse.success(ack(response.getBoCode(), "业务对象元模型创建成功"));
    }

    /**
     * 更新业务对象元模型。
     */
    @PutMapping("/bo-models/{boCode}")
    @Operation(summary = "更新业务对象元模型")
    public ApiResponse<OperationAckResponse> updateBoMetaModel(
        @PathVariable("boCode") String boCode,
        @Valid @RequestBody BoMetaModelRequest request
    ) {
        log.info("更新业务对象元模型 tenantId={}, appCode={}, boCode={}",
            request.getTenantId(), request.getAppCode(), boCode);
        metaAppService.updateBoMetaModel(
            request.getTenantId(),
            request.getAppCode(),
            boCode,
            metaAssembler.toDefinition(request)
        );
        return ApiResponse.success(ack(boCode, "业务对象元模型更新成功"));
    }

    /**
     * 删除业务对象元模型。
     */
    @DeleteMapping("/bo-models/{boCode}")
    @Operation(summary = "删除业务对象元模型")
    public ApiResponse<OperationAckResponse> deleteBoMetaModel(
        @RequestParam("tenantId") String tenantId,
        @RequestParam("appCode") String appCode,
        @PathVariable("boCode") String boCode
    ) {
        log.info("删除业务对象元模型 tenantId={}, appCode={}, boCode={}", tenantId, appCode, boCode);
        metaAppService.deleteBoMetaModel(tenantId, appCode, boCode);
        return ApiResponse.success(ack(boCode, "业务对象元模型删除成功"));
    }

    /**
     * 预览 BO 元数据列信息（三种模式：NATIVE / SHADOW / MANUAL）。
     *
     * <p>返回结果仅供管理员参考，不会直接落库；管理员确认后需调用保存接口。
     *
     * @param tenantId  租户标识
     * @param appCode   应用编码
     * @param boCode    BO 编码（路径参数）
     * @param tableName 物理表名（SHADOW 模式必填）
     * @param mode      采集模式：SHADOW | MANUAL，默认 MANUAL
     * @return 结构化列信息列表；未采集到时返回空列表
     */
    @GetMapping("/bo-models/{boCode}/schema-preview")
    @Operation(
        summary = "预览 BO 元数据列信息",
        description = "SHADOW 模式通过宿主主数据源 JDBC 直接采集；MANUAL 返回空列表。结果仅作为候选展示，不会直接落库。"
    )
    public ApiResponse<List<BoSchemaColumnResponse>> previewBoSchemaColumns(
        @RequestParam("tenantId") String tenantId,
        @RequestParam("appCode") String appCode,
        @PathVariable("boCode") String boCode,
        @Parameter(description = "物理表名，SHADOW 模式必填")
        @RequestParam(value = "tableName", required = false) String tableName,
        @Parameter(description = "采集模式：SHADOW | MANUAL，不传默认 MANUAL")
        @RequestParam(value = "mode", required = false, defaultValue = "MANUAL") String mode
    ) {
        log.info("预览 BO 元数据列信息 tenantId={} appCode={} boCode={} mode={} tableName={}",
            tenantId, appCode, boCode, mode, tableName);
        return ApiResponse.success(
            metaAssembler.toSchemaColumnResponses(
                metaAppService.previewBoSchema(tenantId, appCode, boCode, tableName, mode)
            )
        );
    }

    /**
     * 查询标准动作库。
     *
     * @param tenantId 租户标识
     * @return 标准动作列表
     */
    @GetMapping("/actions/catalog")
    @Operation(summary = "查询标准动作目录（兼容）", description = "最小骨架：返回全局标准动作与租户动作目录", deprecated = true)
    public ApiResponse<List<StandardActionResponse>> listStandardActions(
        @Parameter(description = "租户标识，不传时仅返回全局标准动作")
        @RequestParam(value = "tenantId", required = false) String tenantId
    ) {
        return ApiResponse.success(
            assembler.toActionResponses(
                catalogAppService.listStandardActions(resolveTenantScope(tenantId))
            )
        );
    }

    /**
     * 分页查询标准动作。
     */
    @GetMapping("/actions")
    @Operation(summary = "分页查询标准动作")
    public ApiResponse<PageResponse<StandardActionResponse>> pageStandardActions(
        @RequestParam(value = "tenantId", required = false) String tenantId,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
        @RequestParam(value = "pageSize", defaultValue = "20") int pageSize
    ) {
        return ApiResponse.success(
            metaAssembler.toPageResponse(
                metaAppService.pageStandardActions(resolveTenantScope(tenantId), keyword, pageNo, pageSize),
                metaAssembler::toResponse
            )
        );
    }

    /**
     * 查询标准动作详情。
     */
    @GetMapping("/actions/{actCode}")
    @Operation(summary = "查询标准动作详情")
    public ApiResponse<StandardActionResponse> getStandardAction(
        @RequestParam(value = "tenantId", required = false) String tenantId,
        @PathVariable("actCode") String actCode
    ) {
        return ApiResponse.success(
            metaAssembler.toResponse(metaAppService.getStandardAction(resolveTenantScope(tenantId), actCode))
        );
    }

    /**
     * 创建标准动作。
     */
    @PostMapping("/actions")
    @Operation(summary = "创建标准动作")
    public ApiResponse<OperationAckResponse> createStandardAction(@Valid @RequestBody StandardActionRequest request) {
        log.info("创建标准动作 tenantId={}, actCode={}", request.getTenantId(), request.getActCode());
        StandardActionResponse response = metaAssembler.toResponse(
            metaAppService.createStandardAction(metaAssembler.toDefinition(request))
        );
        return ApiResponse.success(ack(response.getActCode(), "标准动作创建成功"));
    }

    /**
     * 更新标准动作。
     */
    @PutMapping("/actions/{actCode}")
    @Operation(summary = "更新标准动作")
    public ApiResponse<OperationAckResponse> updateStandardAction(
        @PathVariable("actCode") String actCode,
        @Valid @RequestBody StandardActionRequest request
    ) {
        log.info("更新标准动作 tenantId={}, actCode={}", request.getTenantId(), actCode);
        metaAppService.updateStandardAction(
            request.getTenantId(),
            actCode,
            metaAssembler.toDefinition(request)
        );
        return ApiResponse.success(ack(actCode, "标准动作更新成功"));
    }

    /**
     * 删除标准动作。
     */
    @DeleteMapping("/actions/{actCode}")
    @Operation(summary = "删除标准动作")
    public ApiResponse<OperationAckResponse> deleteStandardAction(
        @RequestParam("tenantId") String tenantId,
        @PathVariable("actCode") String actCode
    ) {
        log.info("删除标准动作 tenantId={}, actCode={}", tenantId, actCode);
        metaAppService.deleteStandardAction(tenantId, actCode);
        return ApiResponse.success(ack(actCode, "标准动作删除成功"));
    }

    /**
     * 查询标准策略模板库。
     *
     * @param tenantId 租户标识
     * @return 标准策略模板列表
     */
    @GetMapping("/policy-templates/catalog")
    @Operation(summary = "查询标准策略模板目录（兼容）", description = "最小骨架：返回全局标准模板与租户模板目录", deprecated = true)
    public ApiResponse<List<StandardPolicyTemplateResponse>> listPolicyTemplates(
        @Parameter(description = "租户标识，不传时仅返回全局标准模板")
        @RequestParam(value = "tenantId", required = false) String tenantId
    ) {
        return ApiResponse.success(
            assembler.toPolicyTemplateResponses(
                catalogAppService.listStandardPolicyTemplates(resolveTenantScope(tenantId))
            )
        );
    }

    /**
     * 分页查询策略模板。
     */
    @GetMapping("/policy-templates")
    @Operation(summary = "分页查询策略模板")
    public ApiResponse<PageResponse<StandardPolicyTemplateResponse>> pageStandardPolicyTemplates(
        @RequestParam(value = "tenantId", required = false) String tenantId,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "polType", required = false) String polType,
        @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
        @RequestParam(value = "pageSize", defaultValue = "20") int pageSize
    ) {
        return ApiResponse.success(
            metaAssembler.toPageResponse(
                metaAppService.pageStandardPolicyTemplates(resolveTenantScope(tenantId), keyword, polType, pageNo, pageSize),
                metaAssembler::toResponse
            )
        );
    }

    /**
     * 查询策略模板详情。
     */
    @GetMapping("/policy-templates/{templateCode}")
    @Operation(summary = "查询策略模板详情")
    public ApiResponse<StandardPolicyTemplateResponse> getStandardPolicyTemplate(
        @RequestParam(value = "tenantId", required = false) String tenantId,
        @PathVariable("templateCode") String templateCode
    ) {
        return ApiResponse.success(
            metaAssembler.toResponse(
                metaAppService.getStandardPolicyTemplate(resolveTenantScope(tenantId), templateCode)
            )
        );
    }

    /**
     * 创建策略模板。
     */
    @PostMapping("/policy-templates")
    @Operation(summary = "创建策略模板")
    public ApiResponse<OperationAckResponse> createStandardPolicyTemplate(@Valid @RequestBody PolicyTemplateRequest request) {
        log.info("创建策略模板 tenantId={}, templateCode={}", request.getTenantId(), request.getTemplateCode());
        StandardPolicyTemplateResponse response = metaAssembler.toResponse(
            metaAppService.createStandardPolicyTemplate(metaAssembler.toDefinition(request))
        );
        return ApiResponse.success(ack(response.getTemplateCode(), "策略模板创建成功"));
    }

    /**
     * 更新策略模板。
     */
    @PutMapping("/policy-templates/{templateCode}")
    @Operation(summary = "更新策略模板")
    public ApiResponse<OperationAckResponse> updateStandardPolicyTemplate(
        @PathVariable("templateCode") String templateCode,
        @Valid @RequestBody PolicyTemplateRequest request
    ) {
        log.info("更新策略模板 tenantId={}, templateCode={}", request.getTenantId(), templateCode);
        metaAppService.updateStandardPolicyTemplate(
            request.getTenantId(),
            templateCode,
            metaAssembler.toDefinition(request)
        );
        return ApiResponse.success(ack(templateCode, "策略模板更新成功"));
    }

    /**
     * 删除策略模板。
     */
    @DeleteMapping("/policy-templates/{templateCode}")
    @Operation(summary = "删除策略模板")
    public ApiResponse<OperationAckResponse> deleteStandardPolicyTemplate(
        @RequestParam("tenantId") String tenantId,
        @PathVariable("templateCode") String templateCode
    ) {
        log.info("删除策略模板 tenantId={}, templateCode={}", tenantId, templateCode);
        metaAppService.deleteStandardPolicyTemplate(tenantId, templateCode);
        return ApiResponse.success(ack(templateCode, "策略模板删除成功"));
    }

    private String resolveTenantScope(String tenantId) {
        return StringUtils.hasText(tenantId) ? tenantId : "__GLOBAL__";
    }

    private OperationAckResponse ack(String businessId, String note) {
        return OperationAckResponse.builder()
            .accepted(true)
            .businessId(businessId)
            .note(note)
            .build();
    }
}