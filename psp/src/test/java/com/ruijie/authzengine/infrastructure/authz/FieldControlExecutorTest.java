package com.ruijie.authzengine.infrastructure.authz;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruijie.authzengine.domain.service.FieldMaskExpressionEvaluator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class FieldControlExecutorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldPassFieldNameAndParamsToMaskEvaluator() {
        FieldControlExecutor executor = new FieldControlExecutor(new FieldMaskExpressionEvaluator());
        ObjectNode node = objectMapper.createObjectNode();
        node.put("mobilePhone", "13812345678");

        Map<String, Object> control = new LinkedHashMap<>();
        control.put("code", "mobilePhone");
        control.put("fieldName", "mobilePhone");
        control.put("action", "MASK");
        control.put("maskScript", "#fieldName == 'mobilePhone' ? #originalValue.substring(0, #param['keepHead']) + '****' : #originalValue");
        control.put("params", Collections.singletonMap("keepHead", 3));

        executor.apply(node, Collections.singletonList(control));

        Assertions.assertEquals("138****", node.get("mobilePhone").asText());
    }

    @Test
    void shouldFallbackToColumnNameWhenFieldNameMissing() {
        FieldControlExecutor executor = new FieldControlExecutor(new FieldMaskExpressionEvaluator());
        ObjectNode node = objectMapper.createObjectNode();
        node.put("mobile_phone", "13812345678");

        Map<String, Object> control = new LinkedHashMap<>();
        control.put("code", "mobilePhone");
        control.put("columnName", "mobile_phone");
        control.put("action", "HIDE");

        executor.apply(node, Collections.singletonList(control));

        Assertions.assertFalse(node.has("mobile_phone"));
    }
}