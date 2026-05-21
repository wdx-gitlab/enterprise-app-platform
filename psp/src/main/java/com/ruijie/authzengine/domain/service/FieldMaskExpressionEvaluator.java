package com.ruijie.authzengine.domain.service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * FIELD 策略专用的脱敏表达式评估器（SpEL）。
 *
 * <p>执行 {@code MASK} 类型字段控制策略中的脱敏脚本：
 * <ul>
 *   <li>脚本通过 {@code #originalValue} 访问原始字符串值，可选通过 {@code #fieldName}、{@code #param} 访问字段名与策略参数</li>
 *   <li>超时（默认 100ms）或脚本执行异常时，返回降级占位符 {@code [MASK_ERROR]}</li>
 *   <li>非字符串字段（如数值、布尔）不执行脱敏，直接返回原值字符串表示</li>
 * </ul>
 *
 * <h3>安全约束</h3>
 * <ul>
 *   <li>不注入 rootObject，禁止 {@code T(java.lang.Runtime)} 等任意类型调用</li>
 *   <li>仅向上下文注入 {@code #originalValue}、{@code #fieldName}、{@code #param} 变量，不允许访问外部 Bean</li>
 * </ul>
 *
 * @see DataScopeExpressionEvaluator
 */
@Slf4j
@Service
public class FieldMaskExpressionEvaluator {

    /** 脱敏失败或超时时的降级占位符，§4.2.2 约定。 */
    public static final String MASK_ERROR_PLACEHOLDER = "[MASK_ERROR]";

    private static final long DEFAULT_TIMEOUT_MS = 100L;

    private static final ExecutorService DEFAULT_MASK_EXECUTOR = Executors.newCachedThreadPool(new NamedThreadFactory());

    private final ExpressionParser expressionParser = new SpelExpressionParser();

    private final long timeoutMs;

    private final ExecutorService executorService;

    /**
     * 使用默认 100ms 超时创建脱敏表达式评估器。
     */
    public FieldMaskExpressionEvaluator() {
        this(DEFAULT_TIMEOUT_MS, DEFAULT_MASK_EXECUTOR);
    }

    /**
     * 使用自定义超时时间创建评估器（仅测试使用）。
     *
     * @param timeoutMs 脱敏脚本执行超时，单位毫秒
     */
    FieldMaskExpressionEvaluator(long timeoutMs) {
        this(timeoutMs, DEFAULT_MASK_EXECUTOR);
    }

    /**
     * 使用自定义执行器创建评估器（仅测试使用）。
     *
     * @param timeoutMs       脱敏脚本执行超时，单位毫秒
     * @param executorService 脚本执行线程池
     */
    FieldMaskExpressionEvaluator(long timeoutMs, ExecutorService executorService) {
        this.timeoutMs = timeoutMs;
        this.executorService = executorService;
    }

    /**
     * 对字段原始值执行 MASK 脱敏脚本，返回脱敏后的字符串。
     *
     * <p>脚本示例：
     * <pre>
    *   #originalValue.length() > 6 ? #originalValue.substring(0,3) + '***'
    *       + #originalValue.substring(#originalValue.length()-3) : '***'
     * </pre>
     *
     * @param maskScript    脱敏 SpEL 脚本，通过 {@code #originalValue} 读取原始值
     * @param originalValue 字段原始值（String 类型）
     * @return 脱敏后字符串；脚本为空或原值为 null 时返回 null；超时或异常时返回 {@value #MASK_ERROR_PLACEHOLDER}
     */
    public String evaluate(String maskScript, String originalValue) {
        return evaluate(maskScript, originalValue, null, Collections.emptyMap());
    }

    /**
     * 对字段原始值执行 MASK 脱敏脚本，并向脚本暴露字段名与策略参数。
     *
     * @param maskScript    脱敏 SpEL 脚本
     * @param originalValue 字段原始值（String 类型）
     * @param fieldName     当前字段编码
     * @param param         当前 FIELD 策略参数
     * @return 脱敏后字符串；脚本为空或原值为 null 时返回原值；超时或异常时返回 {@value #MASK_ERROR_PLACEHOLDER}
     */
    public String evaluate(String maskScript, String originalValue, String fieldName, Map<String, Object> param) {
        if (!StringUtils.hasText(maskScript)) {
            return originalValue;
        }
        if (originalValue == null) {
            return null;
        }
        try {
            final String script = maskScript.trim();
            final String value = originalValue;
            final String currentFieldName = StringUtils.hasText(fieldName) ? fieldName.trim() : null;
            final Map<String, Object> safeParam = sanitizeParam(param);
            Object result = evaluateWithTimeout(() -> {
                StandardEvaluationContext ctx = new StandardEvaluationContext();
                ctx.addPropertyAccessor(new MapAccessor());
                ctx.setTypeLocator(typeName -> {
                    throw new EvaluationException("FIELD 策略禁止类型调用: " + typeName);
                });
                ctx.setVariable("originalValue", value);
                ctx.setVariable("fieldName", currentFieldName);
                ctx.setVariable("param", safeParam);
                return expressionParser.parseExpression(script).getValue(ctx);
            });
            return result == null ? null : String.valueOf(result);
        } catch (TimeoutException e) {
            log.warn("[FIELD-MASK] 脱敏脚本执行超时 ({}ms)，字段原值将以降级占位符替换，script={}", timeoutMs, maskScript);
            return MASK_ERROR_PLACEHOLDER;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[FIELD-MASK] 脱敏脚本执行被中断，字段原值将以降级占位符替换，script={}", maskScript);
            return MASK_ERROR_PLACEHOLDER;
        } catch (ExecutionException e) {
            log.warn("[FIELD-MASK] 脱敏脚本执行异常，字段原值将以降级占位符替换，script={}，cause={}", maskScript, e.getCause().getMessage());
            return MASK_ERROR_PLACEHOLDER;
        } catch (Exception e) {
            log.warn("[FIELD-MASK] 脱敏脚本评估意外失败，降级为占位符，script={}，cause={}", maskScript, e.getMessage());
            return MASK_ERROR_PLACEHOLDER;
        }
    }

    private Object evaluateWithTimeout(Callable<Object> task) throws InterruptedException, ExecutionException, TimeoutException {
        Future<Object> future = executorService.submit(task);
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException exception) {
            future.cancel(true);
            throw exception;
        }
    }

    // -------------------------------------------------------------------------
    // 内部辅助：线程工厂
    // -------------------------------------------------------------------------

    private static class NamedThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "authz-mask-eval-" + counter.incrementAndGet());
            t.setDaemon(true);
            return t;
        }
    }

    private Map<String, Object> sanitizeParam(Map<String, Object> param) {
        if (param == null || param.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(param));
    }
}
