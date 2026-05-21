package com.ruijie.authzengine.domain.spi;

/**
 * 鉴权主体提供者 SPI。
 *
 * <p>由宿主应用实现，从当前请求上下文（如 JWT / Session / SecurityContext）提取用户身份。
 * 返回 null 表示未认证，PEP 切面将抛出 UNAUTHENTICATED 异常。
 */
public interface AuthzSubjectProvider {

    /**
     * 获取当前请求的用户 ID。
     *
     * @return 用户 ID，返回 null 表示未认证
     */
    String getCurrentUserId();

    /**
     * 获取当前请求的用户类型，默认 "USER"。
     *
     * @return 用户类型标识
     */
    default String getCurrentUserType() {
        return "USER";
    }
}
