package com.ruijie.authzengine.api.controller;

import com.ruijie.authzengine.api.assembler.ResourceAssembler;
import com.ruijie.authzengine.api.dto.request.ApiResourceRequest;
import com.ruijie.authzengine.api.dto.request.ComponentResourceRequest;
import com.ruijie.authzengine.api.dto.request.MenuResourceRequest;
import com.ruijie.authzengine.api.dto.request.PageResourceRequest;
import com.ruijie.authzengine.api.dto.response.ApiResourceResponse;
import com.ruijie.authzengine.api.dto.response.ComponentResourceResponse;
import com.ruijie.authzengine.api.dto.response.PageResponse;
import com.ruijie.authzengine.api.dto.response.MenuResourceResponse;
import com.ruijie.authzengine.api.dto.response.OperationAckResponse;
import com.ruijie.authzengine.api.dto.response.PageResourceResponse;
import com.ruijie.authzengine.application.service.ResourceAppService;
import com.ruijie.authzengine.shared.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * 治理资源目录分域接口。
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/authz-engine/api/v1/governance/resources")
@Tag(name = " Resource", description = "治理资源目录 CRUD 接口")
public class ResourceController {

    private final ResourceAppService resourceAppService;

    private final ResourceAssembler resourceAssembler;

    @GetMapping("/menus")
    @Operation(summary = "分页查询菜单资源目录")
    public ApiResponse<PageResponse<MenuResourceResponse>> pageMenus(
        @RequestParam("tenantId") @NotBlank(message = "tenantId 不能为空") String tenantId,
        @RequestParam("appCode") @NotBlank(message = "appCode 不能为空") String appCode,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
        @RequestParam(value = "pageSize", defaultValue = "20") int pageSize
    ) {
        return ApiResponse.success(
            resourceAssembler.toPageResponse(
                resourceAppService.pageMenus(tenantId, appCode, keyword, pageNo, pageSize),
                resourceAssembler::toResponse
            )
        );
    }

    @PostMapping("/menus")
    @Operation(summary = "创建菜单资源目录")
    public ApiResponse<OperationAckResponse> createMenu(@Valid @RequestBody MenuResourceRequest request) {
        MenuResourceResponse response = resourceAssembler.toResponse(
            resourceAppService.createMenu(resourceAssembler.toDefinition(request))
        );
        log.info("创建菜单资源 tenantId={}, appCode={}, menuCode={}", request.getTenantId(), request.getAppCode(), request.getMenuCode());
        return ApiResponse.success(ack(response.getMenuCode(), "菜单资源创建成功"));
    }

    @GetMapping("/menus/{recordCode}")
    @Operation(summary = "查询菜单资源详情")
    public ApiResponse<MenuResourceResponse> getMenu(
        @RequestParam("tenantId") @NotBlank(message = "tenantId 不能为空") String tenantId,
        @RequestParam("appCode") @NotBlank(message = "appCode 不能为空") String appCode,
        @PathVariable("recordCode") String recordCode
    ) {
        return ApiResponse.success(
            resourceAssembler.toResponse(resourceAppService.getMenu(tenantId, appCode, recordCode))
        );
    }

    @PutMapping("/menus/{recordCode}")
    @Operation(summary = "更新菜单资源")
    public ApiResponse<OperationAckResponse> updateMenu(
        @PathVariable("recordCode") String recordCode,
        @Valid @RequestBody MenuResourceRequest request
    ) {
        resourceAppService.updateMenu(
            request.getTenantId(),
            request.getAppCode(),
            recordCode,
            resourceAssembler.toDefinition(request)
        );
        return ApiResponse.success(ack(recordCode, "菜单资源更新成功"));
    }

    @DeleteMapping("/menus/{recordCode}")
    @Operation(summary = "删除菜单资源")
    public ApiResponse<OperationAckResponse> deleteMenu(
        @RequestParam("tenantId") @NotBlank(message = "tenantId 不能为空") String tenantId,
        @RequestParam("appCode") @NotBlank(message = "appCode 不能为空") String appCode,
        @PathVariable("recordCode") String recordCode
    ) {
        resourceAppService.deleteMenu(tenantId, appCode, recordCode);
        return ApiResponse.success(ack(recordCode, "菜单资源删除成功"));
    }

    @GetMapping("/pages")
    @Operation(summary = "分页查询页面资源目录")
    public ApiResponse<PageResponse<PageResourceResponse>> pagePages(
        @RequestParam("tenantId") @NotBlank(message = "tenantId 不能为空") String tenantId,
        @RequestParam("appCode") @NotBlank(message = "appCode 不能为空") String appCode,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
        @RequestParam(value = "pageSize", defaultValue = "20") int pageSize
    ) {
        return ApiResponse.success(
            resourceAssembler.toPageResponse(
                resourceAppService.pagePages(tenantId, appCode, keyword, pageNo, pageSize),
                resourceAssembler::toResponse
            )
        );
    }

    @PostMapping("/pages")
    @Operation(summary = "创建页面资源目录")
    public ApiResponse<OperationAckResponse> createPage(@Valid @RequestBody PageResourceRequest request) {
        PageResourceResponse response = resourceAssembler.toResponse(
            resourceAppService.createPage(resourceAssembler.toDefinition(request))
        );
        return ApiResponse.success(ack(response.getPageCode(), "页面资源创建成功"));
    }

    @GetMapping("/pages/{recordCode}")
    @Operation(summary = "查询页面资源详情")
    public ApiResponse<PageResourceResponse> getPage(
        @RequestParam("tenantId") @NotBlank(message = "tenantId 不能为空") String tenantId,
        @RequestParam("appCode") @NotBlank(message = "appCode 不能为空") String appCode,
        @PathVariable("recordCode") String recordCode
    ) {
        return ApiResponse.success(
            resourceAssembler.toResponse(resourceAppService.getPage(tenantId, appCode, recordCode))
        );
    }

    @PutMapping("/pages/{recordCode}")
    @Operation(summary = "更新页面资源")
    public ApiResponse<OperationAckResponse> updatePage(
        @PathVariable("recordCode") String recordCode,
        @Valid @RequestBody PageResourceRequest request
    ) {
        resourceAppService.updatePage(
            request.getTenantId(),
            request.getAppCode(),
            recordCode,
            resourceAssembler.toDefinition(request)
        );
        return ApiResponse.success(ack(recordCode, "页面资源更新成功"));
    }

    @DeleteMapping("/pages/{recordCode}")
    @Operation(summary = "删除页面资源")
    public ApiResponse<OperationAckResponse> deletePage(
        @RequestParam("tenantId") @NotBlank(message = "tenantId 不能为空") String tenantId,
        @RequestParam("appCode") @NotBlank(message = "appCode 不能为空") String appCode,
        @PathVariable("recordCode") String recordCode
    ) {
        resourceAppService.deletePage(tenantId, appCode, recordCode);
        return ApiResponse.success(ack(recordCode, "页面资源删除成功"));
    }

    @GetMapping("/components")
    @Operation(summary = "分页查询组件资源目录")
    public ApiResponse<PageResponse<ComponentResourceResponse>> pageComponents(
        @RequestParam("tenantId") @NotBlank(message = "tenantId 不能为空") String tenantId,
        @RequestParam("appCode") @NotBlank(message = "appCode 不能为空") String appCode,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
        @RequestParam(value = "pageSize", defaultValue = "20") int pageSize
    ) {
        return ApiResponse.success(
            resourceAssembler.toPageResponse(
                resourceAppService.pageComponents(tenantId, appCode, keyword, pageNo, pageSize),
                resourceAssembler::toResponse
            )
        );
    }

    @PostMapping("/components")
    @Operation(summary = "创建组件资源目录")
    public ApiResponse<OperationAckResponse> createComponent(@Valid @RequestBody ComponentResourceRequest request) {
        ComponentResourceResponse response = resourceAssembler.toResponse(
            resourceAppService.createComponent(resourceAssembler.toDefinition(request))
        );
        return ApiResponse.success(ack(response.getComponentCode(), "组件资源创建成功"));
    }

    @GetMapping("/components/{recordCode}")
    @Operation(summary = "查询组件资源详情")
    public ApiResponse<ComponentResourceResponse> getComponent(
        @RequestParam("tenantId") @NotBlank(message = "tenantId 不能为空") String tenantId,
        @RequestParam("appCode") @NotBlank(message = "appCode 不能为空") String appCode,
        @PathVariable("recordCode") String recordCode
    ) {
        return ApiResponse.success(
            resourceAssembler.toResponse(resourceAppService.getComponent(tenantId, appCode, recordCode))
        );
    }

    @PutMapping("/components/{recordCode}")
    @Operation(summary = "更新组件资源")
    public ApiResponse<OperationAckResponse> updateComponent(
        @PathVariable("recordCode") String recordCode,
        @Valid @RequestBody ComponentResourceRequest request
    ) {
        resourceAppService.updateComponent(
            request.getTenantId(),
            request.getAppCode(),
            recordCode,
            resourceAssembler.toDefinition(request)
        );
        return ApiResponse.success(ack(recordCode, "组件资源更新成功"));
    }

    @DeleteMapping("/components/{recordCode}")
    @Operation(summary = "删除组件资源")
    public ApiResponse<OperationAckResponse> deleteComponent(
        @RequestParam("tenantId") @NotBlank(message = "tenantId 不能为空") String tenantId,
        @RequestParam("appCode") @NotBlank(message = "appCode 不能为空") String appCode,
        @PathVariable("recordCode") String recordCode
    ) {
        resourceAppService.deleteComponent(tenantId, appCode, recordCode);
        return ApiResponse.success(ack(recordCode, "组件资源删除成功"));
    }

    @GetMapping("/apis")
    @Operation(summary = "分页查询 API 资源目录")
    public ApiResponse<PageResponse<ApiResourceResponse>> pageApis(
        @RequestParam("tenantId") @NotBlank(message = "tenantId 不能为空") String tenantId,
        @RequestParam("appCode") @NotBlank(message = "appCode 不能为空") String appCode,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
        @RequestParam(value = "pageSize", defaultValue = "20") int pageSize
    ) {
        return ApiResponse.success(
            resourceAssembler.toPageResponse(
                resourceAppService.pageApis(tenantId, appCode, keyword, pageNo, pageSize),
                resourceAssembler::toResponse
            )
        );
    }

    @PostMapping("/apis")
    @Operation(summary = "创建 API 资源")
    public ApiResponse<OperationAckResponse> createApi(@Valid @RequestBody ApiResourceRequest request) {
        ApiResourceResponse response = resourceAssembler.toResponse(
            resourceAppService.createApi(resourceAssembler.toDefinition(request))
        );
        return ApiResponse.success(ack(response.getApiCode(), "API 资源创建成功"));
    }

    @GetMapping("/apis/{recordCode}")
    @Operation(summary = "查询 API 资源详情")
    public ApiResponse<ApiResourceResponse> getApi(
        @RequestParam("tenantId") @NotBlank(message = "tenantId 不能为空") String tenantId,
        @RequestParam("appCode") @NotBlank(message = "appCode 不能为空") String appCode,
        @PathVariable("recordCode") String recordCode
    ) {
        return ApiResponse.success(
            resourceAssembler.toResponse(resourceAppService.getApi(tenantId, appCode, recordCode))
        );
    }

    @PutMapping("/apis/{recordCode}")
    @Operation(summary = "更新 API 资源")
    public ApiResponse<OperationAckResponse> updateApi(
        @PathVariable("recordCode") String recordCode,
        @Valid @RequestBody ApiResourceRequest request
    ) {
        resourceAppService.updateApi(
            request.getTenantId(),
            request.getAppCode(),
            recordCode,
            resourceAssembler.toDefinition(request)
        );
        return ApiResponse.success(ack(recordCode, "API 资源更新成功"));
    }

    @DeleteMapping("/apis/{recordCode}")
    @Operation(summary = "删除 API 资源")
    public ApiResponse<OperationAckResponse> deleteApi(
        @RequestParam("tenantId") @NotBlank(message = "tenantId 不能为空") String tenantId,
        @RequestParam("appCode") @NotBlank(message = "appCode 不能为空") String appCode,
        @PathVariable("recordCode") String recordCode
    ) {
        resourceAppService.deleteApi(tenantId, appCode, recordCode);
        return ApiResponse.success(ack(recordCode, "API 资源删除成功"));
    }

    private OperationAckResponse ack(String businessId, String note) {
        return OperationAckResponse.builder()
            .accepted(true)
            .businessId(businessId)
            .note(note)
            .build();
    }
}