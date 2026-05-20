package com.ruijie.dapengine.migration;

import com.ruijie.dapengine.autoconfigure.DapEngineAutoConfiguration;
import com.ruijie.dapengine.autoconfigure.DapEngineJdbcTemplate;
import com.ruijie.dapengine.common.enums.SchemaStatus;
import com.ruijie.dapengine.common.exception.DapValidationException;
import com.ruijie.dapengine.common.model.FieldConfigRequest;
import com.ruijie.dapengine.common.model.SchemaChangeResult;
import com.ruijie.dapengine.common.model.SubjectRequest;
import com.ruijie.dapengine.admin.service.MetadataConfigService;
import com.ruijie.dapengine.admin.service.SchemaStatusService;
import com.ruijie.dapengine.sync.SyncScheduler;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * DapEngineSchemaInitializer 单元测试（H2 内存数据库）。
 * 覆盖 US1（建表）、US2（加列）、US3（类型扩容）、US4（启动兜底）、US5（SyncScheduler）。
 */
@RunWith(SpringRunner.class)
public class DapEngineSchemaInitializerTest {

    private static final String H2_URL_TPL =
        "jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1;MODE=MySQL";
    private static final String VALID_KEY = "TestEncryptKey32CharactersLong!!";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(JacksonAutoConfiguration.class, DapEngineAutoConfiguration.class);

    private SubjectRequest buildRequest(String code, String name, boolean isTree,
                                        List<FieldConfigRequest> fields) {
        SubjectRequest req = new SubjectRequest();
        SubjectRequest.SubjectInfo info = new SubjectRequest.SubjectInfo();
        info.setCode(code);
        info.setName(name);
        info.setTree(isTree);
        info.setStatus(1);
        req.setSubject(info);
        req.setFields(fields);
        return req;
    }

    private FieldConfigRequest buildField(String name, String type, String label) {
        FieldConfigRequest f = new FieldConfigRequest();
        f.setFieldName(name);
        f.setFieldType(type);
        f.setFieldLabel(label);
        f.setSortOrder(10);
        return f;
    }

    private FieldConfigRequest buildField(String name, String type, int maxLength, String label) {
        FieldConfigRequest field = buildField(name, type, label);
        field.setMaxLength(maxLength);
        return field;
    }

    // =========================================================================
    // US1: 建表
    // =========================================================================

    @Test
    public void applySchema_creates_table_for_new_subject() {
        contextRunner.withPropertyValues(
            "dap.engine.tenant-id=t1", "dap.engine.app-code=app1",
            "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "schema_us1_create_test"),
            "rj.unify.engine.datasource.username=sa", "rj.unify.engine.datasource.password=",
            "dap.engine.security.encrypt-key=" + VALID_KEY
        ).run(ctx -> {
            DapEngineJdbcTemplate jdbcTpl = ctx.getBean(DapEngineJdbcTemplate.class);
            MetadataConfigService metaSvc = ctx.getBean(MetadataConfigService.class);
            SchemaStatusService schemaSvc = ctx.getBean(SchemaStatusService.class);
            DapEngineSchemaInitializer initializer =
                new DapEngineSchemaInitializer(jdbcTpl, metaSvc, schemaSvc);

            // 保存 Subject 元数据
            metaSvc.saveSubjectConfig("CUSTOMER",
                buildRequest("CUSTOMER", "客户", false,
                    Collections.singletonList(buildField("credit_code", "STRING", "信用代码"))),
                "admin");

            SchemaChangeResult result = initializer.applySchema("CUSTOMER");

            assertThat(result.getSubject()).isEqualTo("CUSTOMER");
            assertThat(result.getTable()).isEqualTo("dap_customer");
            assertThat(result.getExecutedDdl()).isNotEmpty();
            assertThat(result.getExecutedDdl().get(0)).contains("CREATE TABLE IF NOT EXISTS dap_customer");

            // 验证表已存在
            assertThat(schemaSvc.tableExists("dap_customer")).isTrue();

            // 验证 schemaStatus = APPLIED
            List<com.ruijie.dapengine.common.model.FieldConfigDTO> activeFields =
                metaSvc.getActiveFieldDTOs("CUSTOMER");
            assertThat(schemaSvc.computeStatus("CUSTOMER", activeFields))
                .isEqualTo(SchemaStatus.APPLIED);
        });
    }

    @Test
    public void applySchema_creates_varchar_with_custom_length() {
        contextRunner.withPropertyValues(
            "dap.engine.tenant-id=t1", "dap.engine.app-code=app1",
            "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "schema_custom_length_create_test"),
            "rj.unify.engine.datasource.username=sa", "rj.unify.engine.datasource.password=",
            "dap.engine.security.encrypt-key=" + VALID_KEY
        ).run(ctx -> {
            DapEngineJdbcTemplate jdbcTpl = ctx.getBean(DapEngineJdbcTemplate.class);
            MetadataConfigService metaSvc = ctx.getBean(MetadataConfigService.class);
            SchemaStatusService schemaSvc = ctx.getBean(SchemaStatusService.class);
            DapEngineSchemaInitializer initializer =
                new DapEngineSchemaInitializer(jdbcTpl, metaSvc, schemaSvc);

            metaSvc.saveSubjectConfig("CUSTOMER",
                buildRequest("CUSTOMER", "客户", false,
                    Collections.singletonList(buildField("short_name", "STRING", 50, "简称"))),
                "admin");

            SchemaChangeResult result = initializer.applySchema("CUSTOMER");

            assertThat(result.getExecutedDdl()).hasSize(1);
            assertThat(result.getExecutedDdl().get(0)).contains("short_name VARCHAR(50)");
            assertThat(schemaSvc.getColumnDefinitionMap("dap_customer").get("short_name").getMaxLength()).isEqualTo(50L);
        });
    }

    @Test
    public void applySchema_idempotent_when_table_exists() {
        contextRunner.withPropertyValues(
            "dap.engine.tenant-id=t1", "dap.engine.app-code=app1",
            "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "schema_us1_idempotent_test"),
            "rj.unify.engine.datasource.username=sa", "rj.unify.engine.datasource.password=",
            "dap.engine.security.encrypt-key=" + VALID_KEY
        ).run(ctx -> {
            DapEngineJdbcTemplate jdbcTpl = ctx.getBean(DapEngineJdbcTemplate.class);
            MetadataConfigService metaSvc = ctx.getBean(MetadataConfigService.class);
            SchemaStatusService schemaSvc = ctx.getBean(SchemaStatusService.class);
            DapEngineSchemaInitializer initializer =
                new DapEngineSchemaInitializer(jdbcTpl, metaSvc, schemaSvc);

            metaSvc.saveSubjectConfig("PRODUCT",
                buildRequest("PRODUCT", "产品", false, Collections.<FieldConfigRequest>emptyList()), "admin");

            // 第一次 apply
            initializer.applySchema("PRODUCT");
            // 第二次 apply（幂等）
            SchemaChangeResult second = initializer.applySchema("PRODUCT");

            assertThat(second.getExecutedDdl()).isEmpty();
        });
    }

    @Test
    public void applySchema_throws_for_nonexistent_subject() {
        contextRunner.withPropertyValues(
            "dap.engine.tenant-id=t1", "dap.engine.app-code=app1",
            "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "schema_us1_noexist_test"),
            "rj.unify.engine.datasource.username=sa", "rj.unify.engine.datasource.password=",
            "dap.engine.security.encrypt-key=" + VALID_KEY
        ).run(ctx -> {
            DapEngineJdbcTemplate jdbcTpl = ctx.getBean(DapEngineJdbcTemplate.class);
            MetadataConfigService metaSvc = ctx.getBean(MetadataConfigService.class);
            SchemaStatusService schemaSvc = ctx.getBean(SchemaStatusService.class);
            DapEngineSchemaInitializer initializer =
                new DapEngineSchemaInitializer(jdbcTpl, metaSvc, schemaSvc);

            assertThatThrownBy(() -> initializer.applySchema("NOEXIST_SUBJECT"))
                .isInstanceOf(DapValidationException.class)
                .hasMessageContaining("不存在或已删除");
        });
    }

    // =========================================================================
    // US2: 加列
    // =========================================================================

    @Test
    public void applySchema_adds_missing_column() {
        contextRunner.withPropertyValues(
            "dap.engine.tenant-id=t1", "dap.engine.app-code=app1",
            "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "schema_us2_addcol_test"),
            "rj.unify.engine.datasource.username=sa", "rj.unify.engine.datasource.password=",
            "dap.engine.security.encrypt-key=" + VALID_KEY
        ).run(ctx -> {
            DapEngineJdbcTemplate jdbcTpl = ctx.getBean(DapEngineJdbcTemplate.class);
            MetadataConfigService metaSvc = ctx.getBean(MetadataConfigService.class);
            SchemaStatusService schemaSvc = ctx.getBean(SchemaStatusService.class);
            DapEngineSchemaInitializer initializer =
                new DapEngineSchemaInitializer(jdbcTpl, metaSvc, schemaSvc);

            // 先建表（无自定义字段）
            metaSvc.saveSubjectConfig("SUPPLIER",
                buildRequest("SUPPLIER", "供应商", false, Collections.<FieldConfigRequest>emptyList()), "admin");
            initializer.applySchema("SUPPLIER");

            // 再追加字段 spec_no
            metaSvc.saveSubjectConfig("SUPPLIER",
                buildRequest("SUPPLIER", "供应商", false,
                    Collections.singletonList(buildField("spec_no", "STRING", "规格编码"))), "admin");
            SchemaChangeResult result = initializer.applySchema("SUPPLIER");

            assertThat(result.getExecutedDdl()).hasSize(1);
            assertThat(result.getExecutedDdl().get(0)).contains("ADD COLUMN spec_no");

            // 验证列存在
            Map<String, String> typeMap = schemaSvc.getColumnTypeMap("dap_supplier");
            assertThat(typeMap).containsKey("spec_no");
        });
    }

    @Test
    public void applySchema_skips_existing_column() {
        contextRunner.withPropertyValues(
            "dap.engine.tenant-id=t1", "dap.engine.app-code=app1",
            "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "schema_us2_skip_test"),
            "rj.unify.engine.datasource.username=sa", "rj.unify.engine.datasource.password=",
            "dap.engine.security.encrypt-key=" + VALID_KEY
        ).run(ctx -> {
            DapEngineJdbcTemplate jdbcTpl = ctx.getBean(DapEngineJdbcTemplate.class);
            MetadataConfigService metaSvc = ctx.getBean(MetadataConfigService.class);
            SchemaStatusService schemaSvc = ctx.getBean(SchemaStatusService.class);
            DapEngineSchemaInitializer initializer =
                new DapEngineSchemaInitializer(jdbcTpl, metaSvc, schemaSvc);

            metaSvc.saveSubjectConfig("ITEM",
                buildRequest("ITEM", "物品", false,
                    Collections.singletonList(buildField("item_code", "STRING", "物品编码"))), "admin");
            initializer.applySchema("ITEM");

            // 相同字段再次 apply
            SchemaChangeResult result = initializer.applySchema("ITEM");
            assertThat(result.getExecutedDdl()).isEmpty();
        });
    }

    @Test
    public void applySchema_does_not_add_system_name_column_again() {
        contextRunner.withPropertyValues(
            "dap.engine.tenant-id=t1", "dap.engine.app-code=app1",
            "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "schema_us2_system_name_test"),
            "rj.unify.engine.datasource.username=sa", "rj.unify.engine.datasource.password=",
            "dap.engine.security.encrypt-key=" + VALID_KEY
        ).run(ctx -> {
            DapEngineJdbcTemplate jdbcTpl = ctx.getBean(DapEngineJdbcTemplate.class);
            MetadataConfigService metaSvc = ctx.getBean(MetadataConfigService.class);
            SchemaStatusService schemaSvc = ctx.getBean(SchemaStatusService.class);
            DapEngineSchemaInitializer initializer =
                new DapEngineSchemaInitializer(jdbcTpl, metaSvc, schemaSvc);

            metaSvc.saveSubjectConfig("CUSTOMER",
                buildRequest("CUSTOMER", "客户", false,
                    Collections.singletonList(buildField("customer_name", "STRING", "客户名称"))),
                "admin");

            SchemaChangeResult first = initializer.applySchema("CUSTOMER");
            assertThat(first.getExecutedDdl()).anyMatch(ddl -> ddl.contains("CREATE TABLE IF NOT EXISTS dap_customer"));

            Map<String, String> typeMap = schemaSvc.getColumnTypeMap("dap_customer");
            assertThat(typeMap).containsKeys("name", "customer_name");

            SchemaChangeResult second = initializer.applySchema("CUSTOMER");
            assertThat(second.getExecutedDdl()).isEmpty();
        });
    }

    @Test
    public void applySchema_does_not_drop_deprecated_column() {
        contextRunner.withPropertyValues(
            "dap.engine.tenant-id=t1", "dap.engine.app-code=app1",
            "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "schema_us2_nodrop_test"),
            "rj.unify.engine.datasource.username=sa", "rj.unify.engine.datasource.password=",
            "dap.engine.security.encrypt-key=" + VALID_KEY
        ).run(ctx -> {
            DapEngineJdbcTemplate jdbcTpl = ctx.getBean(DapEngineJdbcTemplate.class);
            MetadataConfigService metaSvc = ctx.getBean(MetadataConfigService.class);
            SchemaStatusService schemaSvc = ctx.getBean(SchemaStatusService.class);
            DapEngineSchemaInitializer initializer =
                new DapEngineSchemaInitializer(jdbcTpl, metaSvc, schemaSvc);

            // 建表并包含 remark 字段
            metaSvc.saveSubjectConfig("ORDER_TYPE",
                buildRequest("ORDER_TYPE", "订单类型", false,
                    Collections.singletonList(buildField("remark", "STRING", "备注"))), "admin");
            initializer.applySchema("ORDER_TYPE");

            // 废弃 remark（从请求中移除）
            metaSvc.saveSubjectConfig("ORDER_TYPE",
                buildRequest("ORDER_TYPE", "订单类型", false,
                    Collections.<FieldConfigRequest>emptyList()), "admin");
            SchemaChangeResult result = initializer.applySchema("ORDER_TYPE");

            // 不生成 DROP DDL
            assertThat(result.getExecutedDdl()).isEmpty();
            // 列依然存在于物理表
            Map<String, String> typeMap = schemaSvc.getColumnTypeMap("dap_order_type");
            assertThat(typeMap).containsKey("remark");
        });
    }

    @Test
    public void applySchema_skips_existing_columns_when_type_map_is_incomplete() {
        contextRunner.withPropertyValues(
            "dap.engine.tenant-id=t1", "dap.engine.app-code=app1",
            "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "schema_us2_incomplete_typemap_test"),
            "rj.unify.engine.datasource.username=sa", "rj.unify.engine.datasource.password=",
            "dap.engine.security.encrypt-key=" + VALID_KEY
        ).run(ctx -> {
            DapEngineJdbcTemplate jdbcTpl = ctx.getBean(DapEngineJdbcTemplate.class);
            MetadataConfigService metaSvc = ctx.getBean(MetadataConfigService.class);
            SchemaStatusService schemaSvc = ctx.getBean(SchemaStatusService.class);
            DapEngineSchemaInitializer initializer =
                new DapEngineSchemaInitializer(jdbcTpl, metaSvc, schemaSvc);

            metaSvc.saveSubjectConfig("CUSTOMER",
                buildRequest("CUSTOMER", "客户", false,
                    Collections.singletonList(buildField("credit_code", "STRING", "信用代码"))),
                "admin");
            initializer.applySchema("CUSTOMER");

            SchemaStatusService incompleteTypeMapSvc = Mockito.spy(schemaSvc);
            doReturn(Collections.<String, String>emptyMap())
                .when(incompleteTypeMapSvc).getColumnTypeMap("dap_customer");

            DapEngineSchemaInitializer guardedInitializer =
                new DapEngineSchemaInitializer(jdbcTpl, metaSvc, incompleteTypeMapSvc);

            SchemaChangeResult result = guardedInitializer.applySchema("CUSTOMER");

            assertThat(result.getExecutedDdl()).isEmpty();
            assertThat(schemaSvc.getColumnTypeMap("dap_customer"))
                .containsKeys("code", "name", "credit_code");
        });
    }

    // =========================================================================
    // US3: 类型扩容
    // =========================================================================

    @Test
    public void applySchema_widens_string_to_string_long() {
        contextRunner.withPropertyValues(
            "dap.engine.tenant-id=t1", "dap.engine.app-code=app1",
            "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "schema_us3_widen_str_test"),
            "rj.unify.engine.datasource.username=sa", "rj.unify.engine.datasource.password=",
            "dap.engine.security.encrypt-key=" + VALID_KEY
        ).run(ctx -> {
            DapEngineJdbcTemplate jdbcTpl = ctx.getBean(DapEngineJdbcTemplate.class);
            MetadataConfigService metaSvc = ctx.getBean(MetadataConfigService.class);
            SchemaStatusService schemaSvc = ctx.getBean(SchemaStatusService.class);
            DapEngineSchemaInitializer initializer =
                new DapEngineSchemaInitializer(jdbcTpl, metaSvc, schemaSvc);

            // 建表：remark 为 STRING
            metaSvc.saveSubjectConfig("CATALOG",
                buildRequest("CATALOG", "目录", false,
                    Collections.singletonList(buildField("remark", "STRING", "备注"))), "admin");
            initializer.applySchema("CATALOG");

            // 类型扩容 STRING -> STRING_LONG
            metaSvc.saveSubjectConfig("CATALOG",
                buildRequest("CATALOG", "目录", false,
                    Collections.singletonList(buildField("remark", "STRING_LONG", "备注"))), "admin");
            SchemaChangeResult result = initializer.applySchema("CATALOG");

            assertThat(result.getExecutedDdl()).hasSize(1);
            assertThat(result.getExecutedDdl().get(0)).contains("MODIFY COLUMN remark");

            // 验证列类型已更新
            Map<String, String> typeMap = schemaSvc.getColumnTypeMap("dap_catalog");
            assertThat(typeMap.get("remark")).isEqualTo("STRING_LONG");
        });
    }

    @Test
    public void applySchema_widens_same_string_type_by_max_length() {
        contextRunner.withPropertyValues(
            "dap.engine.tenant-id=t1", "dap.engine.app-code=app1",
            "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "schema_us3_widen_length_test"),
            "rj.unify.engine.datasource.username=sa", "rj.unify.engine.datasource.password=",
            "dap.engine.security.encrypt-key=" + VALID_KEY
        ).run(ctx -> {
            DapEngineJdbcTemplate jdbcTpl = ctx.getBean(DapEngineJdbcTemplate.class);
            MetadataConfigService metaSvc = ctx.getBean(MetadataConfigService.class);
            SchemaStatusService schemaSvc = ctx.getBean(SchemaStatusService.class);
            DapEngineSchemaInitializer initializer =
                new DapEngineSchemaInitializer(jdbcTpl, metaSvc, schemaSvc);

            metaSvc.saveSubjectConfig("CATALOG",
                buildRequest("CATALOG", "目录", false,
                    Collections.singletonList(buildField("remark", "STRING", 128, "备注"))), "admin");
            initializer.applySchema("CATALOG");

            metaSvc.saveSubjectConfig("CATALOG",
                buildRequest("CATALOG", "目录", false,
                    Collections.singletonList(buildField("remark", "STRING", 500, "备注"))), "admin");
            SchemaChangeResult result = initializer.applySchema("CATALOG");

            assertThat(result.getExecutedDdl()).hasSize(1);
            assertThat(result.getExecutedDdl().get(0)).contains("MODIFY COLUMN remark VARCHAR(500)");
            assertThat(schemaSvc.getColumnDefinitionMap("dap_catalog").get("remark").getMaxLength()).isEqualTo(500L);
        });
    }

    @Test
    public void applySchema_modifies_column_when_token_indicates_string_but_raw_type_is_unexpected() {
        contextRunner.withPropertyValues(
            "dap.engine.tenant-id=t1", "dap.engine.app-code=app1",
            "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "schema_token_fallback_test"),
            "rj.unify.engine.datasource.username=sa", "rj.unify.engine.datasource.password=",
            "dap.engine.security.encrypt-key=" + VALID_KEY
        ).run(ctx -> {
            DapEngineJdbcTemplate jdbcTpl = ctx.getBean(DapEngineJdbcTemplate.class);
            MetadataConfigService metaSvc = ctx.getBean(MetadataConfigService.class);
            SchemaStatusService realSchemaSvc = ctx.getBean(SchemaStatusService.class);

            metaSvc.saveSubjectConfig("CUSTOMER",
                buildRequest("CUSTOMER", "客户", false,
                    Collections.singletonList(buildField("short_name", "STRING", 500, "简称"))), "admin");

            jdbcTpl.getJdbcTemplate().execute(
                "CREATE TABLE dap_customer (" +
                    "id BIGINT PRIMARY KEY AUTO_INCREMENT, " +
                    "tenant_id VARCHAR(64) NOT NULL DEFAULT '', " +
                    "app_code VARCHAR(64) NOT NULL DEFAULT '', " +
                    "code VARCHAR(128) NOT NULL, " +
                    "name VARCHAR(128) NOT NULL DEFAULT '', " +
                    "parent_code VARCHAR(128) NOT NULL DEFAULT '', " +
                    "dap_version BIGINT NOT NULL DEFAULT 0, " +
                    "dap_sync_time DATETIME, " +
                    "is_delete TINYINT NOT NULL DEFAULT 0, " +
                    "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "created_by VARCHAR(64) NOT NULL DEFAULT '', " +
                    "updated_by VARCHAR(64) NOT NULL DEFAULT '', " +
                    "short_name VARCHAR(128)" +
                    ")");

            SchemaStatusService schemaSvc = Mockito.spy(realSchemaSvc);
            java.util.Map<String, SchemaStatusService.ColumnDefinition> definitions = new java.util.LinkedHashMap<>(realSchemaSvc.getColumnDefinitionMap("dap_customer"));
            definitions.put("short_name", new SchemaStatusService.ColumnDefinition("CHARACTER VARYING", 128L));
            doReturn(definitions).doCallRealMethod().when(schemaSvc).getColumnDefinitionMap("dap_customer");

            DapEngineSchemaInitializer initializer = new DapEngineSchemaInitializer(jdbcTpl, metaSvc, schemaSvc);
            SchemaChangeResult result = initializer.applySchema("CUSTOMER");

            assertThat(result.getExecutedDdl()).hasSize(1);
            assertThat(result.getExecutedDdl().get(0)).contains("MODIFY COLUMN short_name VARCHAR(500)");
            assertThat(realSchemaSvc.getColumnDefinitionMap("dap_customer").get("short_name").getMaxLength()).isEqualTo(500L);
        });
    }

    @Test
    public void applySchema_widens_int_to_decimal() {
        contextRunner.withPropertyValues(
            "dap.engine.tenant-id=t1", "dap.engine.app-code=app1",
            "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "schema_us3_widen_int_test"),
            "rj.unify.engine.datasource.username=sa", "rj.unify.engine.datasource.password=",
            "dap.engine.security.encrypt-key=" + VALID_KEY
        ).run(ctx -> {
            DapEngineJdbcTemplate jdbcTpl = ctx.getBean(DapEngineJdbcTemplate.class);
            MetadataConfigService metaSvc = ctx.getBean(MetadataConfigService.class);
            SchemaStatusService schemaSvc = ctx.getBean(SchemaStatusService.class);
            DapEngineSchemaInitializer initializer =
                new DapEngineSchemaInitializer(jdbcTpl, metaSvc, schemaSvc);

            metaSvc.saveSubjectConfig("PRICE_ITEM",
                buildRequest("PRICE_ITEM", "价格条目", false,
                    Collections.singletonList(buildField("amount", "INT", "金额"))), "admin");
            initializer.applySchema("PRICE_ITEM");

            // 类型扩容 INT -> DECIMAL
            metaSvc.saveSubjectConfig("PRICE_ITEM",
                buildRequest("PRICE_ITEM", "价格条目", false,
                    Collections.singletonList(buildField("amount", "DECIMAL", "金额"))), "admin");
            SchemaChangeResult result = initializer.applySchema("PRICE_ITEM");

            assertThat(result.getExecutedDdl()).hasSize(1);
            assertThat(result.getExecutedDdl().get(0)).contains("MODIFY COLUMN amount");

            Map<String, String> typeMap = schemaSvc.getColumnTypeMap("dap_price_item");
            assertThat(typeMap.get("amount")).isEqualTo("DECIMAL");
        });
    }

    @Test
    public void applySchema_throws_when_schema_still_pending_after_execution() {
        contextRunner.withPropertyValues(
            "dap.engine.tenant-id=t1", "dap.engine.app-code=app1",
            "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "schema_pending_after_apply_test"),
            "rj.unify.engine.datasource.username=sa", "rj.unify.engine.datasource.password=",
            "dap.engine.security.encrypt-key=" + VALID_KEY
        ).run(ctx -> {
            DapEngineJdbcTemplate jdbcTpl = ctx.getBean(DapEngineJdbcTemplate.class);
            MetadataConfigService metaSvc = ctx.getBean(MetadataConfigService.class);
            SchemaStatusService realSchemaSvc = ctx.getBean(SchemaStatusService.class);
            SchemaStatusService schemaSvc = Mockito.spy(realSchemaSvc);
            doReturn(SchemaStatus.PENDING).when(schemaSvc).computeStatus(eq("CUSTOMER"), anyList());

            DapEngineSchemaInitializer initializer = new DapEngineSchemaInitializer(jdbcTpl, metaSvc, schemaSvc);
            metaSvc.saveSubjectConfig("CUSTOMER",
                buildRequest("CUSTOMER", "客户", false, Collections.singletonList(buildField("short_name", "STRING", 50, "简称"))),
                "admin");

            assertThatThrownBy(() -> initializer.applySchema("CUSTOMER"))
                .isInstanceOf(DapValidationException.class)
                .hasMessageContaining("仍存在未应用 Schema 变更");
        });
    }

    @Test
    public void applySchema_no_ddl_when_type_unchanged() {
        contextRunner.withPropertyValues(
            "dap.engine.tenant-id=t1", "dap.engine.app-code=app1",
            "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "schema_us3_notype_test"),
            "rj.unify.engine.datasource.username=sa", "rj.unify.engine.datasource.password=",
            "dap.engine.security.encrypt-key=" + VALID_KEY
        ).run(ctx -> {
            DapEngineJdbcTemplate jdbcTpl = ctx.getBean(DapEngineJdbcTemplate.class);
            MetadataConfigService metaSvc = ctx.getBean(MetadataConfigService.class);
            SchemaStatusService schemaSvc = ctx.getBean(SchemaStatusService.class);
            DapEngineSchemaInitializer initializer =
                new DapEngineSchemaInitializer(jdbcTpl, metaSvc, schemaSvc);

            metaSvc.saveSubjectConfig("WAREHOUSE",
                buildRequest("WAREHOUSE", "仓库", false,
                    Collections.singletonList(buildField("region", "STRING", "区域"))), "admin");
            initializer.applySchema("WAREHOUSE");

            // 相同类型再次 apply
            SchemaChangeResult result = initializer.applySchema("WAREHOUSE");
            assertThat(result.getExecutedDdl()).isEmpty();
        });
    }

    // =========================================================================
    // US4: 启动兜底 ApplicationRunner.run()
    // =========================================================================

    @Test
    public void run_creates_tables_on_startup() {
        contextRunner.withPropertyValues(
            "dap.engine.tenant-id=t1", "dap.engine.app-code=app1",
            "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "schema_us4_startup_test"),
            "rj.unify.engine.datasource.username=sa", "rj.unify.engine.datasource.password=",
            "dap.engine.security.encrypt-key=" + VALID_KEY
        ).run(ctx -> {
            DapEngineJdbcTemplate jdbcTpl = ctx.getBean(DapEngineJdbcTemplate.class);
            MetadataConfigService metaSvc = ctx.getBean(MetadataConfigService.class);
            SchemaStatusService schemaSvc = ctx.getBean(SchemaStatusService.class);
            DapEngineSchemaInitializer initializer =
                new DapEngineSchemaInitializer(jdbcTpl, metaSvc, schemaSvc);

            // 保存元数据但不建表
            metaSvc.saveSubjectConfig("STARTUP_SUBJ",
                buildRequest("STARTUP_SUBJ", "启动兜底测试", false,
                    Collections.<FieldConfigRequest>emptyList()), "admin");

            assertThat(schemaSvc.tableExists("dap_startup_subj")).isFalse();

            // 调用 run()
            initializer.run(null);

            assertThat(schemaSvc.tableExists("dap_startup_subj")).isTrue();
        });
    }

    @Test
    public void run_continues_after_single_subject_failure() {
        contextRunner.withPropertyValues(
            "dap.engine.tenant-id=t1", "dap.engine.app-code=app1",
            "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "schema_us4_failcont_test"),
            "rj.unify.engine.datasource.username=sa", "rj.unify.engine.datasource.password=",
            "dap.engine.security.encrypt-key=" + VALID_KEY
        ).run(ctx -> {
            DapEngineJdbcTemplate jdbcTpl = ctx.getBean(DapEngineJdbcTemplate.class);
            MetadataConfigService metaSvc = ctx.getBean(MetadataConfigService.class);
            SchemaStatusService schemaSvc = ctx.getBean(SchemaStatusService.class);

            // Mock metaSvc: listSubjectCodes 返回两个，applySchema 对 FAIL_SUBJ 抛异常
            MetadataConfigService mockMeta = Mockito.spy(metaSvc);
            doReturn(Arrays.asList("FAIL_SUBJ", "OK_SUBJ")).when(mockMeta).listSubjectCodes();

            // 保存 OK_SUBJ 元数据
            metaSvc.saveSubjectConfig("OK_SUBJ",
                buildRequest("OK_SUBJ", "正常主数据", false, Collections.<FieldConfigRequest>emptyList()),
                "admin");

            DapEngineSchemaInitializer initializer =
                new DapEngineSchemaInitializer(jdbcTpl, mockMeta, schemaSvc);

            // run 不应抛出异常（FAIL_SUBJ 失败被捕获，OK_SUBJ 正常处理）
            assertThatCode(() -> initializer.run(null)).doesNotThrowAnyException();

            // OK_SUBJ 表应已创建
            assertThat(schemaSvc.tableExists("dap_ok_subj")).isTrue();
        });
    }

    // =========================================================================
    // US5: SyncScheduler 可选集成
    // =========================================================================

    @Test
    public void applySchema_triggers_reschedule_when_scheduler_present() {
        contextRunner.withPropertyValues(
            "dap.engine.tenant-id=t1", "dap.engine.app-code=app1",
            "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "schema_us5_sched_test"),
            "rj.unify.engine.datasource.username=sa", "rj.unify.engine.datasource.password=",
            "dap.engine.security.encrypt-key=" + VALID_KEY
        ).run(ctx -> {
            DapEngineJdbcTemplate jdbcTpl = ctx.getBean(DapEngineJdbcTemplate.class);
            MetadataConfigService metaSvc = ctx.getBean(MetadataConfigService.class);
            SchemaStatusService schemaSvc = ctx.getBean(SchemaStatusService.class);

            SyncScheduler mockScheduler = Mockito.mock(SyncScheduler.class);
            DapEngineSchemaInitializer initializer =
                new DapEngineSchemaInitializer(jdbcTpl, metaSvc, schemaSvc);
            // 注入 mock scheduler（模拟 @Autowired(required=false)，Phase 4 已实现该场景）
            initializer.syncScheduler = mockScheduler;

            metaSvc.saveSubjectConfig("SCHED_SUBJ",
                buildRequest("SCHED_SUBJ", "调度测试", false, Collections.<FieldConfigRequest>emptyList()),
                "admin");

            initializer.applySchema("SCHED_SUBJ");

            Mockito.verify(mockScheduler, times(1)).reschedule("SCHED_SUBJ");
        });
    }

    @Test
    public void applySchema_skips_reschedule_when_scheduler_absent() {
        contextRunner.withPropertyValues(
            "dap.engine.tenant-id=t1", "dap.engine.app-code=app1",
            "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "schema_us5_nosched_test"),
            "rj.unify.engine.datasource.username=sa", "rj.unify.engine.datasource.password=",
            "dap.engine.security.encrypt-key=" + VALID_KEY
        ).run(ctx -> {
            DapEngineJdbcTemplate jdbcTpl = ctx.getBean(DapEngineJdbcTemplate.class);
            MetadataConfigService metaSvc = ctx.getBean(MetadataConfigService.class);
            SchemaStatusService schemaSvc = ctx.getBean(SchemaStatusService.class);

            // 不注入 SyncScheduler（syncScheduler = null，Phase 4 已实现场景）
            DapEngineSchemaInitializer initializer =
                new DapEngineSchemaInitializer(jdbcTpl, metaSvc, schemaSvc);

            metaSvc.saveSubjectConfig("NOSCHED_SUBJ",
                buildRequest("NOSCHED_SUBJ", "无调度测试", false, Collections.<FieldConfigRequest>emptyList()),
                "admin");

            // 不注入 scheduler，applySchema 正常完成且无异常
            assertThatCode(() -> initializer.applySchema("NOSCHED_SUBJ")).doesNotThrowAnyException();
        });
    }

    // =========================================================================
    // toDbType / isWideningRequired 工具方法测试
    // =========================================================================

    @Test
    public void toDbType_returns_correct_sql_types() {
        // 构造一个无依赖的 initializer 实例用于工具方法测试
        contextRunner.withPropertyValues(
            "dap.engine.tenant-id=t1", "dap.engine.app-code=app1",
            "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "schema_dbtype_test"),
            "rj.unify.engine.datasource.username=sa", "rj.unify.engine.datasource.password=",
            "dap.engine.security.encrypt-key=" + VALID_KEY
        ).run(ctx -> {
            DapEngineJdbcTemplate jdbcTpl = ctx.getBean(DapEngineJdbcTemplate.class);
            MetadataConfigService metaSvc = ctx.getBean(MetadataConfigService.class);
            SchemaStatusService schemaSvc = ctx.getBean(SchemaStatusService.class);
            DapEngineSchemaInitializer init =
                new DapEngineSchemaInitializer(jdbcTpl, metaSvc, schemaSvc);

            assertThat(init.toDbType("STRING")).isEqualTo("VARCHAR(128)");
            assertThat(init.toDbType("STRING_LONG")).isEqualTo("VARCHAR(1024)");
            assertThat(init.toDbType("TEXT")).isEqualTo("TEXT");
            assertThat(init.toDbType("INT")).isEqualTo("BIGINT");
            assertThat(init.toDbType("DECIMAL")).isEqualTo("DECIMAL(18,4)");
            assertThat(init.toDbType("DATE")).isEqualTo("DATE");
            assertThat(init.toDbType("DATETIME")).isEqualTo("DATETIME");
            assertThat(init.toDbType("ENUM")).isEqualTo("VARCHAR(64)");
        });
    }

    @Test
    public void isWideningRequired_returns_true_for_valid_paths() {
        contextRunner.withPropertyValues(
            "dap.engine.tenant-id=t1", "dap.engine.app-code=app1",
            "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "schema_widening_test"),
            "rj.unify.engine.datasource.username=sa", "rj.unify.engine.datasource.password=",
            "dap.engine.security.encrypt-key=" + VALID_KEY
        ).run(ctx -> {
            DapEngineJdbcTemplate jdbcTpl = ctx.getBean(DapEngineJdbcTemplate.class);
            MetadataConfigService metaSvc = ctx.getBean(MetadataConfigService.class);
            SchemaStatusService schemaSvc = ctx.getBean(SchemaStatusService.class);
            DapEngineSchemaInitializer init =
                new DapEngineSchemaInitializer(jdbcTpl, metaSvc, schemaSvc);

            assertThat(init.isWideningRequired("STRING", "STRING_LONG")).isTrue();
            assertThat(init.isWideningRequired("STRING", "TEXT")).isTrue();
            assertThat(init.isWideningRequired("STRING_LONG", "TEXT")).isTrue();
            assertThat(init.isWideningRequired("INT", "DECIMAL")).isTrue();
            assertThat(init.isWideningRequired("DATE", "DATETIME")).isTrue();

            assertThat(init.isWideningRequired("STRING", "INT")).isFalse();
            assertThat(init.isWideningRequired("DECIMAL", "INT")).isFalse();
            assertThat(init.isWideningRequired("TEXT", "STRING")).isFalse();
        });
    }
}
