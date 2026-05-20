package com.ruijie.uspportal.auth.controller;

import com.ruijie.uspportal.auth.dto.LocalAccountResponse;
import com.ruijie.uspportal.auth.dto.LocalAccountSaveRequest;
import com.ruijie.uspportal.auth.service.LocalAccountService;
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
 * 本地账号管理控制器。
 *
 * <p>提供本地账号的查询、创建、更新以及启停用接口，供门户后台维护门户本地登录账号。</p>
 */
@RestController
@Validated
@RequestMapping("/api/local-accounts")
public class LocalAccountController {

    private final LocalAccountService localAccountService;

    @Autowired
    public LocalAccountController(LocalAccountService localAccountService) {
        this.localAccountService = localAccountService;
    }

    /**
     * 查询本地账号列表。
     *
     * @return 本地账号集合
     */
    @GetMapping
    public ApiResponse<List<LocalAccountResponse>> list() {
        return ApiResponse.success(localAccountService.list());
    }

    /**
     * 按主键查询本地账号详情。
     *
     * @param id 账号主键
     * @return 本地账号详情
     */
    @GetMapping("/{id}")
    public ApiResponse<LocalAccountResponse> get(@PathVariable Long id) {
        return ApiResponse.success(localAccountService.get(id));
    }

    /**
     * 创建本地账号。
     *
     * @param request 账号保存请求
     * @return 创建后的账号信息
     */
    @PostMapping
    public ApiResponse<LocalAccountResponse> create(@Valid @RequestBody LocalAccountSaveRequest request) {
        return ApiResponse.success("创建成功", localAccountService.create(request));
    }

    /**
     * 更新指定本地账号。
     *
     * @param id 账号主键
     * @param request 账号保存请求
     * @return 更新后的账号信息
     */
    @PutMapping("/{id}")
    public ApiResponse<LocalAccountResponse> update(@PathVariable Long id, @Valid @RequestBody LocalAccountSaveRequest request) {
        return ApiResponse.success("更新成功", localAccountService.update(id, request));
    }

    /**
     * 启用指定本地账号。
     *
     * @param id 账号主键
     * @return 空响应
     */
    @PostMapping("/{id}/enable")
    public ApiResponse<Void> enable(@PathVariable Long id) {
        localAccountService.enable(id);
        return ApiResponse.success("启用成功", null);
    }

    /**
     * 停用指定本地账号。
     *
     * @param id 账号主键
     * @return 空响应
     */
    @PostMapping("/{id}/disable")
    public ApiResponse<Void> disable(@PathVariable Long id) {
        localAccountService.disable(id);
        return ApiResponse.success("停用成功", null);
    }
}
