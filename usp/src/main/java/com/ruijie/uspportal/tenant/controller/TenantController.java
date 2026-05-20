package com.ruijie.uspportal.tenant.controller;

import com.ruijie.uspportal.common.ApiResponse;
import com.ruijie.uspportal.tenant.dto.TenantSaveRequest;
import com.ruijie.uspportal.tenant.entity.TenantEntity;
import com.ruijie.uspportal.tenant.service.TenantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 租户管理控制器。
 *
 * <p>提供租户的查询、创建、更新以及启用、暂停、恢复等生命周期管理接口。</p>
 */
@RestController
@Validated
@RequestMapping("/api/tenants")
public class TenantController {

    private final TenantService tenantService;

    @Autowired
    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    /**
     * 查询租户列表。
     *
     * @return 租户集合
     */
    @GetMapping
    public ApiResponse<List<TenantEntity>> list() {
        return ApiResponse.success(tenantService.list());
    }

    /**
     * 查询指定租户详情。
     *
     * @param id 租户主键
     * @return 租户详情
     */
    @GetMapping("/{id}")
    public ApiResponse<TenantEntity> get(@PathVariable Long id) {
        return ApiResponse.success(tenantService.get(id));
    }

    /**
     * 创建租户。
     *
     * @param request 租户保存请求
     * @return 创建后的租户信息
     */
    @PostMapping
    public ApiResponse<TenantEntity> create(@Valid @RequestBody TenantSaveRequest request) {
        return ApiResponse.success("创建成功", tenantService.create(request));
    }

    /**
     * 更新指定租户。
     *
     * @param id 租户主键
     * @param request 租户保存请求
     * @return 更新后的租户信息
     */
    @PutMapping("/{id}")
    public ApiResponse<TenantEntity> update(@PathVariable Long id, @Valid @RequestBody TenantSaveRequest request) {
        return ApiResponse.success("更新成功", tenantService.update(id, request));
    }

    /**
     * 激活指定租户。
     *
     * @param id 租户主键
     * @return 空响应
     */
    @PostMapping("/{id}/activate")
    public ApiResponse<Void> activate(@PathVariable Long id) {
        tenantService.activate(id);
        return ApiResponse.success("启用成功", null);
    }

    /**
     * 暂停指定租户。
     *
     * @param id 租户主键
     * @return 空响应
     */
    @PostMapping("/{id}/suspend")
    public ApiResponse<Void> suspend(@PathVariable Long id) {
        tenantService.suspend(id);
        return ApiResponse.success("暂停成功", null);
    }

    /**
     * 恢复指定租户。
     *
     * @param id 租户主键
     * @return 空响应
     */
    @PostMapping("/{id}/resume")
    public ApiResponse<Void> resume(@PathVariable Long id) {
        tenantService.resume(id);
        return ApiResponse.success("恢复成功", null);
    }
}
