package com.ruijie.authzengine.infrastructure.authz;

import com.ruijie.authzengine.domain.model.decision.AuthzDecision;

/**
 * 当前请求线程的鉴权决策持有者。
 *
 * <p>由 {@link AuthzHttpPepFilter} 在鉴权通过后写入，由 {@link com.ruijie.authzengine.autoconfigure.AuthzRowFilterInterceptor}
 * 读取，实现 DATA 策略行过滤 obligations 在 PEP 与 MyBatis 层之间的无侵入传递。
 *
 * <p>使用方式：
 * <ol>
 *   <li>PEP Filter 鉴权通过后调用 {@link #set(AuthzDecision)}</li>
 *   <li>MyBatis 拦截器读取 {@link #get()} 并从 obligations 中提取行过滤条件</li>
 *   <li>PEP Filter 的 finally 块调用 {@link #clear()} 防止线程池复用时的内存泄漏</li>
 * </ol>
 */
public final class AuthzDecisionHolder {

    private static final ThreadLocal<AuthzDecision> HOLDER = new ThreadLocal<>();

    private AuthzDecisionHolder() {
        // 工具类，禁止实例化
    }

    /**
     * 设置当前请求的鉴权决策。
     *
     * @param decision 鉴权决策结果，含 obligations
     */
    public static void set(AuthzDecision decision) {
        HOLDER.set(decision);
    }

    /**
     * 获取当前请求的鉴权决策。
     *
     * @return 鉴权决策，若未设置返回 null
     */
    public static AuthzDecision get() {
        return HOLDER.get();
    }

    /**
     * 清除当前线程的鉴权决策，防止线程池复用时的内存泄漏与数据污染。
     */
    public static void clear() {
        HOLDER.remove();
    }
}
