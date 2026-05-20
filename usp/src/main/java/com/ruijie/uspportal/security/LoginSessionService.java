package com.ruijie.uspportal.security;

import com.ruijie.uspportal.auth.entity.LocalAccountEntity;
import com.ruijie.uspportal.auth.entity.LoginSessionEntity;
import com.ruijie.uspportal.auth.repository.LocalAccountRepository;
import com.ruijie.uspportal.auth.repository.LoginSessionRepository;
import com.ruijie.uspportal.common.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 登录会话服务。
 *
 * <p>负责登录会话创建、令牌落库、访问令牌鉴权、退出登录以及按租户批量失效会话。</p>
 */
@Service
public class LoginSessionService {

    public static final String UNION_SESSION_TICKET_HEADER = "union_session_ticket";

    private final LoginSessionRepository loginSessionRepository;

    private final LocalAccountRepository localAccountRepository;

    @Autowired
    public LoginSessionService(LoginSessionRepository loginSessionRepository,
                               LocalAccountRepository localAccountRepository) {
        this.loginSessionRepository = loginSessionRepository;
        this.localAccountRepository = localAccountRepository;
    }

    /**
     * 创建一条登录会话记录。
     *
     * @param currentUser 当前登录用户
     * @param loginIp 登录 IP
     * @param userAgent 用户代理
     * @return 登录会话实体
     */
    public LoginSessionEntity createSession(CurrentUserContext.CurrentUser currentUser,
                                            String loginIp,
                                            String userAgent) {
        LoginSessionEntity entity = new LoginSessionEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setUserId(currentUser.getUserId());
        entity.setTenantCode(currentUser.getTenantCode());
        entity.setAuthMode(currentUser.getAuthMode());
        entity.setStatus("ACTIVE");
        entity.setLoginIp(loginIp);
        entity.setUserAgent(userAgent);
        entity.setLastActiveTime(LocalDateTime.now());
        entity.setExpireTime(LocalDateTime.now().plusHours(8));
        loginSessionRepository.insert(entity);
        return entity;
    }

    /**
     * 基于联合会话票据鉴权并恢复当前用户上下文。
     *
     * @param tokenHeaderValue 请求头中的联合会话票据值
     * @return 当前登录用户，未命中时返回 {@code null}
     */
    public CurrentUserContext.CurrentUser authenticate(String tokenHeaderValue) {
        String sessionTicket = extractSessionTicket(tokenHeaderValue);
        if (!StringUtils.hasText(sessionTicket)) {
            return null;
        }
        LoginSessionEntity session = loginSessionRepository.selectById(sessionTicket);
        if (session == null || !"ACTIVE".equals(session.getStatus()) || session.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("登录会话已失效");
        }
        Long accountId = parseAccountId(session.getUserId());
        if (accountId == null) {
            throw new BusinessException("登录会话用户信息异常");
        }
        LocalAccountEntity account = localAccountRepository.findEnabledAccountById(accountId, session.getTenantCode());
        if (account == null) {
            throw new BusinessException("登录用户不存在或已失效");
        }
        loginSessionRepository.touchSession(session.getId(), LocalDateTime.now());
        return CurrentUserContext.CurrentUser.builder()
                .userId(String.valueOf(account.getId()))
                .loginName(account.getLoginName())
                .displayName(account.getDisplayName())
                .tenantCode(session.getTenantCode())
                .sessionId(session.getId())
                .authMode(session.getAuthMode())
                .admin(Boolean.TRUE.equals(account.getAdmin()))
                .build();
    }

    /**
     * 注销当前访问令牌对应的登录会话。
     *
     * @param tokenHeaderValue 请求头中的联合会话票据值
     */
    public void logout(String tokenHeaderValue) {
        String sessionTicket = extractSessionTicket(tokenHeaderValue);
        if (!StringUtils.hasText(sessionTicket)) {
            return;
        }
        LoginSessionEntity session = loginSessionRepository.selectById(sessionTicket);
        if (session == null) {
            return;
        }
        loginSessionRepository.logout(session.getId(), LocalDateTime.now(), "USER_LOGOUT");
    }

    /**
     * 按租户批量使会话失效。
     *
     * @param tenantCode 租户编码
     * @param logoutTime 失效时间
     * @param logoutReason 失效原因
     */
    public void invalidateByTenantCode(String tenantCode, LocalDateTime logoutTime, String logoutReason) {
        loginSessionRepository.invalidateByTenantCode(tenantCode, logoutTime, logoutReason);
    }

    /**
     * 解析请求头中的联合会话票据。
     *
     * @param tokenHeaderValue 请求头值
     * @return 联合会话票据；为空时返回 {@code null}
     */
    private String extractSessionTicket(String tokenHeaderValue) {
        if (!StringUtils.hasText(tokenHeaderValue)) {
            return null;
        }
        return tokenHeaderValue.trim();
    }

    private Long parseAccountId(String userId) {
        try {
            return StringUtils.hasText(userId) ? Long.valueOf(userId) : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
