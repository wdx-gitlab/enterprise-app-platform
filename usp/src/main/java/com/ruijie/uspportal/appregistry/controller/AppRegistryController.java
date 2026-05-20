package com.ruijie.uspportal.appregistry.controller;

import com.ruijie.uspportal.appregistry.dto.AppSaveRequest;
import com.ruijie.uspportal.appregistry.entity.AppRegistryEntity;
import com.ruijie.uspportal.appregistry.service.AppRegistryService;
import com.ruijie.uspportal.common.ApiResponse;
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
 * 应用注册管理控制器。
 *
 * <p>提供应用注册信息的查询、发布与上下线接口，用于维护门户中的应用目录与可见状态。</p>
 */
@RestController
@Validated
@RequestMapping("/api/apps")
public class AppRegistryController {

    private final AppRegistryService appRegistryService;

    @Autowired
    public AppRegistryController(AppRegistryService appRegistryService) {
        this.appRegistryService = appRegistryService;
    }

    /**
     * 查询全部应用注册记录。
     *
     * @return 应用注册列表
     */
    @GetMapping
    public ApiResponse<List<AppRegistryEntity>> list() {
        return ApiResponse.success(appRegistryService.list());
    }

    /**
     * 查询可供宿主消费的应用目录。
     *
     * @return 已上架应用列表
     */
    @GetMapping("/catalog")
    public ApiResponse<List<AppRegistryEntity>> catalog() {
        return ApiResponse.success(appRegistryService.catalog());
    }

    /**
     * 查询单个应用注册详情。
     *
     * @param id 应用主键
     * @return 应用详情
     */
    @GetMapping("/{id}")
    public ApiResponse<AppRegistryEntity> get(@PathVariable Long id) {
        return ApiResponse.success(appRegistryService.get(id));
    }

    /**
     * 创建应用注册记录。
     *
     * @param request 应用保存请求
     * @return 创建后的应用信息
     */
    @PostMapping
    public ApiResponse<AppRegistryEntity> create(@Valid @RequestBody AppSaveRequest request) {
        return ApiResponse.success("创建成功", appRegistryService.create(request));
    }

    /**
     * 更新指定应用注册记录。
     *
     * @param id 应用主键
     * @param request 应用保存请求
     * @return 更新后的应用信息
     */
    @PutMapping("/{id}")
    public ApiResponse<AppRegistryEntity> update(@PathVariable Long id, @Valid @RequestBody AppSaveRequest request) {
        return ApiResponse.success("更新成功", appRegistryService.update(id, request));
    }

    /**
     * 发布指定应用，使其进入可用目录。
     *
     * @param id 应用主键
     * @return 空响应
     */
    @PostMapping("/{id}/publish")
    public ApiResponse<Void> publish(@PathVariable Long id) {
        appRegistryService.publish(id);
        return ApiResponse.success("发布成功", null);
    }

    /**
     * 下线指定应用。
     *
     * @param id 应用主键
     * @return 空响应
     */
    @PostMapping("/{id}/offline")
    public ApiResponse<Void> offline(@PathVariable Long id) {
        appRegistryService.offline(id);
        return ApiResponse.success("下线成功", null);
    }
}
