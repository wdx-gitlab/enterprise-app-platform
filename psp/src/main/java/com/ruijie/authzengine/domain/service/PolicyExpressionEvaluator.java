package com.ruijie.authzengine.domain.service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;

/**
 * 策略模板表达式评估器，负责在运行时执行 authz_std_pol_template.expression_script。
 *
 * <p>使用 SpEL 引擎，支持 sub / res / env / param 四个命名空间。
 * 表达式必须返回布尔值（true = 放行，false/null = 拒绝）。</p>
 *
 * <p>安全限制：
 * <ul>
 *   <li>使用 StandardEvaluationContext 支持变量引用</li>
 *   <li>不注入 rootObject（避免反射调用攻击面）</li>
 *   <li>只注入只读的 Map 变量</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
public class PolicyExpressionEvaluator {

    /**
     * 策略表达式评估结果。
     */
    public enum EvalResult {
        /** 表达式返回 true，策略通过 */
        PASS,
        /** 表达式返回 false 或 null，策略不通过 */
        FAIL,
        /** 表达式执行异常，无法确定 */
        ERROR
    }

    private final ExpressionParser parser = new SpelExpressionParser();

    /**
     * 评估策略模板表达式。
     *
     * @param expressionScript SpEL 表达式脚本（如 "#sub['dept'] == 'SALES' and #env['hour'] >= 9"）
     * @param sub  主体属性命名空间（sub.*）
     * @param res  资源属性命名空间（res.*）
     * @param env  环境属性命名空间（env.*）
     * @param param 策略参数命名空间（param.*），来自 authz_assignment.policy_params
     * @return 评估结果：PASS / FAIL / ERROR
     */
    public EvalResult evaluate(String expressionScript,
                               Map<String, Object> sub,
                               Map<String, Object> res,
                               Map<String, Object> env,
                               Map<String, Object> param) {
        if (expressionScript == null || expressionScript.trim().isEmpty()) {
            log.debug("[策略评估] 表达式为空，视为 PASS（无条件放行）");
            return EvalResult.PASS;
        }
        try {
            // 构建 rootObject，同时支持 env.hour 和 #env['hour'] 两种表达式写法
            Map<String, Object> rootMap = new LinkedHashMap<>();
            rootMap.put("sub", safe(sub));
            rootMap.put("res", safe(res));
            rootMap.put("env", safe(env));
            rootMap.put("param", safe(param));
            StandardEvaluationContext context = new StandardEvaluationContext(Collections.unmodifiableMap(rootMap));
            // 注册 MapAccessor，使 env.hour 等属性访问语法可解析为 Map.get("key")
            context.addPropertyAccessor(new MapAccessor());
            // 注入四个命名空间变量，表达式中通过 #sub、#res、#env、#param 访问
            context.setVariable("sub", safe(sub));
            context.setVariable("res", safe(res));
            context.setVariable("env", safe(env));
            context.setVariable("param", safe(param));

            Expression expression = parser.parseExpression(expressionScript);
            Object result = expression.getValue(context);

            if (result instanceof Boolean) {
                boolean passed = (Boolean) result;
                log.debug("[策略评估] 表达式执行完成: script='{}', result={}", expressionScript, passed);
                return passed ? EvalResult.PASS : EvalResult.FAIL;
            }
            // 非布尔返回值视为 FAIL
            log.warn("[策略评估] 表达式返回非布尔值: script='{}', resultType={}, result={}",
                expressionScript,
                result == null ? "null" : result.getClass().getSimpleName(),
                result);
            return EvalResult.FAIL;
        } catch (Exception e) {
            log.error("[策略评估] 表达式执行异常: script='{}', error={}", expressionScript, e.getMessage(), e);
            return EvalResult.ERROR;
        }
    }

    private Map<String, Object> safe(Map<String, Object> map) {
        return map == null ? Collections.<String, Object>emptyMap() : Collections.unmodifiableMap(new LinkedHashMap<>(map));
    }
}
