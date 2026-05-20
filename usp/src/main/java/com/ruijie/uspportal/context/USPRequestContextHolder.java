package com.ruijie.uspportal.context;

/**
 * USPRequestContext 持有器。
 */
public final class USPRequestContextHolder {

    private static final ThreadLocal<USPRequestContext> HOLDER = new ThreadLocal<>();

    /**
     * 创建 USPRequestContextHolder 实例。
     */
    private USPRequestContextHolder() {
    }

    /**
     * 设置相关状态。
     */
    public static void set(USPRequestContext context) {
        HOLDER.set(context);
    }

    /**
     * 获取当前线程绑定的统一请求上下文。
     *
     * @return 当前线程的请求上下文
     */
    public static USPRequestContext get() {
        return HOLDER.get();
    }

    /**
     * 清理相关状态。
     */
    public static void clear() {
        HOLDER.remove();
    }
}
