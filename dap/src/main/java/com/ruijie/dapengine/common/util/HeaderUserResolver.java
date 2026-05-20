package com.ruijie.dapengine.common.util;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * 请求主体标识解析工具。
 * 优先级：① RequestContextHolder 中的 DAP_USER 属性 → ② X-User-Id Header → ③ 空字符串。
 */
public final class HeaderUserResolver {

    /** RequestAttributes 中存放用户标识的属性键 */
    public static final String REQUEST_ATTR_USER = "DAP_USER";

    /** Header 键名 */
    public static final String HEADER_USER_ID = "X-User-Id";

    private HeaderUserResolver() {
    }

    /**
     * 从当前请求上下文解析用户标识。
     * 无法获取 request 或 Header 不存在时返回空字符串。
     */
    public static String resolve() {
        try {
            RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                Object user = attrs.getAttribute(REQUEST_ATTR_USER, RequestAttributes.SCOPE_REQUEST);
                if (user instanceof String && !((String) user).isEmpty()) {
                    return (String) user;
                }
                if (attrs instanceof ServletRequestAttributes) {
                    HttpServletRequest request = ((ServletRequestAttributes) attrs).getRequest();
                    return resolve(request);
                }
            }
        } catch (Exception ignored) {
            // 非 Web 环境或无请求上下文时静默处理
        }
        return "";
    }

    /**
     * 从指定 request 解析用户标识（用于测试或显式传入场景）。
     */
    public static String resolve(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        // 1. RequestAttribute 优先
        Object user = request.getAttribute(REQUEST_ATTR_USER);
        if (user instanceof String && !((String) user).isEmpty()) {
            return (String) user;
        }
        // 2. X-User-Id Header
        String header = request.getHeader(HEADER_USER_ID);
        if (header != null && !header.isEmpty()) {
            return header;
        }
        return "";
    }
}
