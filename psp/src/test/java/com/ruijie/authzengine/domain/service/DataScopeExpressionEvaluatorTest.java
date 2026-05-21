package com.ruijie.authzengine.domain.service;

import com.ruijie.authzengine.domain.model.decision.DataScopeFragment;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.expression.EvaluationException;

class DataScopeExpressionEvaluatorTest {

    @Test
    void shouldGenerateSqlAndParamsForScalarParam() {
        DataScopeExpressionEvaluator evaluator = new DataScopeExpressionEvaluator();

        DataScopeFragment fragment = evaluator.evaluate(
            "#tableName + '.owner_id = ' + param(#sub['userId'])",
            Collections.singletonMap("userId", "U100"),
            "biz_customer",
            buildAttributes(),
            Collections.emptyMap()
        );

        Assertions.assertEquals("biz_customer.owner_id = ?", fragment.getSql());
        Assertions.assertEquals(Collections.singletonList("U100"), fragment.getParams());
    }

    @Test
    void shouldGenerateSqlAndParamsForPolicyParamLookup() {
        DataScopeExpressionEvaluator evaluator = new DataScopeExpressionEvaluator();
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("calcType", "SETTLEMENT");

        DataScopeFragment fragment = evaluator.evaluate(
            "#tableName + '.calc_type = ' + param(#param['calcType'])",
            Collections.emptyMap(),
            "salary_calculation_log",
            buildAttributes(),
            params
        );

        Assertions.assertEquals("salary_calculation_log.calc_type = ?", fragment.getSql());
        Assertions.assertEquals(Collections.singletonList("SETTLEMENT"), fragment.getParams());
    }

    @Test
    void shouldTreatStringLiteralAsLiteralParamValue() {
        DataScopeExpressionEvaluator evaluator = new DataScopeExpressionEvaluator();
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("calcType", "SETTLEMENT");

        DataScopeFragment fragment = evaluator.evaluate(
            "#tableName + '.calc_type = ' + param('calcType')",
            Collections.emptyMap(),
            "salary_calculation_log",
            buildAttributes(),
            params
        );

        Assertions.assertEquals("salary_calculation_log.calc_type = ?", fragment.getSql());
        Assertions.assertEquals(Collections.singletonList("calcType"), fragment.getParams());
    }

    @Test
    void shouldExpandCollectionParamIntoPlaceholders() {
        DataScopeExpressionEvaluator evaluator = new DataScopeExpressionEvaluator();
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("deptIds", Arrays.asList(10L, 20L, 30L));

        DataScopeFragment fragment = evaluator.evaluate(
            "#tableName + '.dept_id in ' + param(#param['deptIds'])",
            Collections.emptyMap(),
            "biz_customer",
            buildAttributes(),
            params
        );

        Assertions.assertEquals("biz_customer.dept_id in (?,?,?)", fragment.getSql());
        Assertions.assertEquals(Arrays.asList(10L, 20L, 30L), fragment.getParams());
    }

    @Test
    void shouldRejectIllegalIdentifier() {
        DataScopeExpressionEvaluator evaluator = new DataScopeExpressionEvaluator();

        Assertions.assertThrows(IllegalArgumentException.class, () -> evaluator.evaluate(
            "#tableName + '.owner_id = ' + param(#sub['userId'])",
            Collections.singletonMap("userId", "U100"),
            "biz_customer;drop table authz_permission_item",
            buildAttributes(),
            Collections.emptyMap()
        ));
    }

    @Test
    void shouldRejectTypeReference() {
        DataScopeExpressionEvaluator evaluator = new DataScopeExpressionEvaluator();

        Assertions.assertThrows(EvaluationException.class, () -> evaluator.evaluate(
            "T(java.lang.Runtime).getRuntime()",
            Collections.emptyMap(),
            "biz_customer",
            buildAttributes(),
            Collections.emptyMap()
        ));
    }

    @Test
    void shouldTimeoutWhenExpressionExecutionExceedsLimit() {
        DataScopeExpressionEvaluator evaluator = new DataScopeExpressionEvaluator(1L) {
            @Override
            protected Object doEvaluate(String expressionScript, org.springframework.expression.EvaluationContext evaluationContext) {
                try {
                    Thread.sleep(20L);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
                return "biz_customer.owner_id = ?";
            }
        };

        IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class, () -> evaluator.evaluate(
            "ignored",
            Collections.emptyMap(),
            "biz_customer",
            buildAttributes(),
            Collections.emptyMap()
        ));
        Assertions.assertTrue(exception.getMessage().contains("DATA_SCOPE_TIMEOUT"));
    }

    private List<Map<String, Object>> buildAttributes() {
        Map<String, Object> attribute = new LinkedHashMap<>();
        attribute.put("code", "ownerId");
        attribute.put("fieldName", "ownerId");
        attribute.put("columnName", "owner_id");
        attribute.put("type", "STRING");
        attribute.put("fieldControl", false);
        return Collections.singletonList(attribute);
    }
}