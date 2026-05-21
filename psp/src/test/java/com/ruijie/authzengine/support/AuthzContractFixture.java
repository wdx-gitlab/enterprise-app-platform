package com.ruijie.authzengine.support;

import java.util.HashMap;
import java.util.Map;

/**
 * 004 契约回归夹具：统一复用 decision/data-scope 成功路径与标准错误响应样本。
 * <p>
 * 提供标准请求体构造方法和断言辅助常量，供 AuthzDecisionControllerTest 与
 * AuthzEngineApplicationIntegrationTest 复用。
 * </p>
 */
public final class AuthzContractFixture {

    /** 默认测试租户 */
    public static final String TENANT_ID = "T001";

    /** 默认测试应用 */
    public static final String APP_CODE = "CRM";

    /** 正常路径权限决策端点 */
    public static final String CHECK_ENDPOINT = "/authz-engine/api/v1/authz/check";

    /** 数据范围解析端点 */
    public static final String DATA_SCOPE_ENDPOINT = "/authz-engine/api/v1/authz/data-scope/resolve";

    /** 批量权限决策端点 */
    public static final String BATCH_CHECK_ENDPOINT = "/authz-engine/api/v1/authz/batch-check";

    /** 统一成功响应码 */
    public static final String SUCCESS_CODE = "0";

    /** 鉴权集成错误码（外部 Hook 异常） */
    public static final String INTEGRATION_ERROR_CODE = "AUTHZ-503";

    /** 鉴权系统错误码 */
    public static final String SYSTEM_ERROR_CODE = "AUTHZ-500";

    /** 请求参数校验错误码 */
    public static final String VALIDATION_ERROR_CODE = "AUTHZ-400";

    private AuthzContractFixture() {
    }

    /**
     * 构造标准单次鉴权请求体。
     *
     * @param subjectId    主体 ID
     * @param subjectModel 主体模型编码
     * @param boModelId    BO 模型id
     * @param action       动作编码
     * @return MockMvc 可直接序列化的请求 Map
     */
    public static Map<String, Object> buildCheckRequest(String subjectId, String subjectModel, String boModelId, String action) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("tenantId", TENANT_ID);
        payload.put("appCode", APP_CODE);
        payload.put("action", action);
        payload.put("subject", buildSubject(subjectId, subjectModel));
        payload.put("resource", buildResource("RES_DATA_BO", boModelId));
        return payload;
    }

    /**
     * 构造标准数据范围解析请求体。
     *
     * @param subjectId           主体 ID
     * @param policyTemplateCode  策略模板编码
     * @param boModelId           BO 模型id
     * @return MockMvc 可直接序列化的请求 Map
     */
    public static Map<String, Object> buildDataScopeRequest(String subjectId, String policyTemplateCode, String boModelId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("tenantId", TENANT_ID);
        payload.put("appCode", APP_CODE);
        payload.put("policyTemplateCode", policyTemplateCode);
        payload.put("subject", buildSubject(subjectId, "SUB_USER"));
        payload.put("resource", buildResource("RES_DATA_BO", boModelId));
        return payload;
    }

    /**
     * 构造主体描述符。
     */
    public static Map<String, Object> buildSubject(String subjectId, String subjectModel) {
        Map<String, Object> subject = new HashMap<>();
        subject.put("subjectId", subjectId);
        subject.put("subjectModel", subjectModel);
        return subject;
    }

    /**
     * 构造资源描述符。
     */
    public static Map<String, Object> buildResource(String resourceModel, String resourceId) {
        Map<String, Object> resource = new HashMap<>();
        resource.put("resourceModel", resourceModel);
        resource.put("resourceId", resourceId);
        return resource;
    }
}
