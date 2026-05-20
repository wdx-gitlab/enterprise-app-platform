package com.ruijie.uspportal.auth.service;

import com.ruijie.uspportal.auth.dto.CurrentUserResponse;
import com.ruijie.uspportal.auth.dto.LoginOptionsResponse;
import com.ruijie.uspportal.auth.dto.LoginRequest;
import com.ruijie.uspportal.auth.dto.LoginResponse;

import javax.servlet.http.HttpServletRequest;

/**
 * 认证服务。
 *
 * <p>定义登录配置查询、账号登录、当前用户查询与退出登录等认证能力。</p>
 */
public interface AuthService {

    /**
     * 查询当前可用的登录方式。
     *
     * @return 登录方式配置
     */
    LoginOptionsResponse getLoginOptions();

    /**
     * 执行账号登录。
     *
     * @param request 登录请求体
     * @param httpServletRequest 原始 HTTP 请求
     * @return 登录结果
     */
    LoginResponse login(LoginRequest request, HttpServletRequest httpServletRequest);

    /**
     * 查询当前登录用户。
     *
     * @return 当前用户信息
     */
    CurrentUserResponse getCurrentUser();

    /**
     * 注销当前登录态。
     *
     * @param unionSessionTicketHeader 请求头中的联合会话票据
     */
    void logout(String unionSessionTicketHeader);
}
