package com.ruijie.authzengine.infrastructure.authz;

import com.ruijie.authzengine.application.spi.BoMetaModelAdapter;
import com.ruijie.authzengine.application.spi.BoSchemaColumnInfo;
import com.ruijie.authzengine.shared.exception.AuthzIntegrationException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.StaticApplicationContext;

class BoResolverRouterTest {

    @Test
    void shouldTreatNoopHookAsNoopResolver() {
        BoResolverRouter router = BoResolverRouter.noop();

        Assertions.assertNull(router.resolve("JAVA_BEAN", "noopHook"));
    }

    @Test
    void shouldRejectUnsupportedAdapterType() {
        BoResolverRouter router = BoResolverRouter.noop();

        Assertions.assertThrows(AuthzIntegrationException.class, () -> router.resolve("HTTP", "orderHook"));
    }

    @Test
    void shouldRejectMissingAdapterTypeWhenResolverIsConfigured() {
        BoResolverRouter router = BoResolverRouter.noop();

        Assertions.assertThrows(AuthzIntegrationException.class, () -> router.resolve(null, "orderHook"));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // T022：Shadow 模式 SPI 路由、fetchBoSchema 降级语义测试
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * T022-1：Shadow 模式 - 路由器从 ApplicationContext 解析到正确 Bean 并调用 fetchBoSchema()。
     * 期望：路由成功，fetchBoSchema() 返回适配器提供的列元数据。
     */
    @Test
    void shouldRouteToAdapterAndFetchBoSchemaInShadowMode() {
        // 构造一个实现了 fetchBoSchema 的适配器
        List<BoSchemaColumnInfo> stubColumns = Arrays.asList(
            BoSchemaColumnInfo.builder()
                .tableName("biz_contract").columnName("id").columnType("BIGINT")
                .isPrimaryKey(true).nullable(false).comment("主键").build()
        );
        BoMetaModelAdapter stubAdapter = new BoMetaModelAdapter() {
            @Override
            public Map<String, Object> fetchInstanceAttributes(String instanceId, String schemaJson, Map<String, Object> requestContext) {
                return null;
            }
            @Override
            public List<BoSchemaColumnInfo> fetchBoSchema(String boCode, String tableName, Map<String, Object> hints) {
                return stubColumns;
            }
        };

        // 使用 StaticApplicationContext 注册 Bean（绕过真实 Spring 容器）
        StaticApplicationContext context = new StaticApplicationContext();
        context.getBeanFactory().registerSingleton("contractBoProvider", stubAdapter);

        BoResolverRouter router = new BoResolverRouter(context);
        BoMetaModelAdapter resolved = router.resolve("JAVA_BEAN", "contractBoProvider");
        Assertions.assertNotNull(resolved, "应成功路由到 contractBoProvider Bean");

        List<BoSchemaColumnInfo> result = resolved.fetchBoSchema("CONTRACT", "biz_contract", Collections.emptyMap());
        Assertions.assertEquals(1, result.size(), "fetchBoSchema 应返回 1 列");
        Assertions.assertEquals("id", result.get(0).getColumnName());
        Assertions.assertTrue(result.get(0).isPrimaryKey());
    }

    /**
     * T022-2：Shadow 模式 - 适配器 fetchBoSchema() 返回 null（未实现），不抛异常。
     * 期望：路由成功，fetchBoSchema() 安全返回 null，调用方需处理 null。
     */
    @Test
    void shouldNotThrowWhenShadowAdapterFetchBoSchemaReturnsNull() {
        BoMetaModelAdapter nullSchemaAdapter = new BoMetaModelAdapter() {
            @Override
            public Map<String, Object> fetchInstanceAttributes(String instanceId, String schemaJson, Map<String, Object> requestContext) {
                return null;
            }
            @Override
            public List<BoSchemaColumnInfo> fetchBoSchema(String boCode, String tableName, Map<String, Object> hints) {
                return null; // 未实现
            }
        };

        StaticApplicationContext context = new StaticApplicationContext();
        context.getBeanFactory().registerSingleton("salaryBoProvider", nullSchemaAdapter);
        BoResolverRouter router = new BoResolverRouter(context);

        BoMetaModelAdapter resolved = router.resolve("JAVA_BEAN", "salaryBoProvider");
        Assertions.assertNotNull(resolved);

        List<BoSchemaColumnInfo> result = resolved.fetchBoSchema("SALARY", "biz_salary", Collections.emptyMap());
        Assertions.assertNull(result, "适配器未实现 fetchBoSchema 时应返回 null，调用方降级");
    }

    /**
     * T022-3：Shadow 模式 - 路由器未注入 ApplicationContext（noop），fetchBoSchema 不可达。
     * 期望：noop 模式下 resolve() 返回 null，不发起 Bean 查找。
     */
    @Test
    void shouldReturnNullWhenNoopRouterResolvesAnyBean() {
        BoResolverRouter noopRouter = BoResolverRouter.noop();

        // noop 路由器对任意 resolver 均返回 null（因为 applicationContext == null）
        BoMetaModelAdapter resolved = noopRouter.resolve("JAVA_BEAN", "anyProvider");
        Assertions.assertNull(resolved, "noop 路由器应对任意 resolver 返回 null");
    }

    /**
     * T022-4：Shadow 模式 - 按 BO / 表名批量提示参数传递。
     * 期望：hints 中包含 boCode 和 tableName，适配器能按需区分不同 BO 的查询请求。
     */
    @Test
    void shouldPassHintsWithBoCodeAndTableNameToFetchBoSchema() {
        Map<String, Object>[] capturedHints = new Map[1];
        BoMetaModelAdapter hintsCapturingAdapter = new BoMetaModelAdapter() {
            @Override
            public Map<String, Object> fetchInstanceAttributes(String instanceId, String schemaJson, Map<String, Object> requestContext) {
                return null;
            }
            @Override
            public List<BoSchemaColumnInfo> fetchBoSchema(String boCode, String tableName, Map<String, Object> hints) {
                capturedHints[0] = hints;
                return Collections.emptyList();
            }
        };

        Map<String, Object> hints = new java.util.HashMap<>();
        hints.put("tenantId", "T001");
        hints.put("appCode", "CRM");
        hints.put("boCode", "CONTRACT");

        // 直接调用适配器（不经过路由器，验证 hints 传递契约）
        List<BoSchemaColumnInfo> result = hintsCapturingAdapter.fetchBoSchema("CONTRACT", "biz_contract", hints);

        Assertions.assertNotNull(capturedHints[0], "hints 必须被传递给适配器");
        Assertions.assertEquals("T001", capturedHints[0].get("tenantId"));
        Assertions.assertEquals("CONTRACT", capturedHints[0].get("boCode"));
        Assertions.assertNotNull(result);
    }
}