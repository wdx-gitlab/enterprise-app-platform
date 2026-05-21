package com.ruijie.authzengine.application.spi;

import java.util.concurrent.Callable;

/**
 * Subject Hook 运行时上下文持有器。
 *
 * <p>推荐使用 {@link #executeInContext} 确保上下文在 finally 中被清理，
 * 避免线程池复用时上下文泄漏。
 */
public final class AuthMetaModelRuntimeContextHolder {

    private static final ThreadLocal<AuthMetaModelRuntimeContext> CONTEXT = new ThreadLocal<>();

    private AuthMetaModelRuntimeContextHolder() {
    }

    /**
     * 在指定上下文中执行任务，无论成功或异常均保证清理 ThreadLocal。
     *
     * @param context 运行时上下文
     * @param task    需要执行的任务
     * @param <T>     返回值类型
     * @return 任务执行结果
     * @throws Exception 任务抛出的异常原样传播
     */
    public static <T> T executeInContext(AuthMetaModelRuntimeContext context, Callable<T> task) throws Exception {
        bind(context);
        try {
            return task.call();
        } finally {
            clear();
        }
    }

    /**
     * 在指定上下文中执行无返回值任务，无论成功或异常均保证清理 ThreadLocal。
     *
     * @param context  运行时上下文
     * @param runnable 需要执行的任务
     */
    public static void executeInContext(AuthMetaModelRuntimeContext context, Runnable runnable) {
        bind(context);
        try {
            runnable.run();
        } finally {
            clear();
        }
    }

    /**
     * 绑定当前线程的 Subject Hook 上下文。
     *
     * @param context 运行时上下文
     */
    public static void bind(AuthMetaModelRuntimeContext context) {
        CONTEXT.set(context);
    }

    /**
     * 获取当前线程上下文。
     *
     * @return 运行时上下文，不存在时返回 null
     */
    public static AuthMetaModelRuntimeContext getCurrent() {
        return CONTEXT.get();
    }

    /**
     * 获取当前线程上下文，不存在时抛出异常。
     *
     * @return 运行时上下文
     */
    public static AuthMetaModelRuntimeContext requireCurrent() {
        AuthMetaModelRuntimeContext context = CONTEXT.get();
        if (context == null) {
            throw new IllegalStateException("当前线程不存在 Subject Hook 运行时上下文");
        }
        return context;
    }

    /**
     * 清理当前线程上下文。
     */
    public static void clear() {
        CONTEXT.remove();
    }
}