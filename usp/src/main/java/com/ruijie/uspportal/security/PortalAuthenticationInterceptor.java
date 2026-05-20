package com.ruijie.uspportal.security;

import com.ruijie.uspportal.auth.integration.sso.SsoAuthenticationService;
import com.ruijie.framework.base.RequestContext;
import org.springframework.http.HttpMethod;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 门户认证拦截器。
 *
 * <p>在 MVC 处理阶段基于请求头恢复当前登录用户上下文，确保执行顺序晚于上游过滤器与 SSO 校验链。</p>
 */
public class PortalAuthenticationInterceptor implements HandlerInterceptor {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final SsoAuthenticationService ssoAuthenticationService;
    private final List<String> whitelist;

    public PortalAuthenticationInterceptor(SsoAuthenticationService ssoAuthenticationService, List<String> whitelist) {
        this.ssoAuthenticationService = ssoAuthenticationService;
        this.whitelist = Collections.unmodifiableList(new ArrayList<>(whitelist));
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!requiresAuthentication(request)) {
            return true;
        }
        CurrentUserContext.CurrentUser currentUser = ssoAuthenticationService.authenticate(request, response);
        if (currentUser == null) {
            response.setStatus(428);
            return false;
        }
        CurrentUserContext.set(currentUser);
        bindRequestContext(currentUser, request);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        CurrentUserContext.clear();
        RequestContext.getCurrentContext().unset();
    }

    private boolean requiresAuthentication(HttpServletRequest request) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return false;
        }
        for (String candidatePath : resolveCandidatePaths(request)) {
            for (String pattern : whitelist) {
                if (PATH_MATCHER.match(pattern, candidatePath)) {
                    return false;
                }
            }
        }
        return true;
    }

    private List<String> resolveCandidatePaths(HttpServletRequest request) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        String applicationPath = resolveApplicationPath(request);
        if (StringUtils.hasText(applicationPath)) {
            candidates.add(applicationPath);
        }
        String requestUri = request.getRequestURI();
        if (StringUtils.hasText(requestUri)) {
            candidates.add(requestUri);
        }
        return new ArrayList<>(candidates);
    }

    private String resolveApplicationPath(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (StringUtils.hasText(contextPath) && requestUri.startsWith(contextPath)) {
            return requestUri.substring(contextPath.length());
        }
        return requestUri;
    }

    private void bindRequestContext(CurrentUserContext.CurrentUser currentUser, HttpServletRequest request) {
        RequestContext requestContext = RequestContext.getCurrentContext();
        requestContext.setUserId(currentUser.getUserId());
        requestContext.setOperId(currentUser.getUserId());
        requestContext.setUserName(currentUser.getDisplayName());
        requestContext.setCustomName(currentUser.getLoginName());
        requestContext.setUserNo(currentUser.getLoginName());
        requestContext.setUserType(Boolean.TRUE.equals(currentUser.getAdmin()) ? "ADMIN" : "USER");
        requestContext.setSSOType(currentUser.getAuthMode());
        requestContext.setHost(request.getRemoteHost());
    }
}