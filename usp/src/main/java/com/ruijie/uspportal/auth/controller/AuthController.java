package com.ruijie.uspportal.auth.controller;

import com.ruijie.uspportal.auth.dto.CurrentUserResponse;
import com.ruijie.uspportal.auth.dto.LoginOptionsResponse;
import com.ruijie.uspportal.auth.dto.LoginRequest;
import com.ruijie.uspportal.auth.dto.LoginResponse;
import com.ruijie.uspportal.auth.service.AuthService;
import com.ruijie.uspportal.common.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * 认证接口控制器。
 *
 * <p>负责向前端提供登录方式查询、账号登录、当前登录态查询以及退出登录等认证相关接口。</p>
 */
@RestController
@Validated
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 查询当前可用的登录方式。
     *
     * @return 登录入口及默认登录模式信息
     */
    @GetMapping("/login-options")
    public ApiResponse<LoginOptionsResponse> loginOptions() {
        return ApiResponse.success(authService.getLoginOptions());
    }

    /**
     * 执行账号登录并创建会话。
     *
     * @param request 登录请求体
     * @param httpServletRequest 原始 HTTP 请求
     * @return 登录结果与令牌信息
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                            HttpServletRequest httpServletRequest) {
        return ApiResponse.success("登录成功", authService.login(request, httpServletRequest));
    }

    /**
     * 查询当前登录用户信息。
     *
     * @return 当前用户与认证模式快照
     */
    @GetMapping("/me")
    public ApiResponse<CurrentUserResponse> me() {
        return ApiResponse.success(authService.getCurrentUser());
    }

    /**
     * 注销当前访问令牌对应的登录态。
     *
     * @param unionSessionTicket 请求头中的联合会话票据
     * @return 空响应
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader(value = "union_session_ticket", required = false) String unionSessionTicket) {
        authService.logout(unionSessionTicket);
        return ApiResponse.success("退出成功", null);
    }
}
