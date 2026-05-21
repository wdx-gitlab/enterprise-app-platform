package com.ruijie.authzengine.domain.service;

import com.ruijie.authzengine.domain.model.decision.DataScopeFragment;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * DATA 策略专用的 SpEL 评估器。
 */
@Slf4j
@Service
public class DataScopeExpressionEvaluator {

    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("^`?[A-Za-z0-9_]+`?$");

    private static final long DEFAULT_TIMEOUT_MS = 100L;

    private static final ExecutorService EVALUATION_EXECUTOR = Executors.newCachedThreadPool(new NamedThreadFactory());

    private final ExpressionParser expressionParser = new SpelExpressionParser();

    private final long timeoutMs;

    /**
     * 使用默认 100ms 超时创建评估器。
     */
    public DataScopeExpressionEvaluator() {
        this(DEFAULT_TIMEOUT_MS);
    }

    /**
     * 使用自定义超时时间创建评估器。
     *
     * @param timeoutMs 表达式执行超时时间，单位毫秒
     */
    DataScopeExpressionEvaluator(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    /**
     * 执行 DATA 策略表达式并返回参数化 SQL 片段。
     *
     * @param expressionScript DATA 策略 SpEL 表达式
     * @param sub 主体变量，映射到脚本中的 #sub
     * @param tableName 主表名，映射到脚本中的 #tableName
     * @param attributes 字段元数据列表，映射到脚本中的 #attributes
     * @param param 授权绑定参数，映射到脚本中的 #param
     * @return DATA 过滤 SQL 片段，空脚本或空结果时返回空片段
     */
    public DataScopeFragment evaluate(
        String expressionScript,
        Map<String, Object> sub,
        String tableName,
        List<Map<String, Object>> attributes,
        Map<String, Object> param
    ) {
        if (!StringUtils.hasText(expressionScript)) {
            return DataScopeFragment.empty();
        }
        DataScopeParamContext paramContext = new DataScopeParamContext();
        StandardEvaluationContext evaluationContext = buildEvaluationContext(
            sub,
            validateIdentifier(tableName, "tableName"),
            sanitizeAttributes(attributes),
            paramContext,
            sanitizeMap(param)
        );
        try {
            Object value = evaluateWithTimeout(expressionScript, evaluationContext);
            if (value instanceof DataScopeFragment) {
                DataScopeFragment fragment = (DataScopeFragment) value;
                return new DataScopeFragment(fragment.getSql(), mergeParams(fragment.getParams(), paramContext.snapshot()));
            }
            if (value == null) {
                return new DataScopeFragment(null, paramContext.snapshot());
            }
            String sql = String.valueOf(value).trim();
            if (!StringUtils.hasText(sql)) {
                return new DataScopeFragment(null, paramContext.snapshot());
            }
            return new DataScopeFragment(sql, paramContext.snapshot());
        } finally {
            paramContext.clear();
        }
    }

    /**
     * 执行实际的 SpEL 求值。
     *
     * @param expressionScript SpEL 表达式文本
     * @param evaluationContext 求值上下文
     * @return 表达式结果
     */
    protected Object doEvaluate(String expressionScript, EvaluationContext evaluationContext) {
        Expression expression = expressionParser.parseExpression(expressionScript);
        return expression.getValue(evaluationContext);
    }

    private StandardEvaluationContext buildEvaluationContext(
        Map<String, Object> sub,
        String tableName,
        List<Map<String, Object>> attributes,
        DataScopeParamContext paramContext,
        Map<String, Object> param
    ) {
        StandardEvaluationContext evaluationContext = new StandardEvaluationContext();
        evaluationContext.setRootObject(new DataScopeEvaluationRoot(paramContext));
        evaluationContext.addPropertyAccessor(new MapAccessor());
        evaluationContext.setTypeLocator(typeName -> {
            throw new EvaluationException("DATA 策略禁止类型调用: " + typeName);
        });
        evaluationContext.setVariable("sub", sanitizeMap(sub));
        evaluationContext.setVariable("tableName", tableName);
        evaluationContext.setVariable("attributes", attributes);
        evaluationContext.setVariable("param", param);
        return evaluationContext;
    }

    private Object evaluateWithTimeout(String expressionScript, StandardEvaluationContext evaluationContext) {
        Future<Object> future = EVALUATION_EXECUTOR.submit(() -> doEvaluate(expressionScript, evaluationContext));
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException exception) {
            future.cancel(true);
            throw new IllegalStateException("DATA_SCOPE_TIMEOUT", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("DATA_SCOPE_INTERRUPTED", exception);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new IllegalStateException("DATA_SCOPE_EVAL_ERROR", cause);
        }
    }

    private String validateIdentifier(String rawValue, String fieldName) {
        if (!StringUtils.hasText(rawValue) || !SAFE_IDENTIFIER.matcher(rawValue.trim()).matches()) {
            throw new IllegalArgumentException("非法标识符: " + fieldName);
        }
        return rawValue.trim();
    }

    private List<Map<String, Object>> sanitizeAttributes(List<Map<String, Object>> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> sanitized = new ArrayList<>(attributes.size());
        for (Map<String, Object> attribute : attributes) {
            Map<String, Object> copy = sanitizeMap(attribute);
            Object columnName = copy.get("columnName");
            if (columnName != null && StringUtils.hasText(String.valueOf(columnName))) {
                validateIdentifier(String.valueOf(columnName), "attributes.columnName");
            }
            sanitized.add(Collections.unmodifiableMap(copy));
        }
        return Collections.unmodifiableList(sanitized);
    }

    private Map<String, Object> sanitizeMap(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }

    private List<Object> mergeParams(List<Object> left, List<Object> right) {
        if ((left == null || left.isEmpty()) && (right == null || right.isEmpty())) {
            return Collections.emptyList();
        }
        List<Object> merged = new ArrayList<>();
        if (left != null && !left.isEmpty()) {
            merged.addAll(left);
        }
        if (right != null && !right.isEmpty()) {
            merged.addAll(right);
        }
        return merged;
    }

    private static final class DataScopeParamContext {

        private final List<Object> params = new ArrayList<>();

        private String append(Object value) {
            if (value instanceof Collection) {
                Collection<?> collection = (Collection<?>) value;
                if (collection.isEmpty()) {
                    return "(NULL)";
                }
                StringJoiner joiner = new StringJoiner(",", "(", ")");
                for (Object item : collection) {
                    params.add(item);
                    joiner.add("?");
                }
                return joiner.toString();
            }
            params.add(value);
            return "?";
        }

        private List<Object> snapshot() {
            return Collections.unmodifiableList(new ArrayList<>(params));
        }

        private void clear() {
            params.clear();
        }
    }

    private static final class DataScopeEvaluationRoot {

        private final DataScopeParamContext paramContext;

        private DataScopeEvaluationRoot(DataScopeParamContext paramContext) {
            this.paramContext = paramContext;
        }

        public String param(Object value) {
            return paramContext.append(value);
        }
    }

    private static final class NamedThreadFactory implements ThreadFactory {

        private final AtomicInteger counter = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "authz-data-scope-" + counter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }
}