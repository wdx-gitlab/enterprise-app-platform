package com.ruijie.authzengine.infrastructure.authz;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruijie.authzengine.shared.exception.BusinessException;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BoSchemaJsonValidatorTest {

    private static final String VALID_NEW_SCHEMA = "{"
        + "\"entities\":[{"
        + "\"code\":\"customer_main\"," 
        + "\"name\":\"客户主实体\"," 
        + "\"isPrimary\":true,"
        + "\"tableName\":\"biz_customer\"," 
        + "\"rowLevelRuleEntries\":[\"dept_id\"],"
        + "\"attributes\":["
        + "{\"code\":\"id\",\"fieldName\":\"id\",\"columnName\":\"id\",\"name\":\"主键\",\"type\":\"LONG\",\"isPk\":true},"
        + "{\"code\":\"dept_id\",\"fieldName\":\"deptId\",\"columnName\":\"dept_id\",\"name\":\"部门\",\"type\":\"STRING\",\"isPk\":false,\"filterable\":true},"
        + "{\"code\":\"mobile\",\"fieldName\":\"mobile\",\"columnName\":\"mobile\",\"name\":\"手机号\",\"type\":\"STRING\",\"isPk\":false,\"fieldControl\":true,\"fieldControlStrategy\":\"MASK\"}"
        + "]}],"
        + "\"operations\":[{\"code\":\"QUERY\",\"name\":\"查询\",\"scope\":\"BO\"}]"
        + "}";

    @Test
    void shouldAllowLegacySchemaWithoutVersion() {
        BoSchemaJsonValidator validator = new BoSchemaJsonValidator(new ObjectMapper());

        Assertions.assertDoesNotThrow(() -> validator.validateForRuntime(
            "{\"fields\":[{\"code\":\"ownerDept\",\"type\":\"STRING\"}],\"policyContext\":{},\"sqlTranslation\":{}}"
        ));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{}",
        "{\"version\":\"2.0\",\"fields\":{\"code\":\"ownerDept\"}}",
        "[]",
        "{\"version\":\"3.0\",\"fields\":[{\"code\":\"ownerDept\",\"type\":\"STRING\"}]}",
        "{\"version\":\"2.0\",\"fields\":[{\"code\":\"stage\",\"type\":\"STRING\",\"exposedToPolicy\":true}],\"policyContext\":{\"allowFields\":[\"ownerDept\"]}}"
    })
    void shouldRejectInvalidSchemaOnSave(String schemaJson) {
        BoSchemaJsonValidator validator = new BoSchemaJsonValidator(new ObjectMapper());

        Assertions.assertThrows(BusinessException.class, () -> validator.validateForSave(schemaJson));
    }

    @Test
    void shouldAllowNewSchemaOnSave() {
        BoSchemaJsonValidator validator = new BoSchemaJsonValidator(new ObjectMapper());

        Assertions.assertDoesNotThrow(() -> validator.validateForSave(VALID_NEW_SCHEMA));
    }

    @Test
    void shouldRejectNewSchemaWithoutPrimaryEntityOnSave() {
        BoSchemaJsonValidator validator = new BoSchemaJsonValidator(new ObjectMapper());

        BusinessException exception = Assertions.assertThrows(
            BusinessException.class,
            () -> validator.validateForSave(VALID_NEW_SCHEMA.replace("\"isPrimary\":true", "\"isPrimary\":false"))
        );

        Assertions.assertEquals("AUTHZ-400", exception.getCode());
        Assertions.assertTrue(exception.getMessage().contains("必须且只能有一个 isPrimary=true 的实体"));
    }

    @Test
    void shouldProjectOnlyDeclaredFieldsForNewSchema() {
        BoSchemaJsonValidator validator = new BoSchemaJsonValidator(new ObjectMapper());

        BoSchemaJsonValidator.PolicyExposure policyExposure = validator.resolvePolicyExposure(VALID_NEW_SCHEMA);

        Map<String, Object> rawAttributes = new java.util.LinkedHashMap<>();
        rawAttributes.put("id", 1L);
        rawAttributes.put("dept_id", "SALES");
        rawAttributes.put("mobile", "13800000000");
        rawAttributes.put("ignoredField", "SHOULD_NOT_PASS");

        Map<String, Object> projected = policyExposure.projectAttributes(rawAttributes);

        Assertions.assertEquals("res", policyExposure.getResourcePrefix());
        Assertions.assertEquals(3, projected.size());
        Assertions.assertEquals(1L, projected.get("id"));
        Assertions.assertEquals("SALES", projected.get("dept_id"));
        Assertions.assertEquals("13800000000", projected.get("mobile"));
        Assertions.assertFalse(projected.containsKey("ignoredField"));
    }

    @Test
    void shouldProjectOnlyAllowedPolicyFieldsForLegacySchema() {
        BoSchemaJsonValidator validator = new BoSchemaJsonValidator(new ObjectMapper());

        BoSchemaJsonValidator.PolicyExposure policyExposure = validator.resolvePolicyExposure(
            "{\"version\":\"2.0\",\"fields\":["
                + "{\"code\":\"stage\",\"type\":\"STRING\",\"source\":\"f_stage\",\"exposedToPolicy\":true},"
                + "{\"code\":\"ownerDept\",\"type\":\"STRING\",\"source\":\"f_owner_dept\",\"exposedToPolicy\":false},"
                + "{\"code\":\"amount\",\"type\":\"NUMBER\",\"source\":\"f_amount\",\"exposedToPolicy\":true}"
                + "],\"policyContext\":{\"resourcePrefix\":\"bizRes\",\"allowFields\":[\"stage\"]},\"sqlTranslation\":{\"mode\":\"FIELD_MAPPING\"}}"
        );

        Map<String, Object> rawAttributes = new java.util.LinkedHashMap<>();
        rawAttributes.put("stage", "APPROVING");
        rawAttributes.put("ownerDept", "SALES");
        rawAttributes.put("amount", 1000);

        Map<String, Object> projected = policyExposure.projectAttributes(rawAttributes);

        Assertions.assertEquals("bizRes", policyExposure.getResourcePrefix());
        Assertions.assertEquals(1, projected.size());
        Assertions.assertEquals("APPROVING", projected.get("stage"));
        Assertions.assertFalse(projected.containsKey("ownerDept"));
        Assertions.assertFalse(projected.containsKey("amount"));
    }
}