package com.ruijie.authzengine.domain.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PolicyExpressionEvaluator 单元测试。
 */
class PolicyExpressionEvaluatorTest {

    private PolicyExpressionEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new PolicyExpressionEvaluator();
    }

    @Test
    void 空表达式视为PASS() {
        assertEquals(PolicyExpressionEvaluator.EvalResult.PASS,
            evaluator.evaluate(null, null, null, null, null));
        assertEquals(PolicyExpressionEvaluator.EvalResult.PASS,
            evaluator.evaluate("", null, null, null, null));
        assertEquals(PolicyExpressionEvaluator.EvalResult.PASS,
            evaluator.evaluate("   ", null, null, null, null));
    }

    @Test
    void 简单布尔表达式返回true时为PASS() {
        assertEquals(PolicyExpressionEvaluator.EvalResult.PASS,
            evaluator.evaluate("true", null, null, null, null));
    }

    @Test
    void 简单布尔表达式返回false时为FAIL() {
        assertEquals(PolicyExpressionEvaluator.EvalResult.FAIL,
            evaluator.evaluate("false", null, null, null, null));
    }

    @Test
    void sub命名空间变量访问() {
        Map<String, Object> sub = new HashMap<>();
        sub.put("dept", "SALES");
        assertEquals(PolicyExpressionEvaluator.EvalResult.PASS,
            evaluator.evaluate("#sub['dept'] == 'SALES'", sub, null, null, null));
        assertEquals(PolicyExpressionEvaluator.EvalResult.FAIL,
            evaluator.evaluate("#sub['dept'] == 'HR'", sub, null, null, null));
    }

    @Test
    void env命名空间时间变量访问() {
        Map<String, Object> env = new HashMap<>();
        env.put("hour", 10);
        assertEquals(PolicyExpressionEvaluator.EvalResult.PASS,
            evaluator.evaluate("#env['hour'] >= 9 and #env['hour'] < 18", null, null, env, null));
        env.put("hour", 20);
        assertEquals(PolicyExpressionEvaluator.EvalResult.FAIL,
            evaluator.evaluate("#env['hour'] >= 9 and #env['hour'] < 18", null, null, env, null));
    }

    @Test
    void res命名空间资源属性访问() {
        Map<String, Object> res = new HashMap<>();
        res.put("status", "PENDING");
        assertEquals(PolicyExpressionEvaluator.EvalResult.PASS,
            evaluator.evaluate("#res['status'] == 'PENDING'", null, res, null, null));
    }

    @Test
    void param命名空间参数访问() {
        Map<String, Object> param = new HashMap<>();
        param.put("maxAmount", 10000);
        assertEquals(PolicyExpressionEvaluator.EvalResult.PASS,
            evaluator.evaluate("#param['maxAmount'] > 5000", null, null, null, param));
    }

    @Test
    void 多命名空间组合表达式() {
        Map<String, Object> sub = new HashMap<>();
        sub.put("dept", "SALES");
        Map<String, Object> env = new HashMap<>();
        env.put("hour", 10);
        Map<String, Object> res = new HashMap<>();
        res.put("status", "PENDING");

        // 三个条件全满足
        assertEquals(PolicyExpressionEvaluator.EvalResult.PASS,
            evaluator.evaluate(
                "#sub['dept'] == 'SALES' and #env['hour'] >= 9 and #res['status'] == 'PENDING'",
                sub, res, env, null));

        // 其中一个条件不满足
        env.put("hour", 6);
        assertEquals(PolicyExpressionEvaluator.EvalResult.FAIL,
            evaluator.evaluate(
                "#sub['dept'] == 'SALES' and #env['hour'] >= 9 and #res['status'] == 'PENDING'",
                sub, res, env, null));
    }

    @Test
    void 非法表达式返回ERROR() {
        assertEquals(PolicyExpressionEvaluator.EvalResult.ERROR,
            evaluator.evaluate("this is not a valid expression !!!", null, null, null, null));
    }

    @Test
    void 非布尔返回值视为FAIL() {
        assertEquals(PolicyExpressionEvaluator.EvalResult.FAIL,
            evaluator.evaluate("'hello'", null, null, null, null));
    }

    @Test
    void 不存在的变量key返回null时FAIL() {
        Map<String, Object> sub = new HashMap<>();
        // sub 中没有 dept 键，#sub['dept'] 返回 null，null == 'SALES' 为 false
        assertEquals(PolicyExpressionEvaluator.EvalResult.FAIL,
            evaluator.evaluate("#sub['dept'] == 'SALES'", sub, null, null, null));
    }

    @Test
    void null命名空间等同于空Map() {
        // 不传入任何命名空间，访问 #sub 应该得到空 Map，不应报错
        assertEquals(PolicyExpressionEvaluator.EvalResult.PASS,
            evaluator.evaluate("#sub.isEmpty()", null, null, null, null));
    }
}
