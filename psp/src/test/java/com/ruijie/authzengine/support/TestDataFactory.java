package com.ruijie.authzengine.support;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 治理测试数据工厂。
 */
public final class TestDataFactory {

    private TestDataFactory() {
    }

    public static Map<String, Object> metaModelRequest() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tenantId", "T001");
        payload.put("appCode", "CRM");
        payload.put("modelCode", "RES_API_DEMO");
        payload.put("modelName", "接口资源");
        payload.put("category", "RESOURCE");
        payload.put("adapterType", "JAVA_BEAN");
        payload.put("resolver", "noopHook");
        payload.put("schemaView", "{}");
        return payload;
    }

    public static Map<String, Object> userRequest() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tenantId", "T001");
        payload.put("appCode", "CRM");
        payload.put("staffNo", "U100");
        payload.put("userId", "zhangsan");
        payload.put("staffName", "张三");
        payload.put("orgCode", "ORG-SALES");
        payload.put("status", "ENABLED");
        return payload;
    }

    public static Map<String, Object> apiRequest() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tenantId", "T001");
        payload.put("appCode", "CRM");
        payload.put("apiCode", "API-CONTRACT-QUERY");
        payload.put("apiName", "合同查询接口");
        payload.put("httpMethod", "GET");
        payload.put("uriPattern", "/api/contracts/query");
        payload.put("status", "ENABLED");
        return payload;
    }

    public static LocalDateTime futureTime() {
        return LocalDateTime.of(2030, 1, 1, 0, 0);
    }
}