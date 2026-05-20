package com.ruijie.uspportal.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 当前登录用户上下文持有器。
 *
 * <p>基于 ThreadLocal 维护单次请求内的当前登录用户信息，供认证、租户与上下文装配链路复用。</p>
 */
public final class CurrentUserContext {

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    /**
     * 禁止实例化工具类。
     */
    private CurrentUserContext() {
    }

    /**
     * 绑定当前线程的登录用户。
     *
     * @param currentUser 当前登录用户
     */
    public static void set(CurrentUser currentUser) {
        HOLDER.set(currentUser);
    }

    /**
     * 获取当前线程绑定的登录用户。
     *
     * @return 当前登录用户
     */
    public static CurrentUser get() {
        return HOLDER.get();
    }

    /**
     * 获取当前登录用户 ID。
     *
     * @return 当前用户 ID
     */
    public static String getUserId() {
        CurrentUser currentUser = HOLDER.get();
        return currentUser == null ? null : currentUser.getUserId();
    }

    /**
     * 清理当前线程的登录用户上下文。
     */
    public static void clear() {
        HOLDER.remove();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    /**
     * 当前登录用户快照。
     */
    public static class CurrentUser {

        private String userId;

        private String loginName;

        private String displayName;

        private String tenantCode;

        private String sessionId;

        private String authMode;

        private Boolean admin;
    }
}
