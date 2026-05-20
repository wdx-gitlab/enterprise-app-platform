package com.ruijie.uspportal.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruijie.uspportal.auth.domain.AuthMode;
import com.ruijie.uspportal.auth.dto.CurrentUserResponse;
import com.ruijie.uspportal.auth.dto.LoginOptionsResponse;
import com.ruijie.uspportal.auth.dto.LoginRequest;
import com.ruijie.uspportal.auth.dto.LoginResponse;
import com.ruijie.uspportal.auth.entity.LocalAccountEntity;
import com.ruijie.uspportal.auth.entity.LoginConfigEntity;
import com.ruijie.uspportal.auth.entity.LoginSessionEntity;
import com.ruijie.uspportal.auth.repository.LocalAccountRepository;
import com.ruijie.uspportal.auth.service.AuthService;
import com.ruijie.uspportal.auth.service.LoginConfigResolver;
import com.ruijie.uspportal.auth.service.SsoAccountMappingService;
import com.ruijie.uspportal.auth.util.LocalAccountPasswordCodec;
import com.ruijie.uspportal.common.BusinessException;
import com.ruijie.uspportal.security.CurrentUserContext;
import com.ruijie.uspportal.security.LoginSessionService;
import com.ruijie.uspportal.tenant.entity.TenantEntity;
import com.ruijie.uspportal.tenant.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Objects;

/**
 * 认证服务默认实现。
 *
 * <p>负责解析登录配置、校验本地账号登录、创建登录会话并返回当前登录用户信息。</p>
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final LoginConfigResolver loginConfigResolver;

    private final LocalAccountRepository localAccountRepository;

    private final TenantRepository tenantRepository;

    private final SsoAccountMappingService ssoAccountMappingService;

    private final LoginSessionService loginSessionService;

    @Autowired
    public AuthServiceImpl(LoginConfigResolver loginConfigResolver,
                           LocalAccountRepository localAccountRepository,
                           TenantRepository tenantRepository,
                           SsoAccountMappingService ssoAccountMappingService,
                           LoginSessionService loginSessionService) {
        this.loginConfigResolver = loginConfigResolver;
        this.localAccountRepository = localAccountRepository;
        this.tenantRepository = tenantRepository;
        this.ssoAccountMappingService = ssoAccountMappingService;
        this.loginSessionService = loginSessionService;
    }

    @Value("${usp.portal.default-tenant-code}")
    private String defaultTenantCode;

    /**
     * 查询当前可用的登录方式。
     *
     * @return 登录方式配置
     */
    @Override
    public LoginOptionsResponse getLoginOptions() {
        LoginConfigEntity config = loginConfigResolver.getEnabledLoginConfig();
        return LoginOptionsResponse.builder()
                .internalLoginEnabled(Boolean.TRUE.equals(config.getInternalLoginEnabled()))
                .ssoLoginEnabled(Boolean.TRUE.equals(config.getSsoLoginEnabled()))
                .defaultLoginMode(config.getDefaultLoginMode())
                .ssoButtonText(config.getSsoButtonText())
                .build();
    }

    /**
    * 执行本地账号登录并创建登录会话。
     *
     * @param request 登录请求体
     * @param httpServletRequest 原始 HTTP 请求
     * @return 登录结果
     */
    @Override
    public LoginResponse login(LoginRequest request, HttpServletRequest httpServletRequest) {
        LoginConfigEntity config = loginConfigResolver.getEnabledLoginConfig();
        if (!Boolean.TRUE.equals(config.getInternalLoginEnabled())) {
            throw new BusinessException("当前环境未启用账号密码登录");
        }
        String tenantCode = StringUtils.hasText(request.getTenantCode()) ? request.getTenantCode().trim() : defaultTenantCode;
        TenantEntity tenant = tenantRepository.findByCode(tenantCode);
        if (tenant == null) {
            throw new BusinessException("租户不存在");
        }
        if (!"ACTIVE".equalsIgnoreCase(tenant.getStatus())) {
            throw new BusinessException("当前租户不可登录");
        }
        String loginName = ssoAccountMappingService.resolveLoginName(request.getLoginName());
        LocalAccountEntity account = localAccountRepository.findEnabledAccount(loginName, tenantCode);
        if (account == null) {
            throw new BusinessException("账号或密码错误");
        }
        if (!LocalAccountPasswordCodec.matches(request.getPassword(), account.getPasswordHash(), account.getPasswordEncodeType())) {
            throw new BusinessException("账号或密码错误");
        }
        CurrentUserContext.CurrentUser currentUser = CurrentUserContext.CurrentUser.builder()
                .userId(String.valueOf(account.getId()))
                .loginName(account.getLoginName())
                .displayName(account.getDisplayName())
                .tenantCode(tenantCode)
                .authMode(AuthMode.INTERNAL.name())
                .admin(Boolean.TRUE.equals(account.getAdmin()))
                .build();
        LoginSessionEntity session = loginSessionService.createSession(currentUser,
                httpServletRequest.getRemoteAddr(),
                httpServletRequest.getHeader("User-Agent"));
        currentUser.setSessionId(session.getId());
        localAccountRepository.touchLastLogin(account.getId());
        return LoginResponse.builder()
            .unionSessionTicket(session.getId())
                .loginName(account.getLoginName())
                .displayName(account.getDisplayName())
                .tenantCode(tenantCode)
                .admin(Boolean.TRUE.equals(account.getAdmin()))
                .build();
    }

    /**
     * 查询当前登录用户信息。
     *
     * @return 当前用户信息
     */
    @Override
    public CurrentUserResponse getCurrentUser() {
        CurrentUserContext.CurrentUser currentUser = CurrentUserContext.get();
        if (currentUser == null) {
            throw new BusinessException("当前未登录");
        }
        return CurrentUserResponse.builder()
                .userId(currentUser.getUserId())
                .loginName(currentUser.getLoginName())
                .displayName(currentUser.getDisplayName())
                .tenantCode(currentUser.getTenantCode())
                .sessionId(currentUser.getSessionId())
                .authMode(currentUser.getAuthMode())
                .admin(currentUser.getAdmin())
                .build();
    }

    /**
     * 注销当前访问令牌对应的登录态。
     *
     * @param unionSessionTicketHeader 请求头中的联合会话票据
     */
    @Override
    public void logout(String unionSessionTicketHeader) {
        loginSessionService.logout(unionSessionTicketHeader);
    }

}
