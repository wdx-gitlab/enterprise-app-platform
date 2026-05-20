package com.ruijie.uspportal.auth.integration.sso;

import com.ruijie.framework.sso.base.enums.CertificationTypeEnum;
import com.ruijie.framework.sso.base.model.LocalUserInfo;
import com.ruijie.framework.sso.base.spi.RuiJieSsoVerifyProcessor;
import com.ruijie.uspportal.security.CurrentUserContext;
import com.ruijie.uspportal.security.LoginSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * UspLocalVerify 处理器。
 */
public class UspLocalVerifyProcessor implements RuiJieSsoVerifyProcessor {

    private static final Logger log = LoggerFactory.getLogger(UspLocalVerifyProcessor.class);

    private static final String DEFAULT_SSO_TYPE = "USP";
    private static final String DEFAULT_USER_TYPE = "UspLocalAccount";

    private LoginSessionService loginSessionService;
    private String ssoType = DEFAULT_SSO_TYPE;
    private String userType = DEFAULT_USER_TYPE;

    @Override
    /**
     * 使用门户本地会话票据完成 SSO 校验。
     *
     * @param request 当前 HTTP 请求
     * @param response 当前 HTTP 响应
     * @return SSO 本地用户信息，未命中时返回 {@code null}
     * @throws Exception 底层校验异常
     */
    public LocalUserInfo loginCheck(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (loginSessionService == null) {
            return null;
        }
        String unionSessionTicket = request.getHeader(LoginSessionService.UNION_SESSION_TICKET_HEADER);
        if (!StringUtils.hasText(unionSessionTicket)) {
            return null;
        }
        try {
            CurrentUserContext.CurrentUser currentUser = loginSessionService.authenticate(unionSessionTicket);
            if (currentUser == null) {
                return null;
            }
            LocalUserInfo localUserInfo = new LocalUserInfo();
            localUserInfo.setUserId(currentUser.getUserId());
            localUserInfo.setUserName(StringUtils.hasText(currentUser.getDisplayName()) ? currentUser.getDisplayName() : currentUser.getLoginName());
            localUserInfo.setUserNo(currentUser.getLoginName());
            localUserInfo.setDeptCode(currentUser.getTenantCode());
            localUserInfo.setDeptName(currentUser.getTenantCode());
            localUserInfo.setUserType(getUserType());
            Map<String, Object> ext = new LinkedHashMap<>();
            ext.put("loginName", currentUser.getLoginName());
            ext.put("displayName", currentUser.getDisplayName());
            ext.put("tenantCode", currentUser.getTenantCode());
            ext.put("sessionId", currentUser.getSessionId());
            ext.put("authMode", currentUser.getAuthMode());
            ext.put("admin", currentUser.getAdmin());
            localUserInfo.setExt(ext);
            return localUserInfo;
        } catch (Exception ex) {
            log.debug("USP local verify skipped: {}", ex.getMessage());
            return null;
        }
    }

    @Override
    /**
     * 返回当前处理器的 SSO 类型。
     *
     * @return SSO 类型编码
     */
    public String getSSOType() {
        return ssoType;
    }

    @Override
    /**
     * 返回当前处理器使用的认证信息来源类型。
     *
     * @return 请求头认证类型
     */
    public CertificationTypeEnum getCertificationType() {
        return CertificationTypeEnum.REQUEST_HEADER;
    }

    @Override
    /**
     * 返回当前处理器读取的请求头名称。
     *
     * @return 联合会话票据请求头名
     */
    public String getCertificationName() {
        return LoginSessionService.UNION_SESSION_TICKET_HEADER;
    }

    @Override
    /**
     * 从 Spring 容器初始化当前处理器所需依赖与配置。
     *
     * @param applicationContext Spring 应用上下文
     */
    public void init(ApplicationContext applicationContext) {
        ConfigurableEnvironment environment = applicationContext.getBean(ConfigurableEnvironment.class);
        loginSessionService = applicationContext.getBean(LoginSessionService.class);
        ssoType = environment.getProperty("usp.portal.usp.sso-type", DEFAULT_SSO_TYPE);
        userType = environment.getProperty("usp.portal.usp.user-type", DEFAULT_USER_TYPE);
    }

    @Override
    /**
     * 返回当前处理器映射的用户类型。
     *
     * @return 用户类型编码
     */
    public String getUserType() {
        return userType;
    }
}
