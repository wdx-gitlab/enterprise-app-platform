package com.ruijie.dapengine.admin.service;

import com.ruijie.dapengine.autoconfigure.DapEngineAutoConfiguration;
import com.ruijie.dapengine.autoconfigure.DapEngineJdbcTemplate;
import com.ruijie.dapengine.common.enums.SchemaStatus;
import com.ruijie.dapengine.common.model.FieldConfigDTO;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * SchemaStatusService 单元测试（H2 内存数据库）。
 * 覆盖：动态表不存在→PENDING；表存在但缺列→PENDING；所有有效字段列均存在→APPLIED。
 */
@RunWith(SpringRunner.class)
public class SchemaStatusServiceTest {

    private static final String H2_URL_TPL =
        "jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1;MODE=MySQL";
    private static final String VALID_KEY = "TestEncryptKey32CharactersLong!!";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(JacksonAutoConfiguration.class, DapEngineAutoConfiguration.class);

    private FieldConfigDTO fieldOf(String name) {
        FieldConfigDTO f = new FieldConfigDTO();
        f.setFieldName(name);
        f.setFieldType("STRING");
        f.setMaxLength(128);
        return f;
    }

    @Test
    public void should_return_PENDING_when_dynamic_table_not_exists() {
        contextRunner.withPropertyValues(
            "dap.engine.tenant-id=t1", "dap.engine.app-code=app1",
            "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "schema_notexist"),
            "rj.unify.engine.datasource.username=sa", "rj.unify.engine.datasource.password=",
            "dap.engine.security.encrypt-key=" + VALID_KEY
        ).run(ctx -> {
            DapEngineJdbcTemplate jdbcTpl = ctx.getBean(DapEngineJdbcTemplate.class);
            SchemaStatusService svc = new SchemaStatusService(jdbcTpl.getJdbcTemplate());

            // dap_customer 表不存在
            List<FieldConfigDTO> fields = Arrays.asList(fieldOf("code"), fieldOf("name"));
            SchemaStatus status = svc.computeStatus("CUSTOMER", fields);
            assertThat(status).isEqualTo(SchemaStatus.PENDING);
        });
    }

    @Test
    public void should_return_PENDING_when_table_exists_but_column_missing() {
        contextRunner.withPropertyValues(
            "dap.engine.tenant-id=t1", "dap.engine.app-code=app1",
            "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "schema_missing_col"),
            "rj.unify.engine.datasource.username=sa", "rj.unify.engine.datasource.password=",
            "dap.engine.security.encrypt-key=" + VALID_KEY
        ).run(ctx -> {
            DapEngineJdbcTemplate jdbcTpl = ctx.getBean(DapEngineJdbcTemplate.class);
            SchemaStatusService svc = new SchemaStatusService(jdbcTpl.getJdbcTemplate());

            // 手动建动态表，只有 code 列，没有 credit_code 列
            jdbcTpl.getJdbcTemplate().execute(
                "CREATE TABLE dap_supplier (id BIGINT PRIMARY KEY AUTO_INCREMENT, code VARCHAR(128))");

            List<FieldConfigDTO> fields = Arrays.asList(fieldOf("code"), fieldOf("credit_code"));
            SchemaStatus status = svc.computeStatus("SUPPLIER", fields);
            assertThat(status).isEqualTo(SchemaStatus.PENDING);
        });
    }

    @Test
    public void should_return_APPLIED_when_all_active_fields_have_columns() {
        contextRunner.withPropertyValues(
            "dap.engine.tenant-id=t1", "dap.engine.app-code=app1",
            "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "schema_applied"),
            "rj.unify.engine.datasource.username=sa", "rj.unify.engine.datasource.password=",
            "dap.engine.security.encrypt-key=" + VALID_KEY
        ).run(ctx -> {
            DapEngineJdbcTemplate jdbcTpl = ctx.getBean(DapEngineJdbcTemplate.class);
            SchemaStatusService svc = new SchemaStatusService(jdbcTpl.getJdbcTemplate());

            // 手动建完整动态表
            jdbcTpl.getJdbcTemplate().execute(
                "CREATE TABLE dap_product (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, " +
                "code VARCHAR(128) NOT NULL, " +
                "name VARCHAR(128) NOT NULL, " +
                "credit_code VARCHAR(128)" +
                ")");

            List<FieldConfigDTO> fields = Arrays.asList(
                fieldOf("code"), fieldOf("name"), fieldOf("credit_code"));
            SchemaStatus status = svc.computeStatus("PRODUCT", fields);
            assertThat(status).isEqualTo(SchemaStatus.APPLIED);
        });
    }

    @Test
    public void should_return_PENDING_when_varchar_length_not_match() {
        contextRunner.withPropertyValues(
            "dap.engine.tenant-id=t1", "dap.engine.app-code=app1",
            "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "schema_length_mismatch"),
            "rj.unify.engine.datasource.username=sa", "rj.unify.engine.datasource.password=",
            "dap.engine.security.encrypt-key=" + VALID_KEY
        ).run(ctx -> {
            DapEngineJdbcTemplate jdbcTpl = ctx.getBean(DapEngineJdbcTemplate.class);
            SchemaStatusService svc = new SchemaStatusService(jdbcTpl.getJdbcTemplate());

            jdbcTpl.getJdbcTemplate().execute(
                "CREATE TABLE dap_customer_length (" +
                    "id BIGINT PRIMARY KEY AUTO_INCREMENT, " +
                    "code VARCHAR(128) NOT NULL, " +
                    "name VARCHAR(128) NOT NULL, " +
                    "short_name VARCHAR(128)" +
                    ")");

            FieldConfigDTO shortName = fieldOf("short_name");
            shortName.setMaxLength(50);
            List<FieldConfigDTO> fields = Arrays.asList(fieldOf("code"), fieldOf("name"), shortName);

            SchemaStatus status = svc.computeStatus("CUSTOMER_LENGTH", fields);
            assertThat(status).isEqualTo(SchemaStatus.PENDING);
        });
    }

    @Test
    public void should_return_APPLIED_when_no_active_fields_and_table_exists() {
        contextRunner.withPropertyValues(
            "dap.engine.tenant-id=t1", "dap.engine.app-code=app1",
            "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "schema_empty_fields"),
            "rj.unify.engine.datasource.username=sa", "rj.unify.engine.datasource.password=",
            "dap.engine.security.encrypt-key=" + VALID_KEY
        ).run(ctx -> {
            DapEngineJdbcTemplate jdbcTpl = ctx.getBean(DapEngineJdbcTemplate.class);
            SchemaStatusService svc = new SchemaStatusService(jdbcTpl.getJdbcTemplate());

            jdbcTpl.getJdbcTemplate().execute(
                "CREATE TABLE dap_empty (id BIGINT PRIMARY KEY AUTO_INCREMENT)");

            // 无有效字段时，表存在即为 APPLIED
            SchemaStatus status = svc.computeStatus("EMPTY", Collections.<FieldConfigDTO>emptyList());
            assertThat(status).isEqualTo(SchemaStatus.APPLIED);
        });
    }

    // -------------------------------------------------------------------------
    // T008: tableExists + getColumnTypeMap 新方法测试
    // -------------------------------------------------------------------------

    @Test
    public void tableExists_returns_true_when_table_exists() {
        contextRunner.withPropertyValues(
            "dap.engine.tenant-id=t1", "dap.engine.app-code=app1",
            "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "schema_status_ext_test_exists"),
            "rj.unify.engine.datasource.username=sa", "rj.unify.engine.datasource.password=",
            "dap.engine.security.encrypt-key=" + VALID_KEY
        ).run(ctx -> {
            DapEngineJdbcTemplate jdbcTpl = ctx.getBean(DapEngineJdbcTemplate.class);
            SchemaStatusService svc = new SchemaStatusService(jdbcTpl.getJdbcTemplate());

            jdbcTpl.getJdbcTemplate().execute(
                "CREATE TABLE dap_exists_test (id BIGINT PRIMARY KEY AUTO_INCREMENT, code VARCHAR(128))");

            assertThat(svc.tableExists("dap_exists_test")).isTrue();
        });
    }

    @Test
    public void tableExists_returns_false_for_missing_table() {
        contextRunner.withPropertyValues(
            "dap.engine.tenant-id=t1", "dap.engine.app-code=app1",
            "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "schema_status_ext_test_missing"),
            "rj.unify.engine.datasource.username=sa", "rj.unify.engine.datasource.password=",
            "dap.engine.security.encrypt-key=" + VALID_KEY
        ).run(ctx -> {
            DapEngineJdbcTemplate jdbcTpl = ctx.getBean(DapEngineJdbcTemplate.class);
            SchemaStatusService svc = new SchemaStatusService(jdbcTpl.getJdbcTemplate());

            assertThat(svc.tableExists("dap_nonexistent_xyz")).isFalse();
        });
    }

    @Test
    public void getColumnTypeMap_returns_correct_token_mapping() {
        contextRunner.withPropertyValues(
            "dap.engine.tenant-id=t1", "dap.engine.app-code=app1",
            "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "schema_status_ext_test_typemap"),
            "rj.unify.engine.datasource.username=sa", "rj.unify.engine.datasource.password=",
            "dap.engine.security.encrypt-key=" + VALID_KEY
        ).run(ctx -> {
            DapEngineJdbcTemplate jdbcTpl = ctx.getBean(DapEngineJdbcTemplate.class);
            SchemaStatusService svc = new SchemaStatusService(jdbcTpl.getJdbcTemplate());

            jdbcTpl.getJdbcTemplate().execute(
                "CREATE TABLE dap_typemap_test (" +
                "id BIGINT, " +
                "short_name VARCHAR(128), " +
                "long_name VARCHAR(1024), " +
                "enum_code VARCHAR(64), " +
                "note TEXT, " +
                "cnt BIGINT, " +
                "price DECIMAL(18,4), " +
                "birth DATE, " +
                "created_at DATETIME" +
                ")");

            java.util.Map<String, String> typeMap = svc.getColumnTypeMap("dap_typemap_test");
            assertThat(typeMap).isNotNull();
            assertThat(typeMap.get("id")).isEqualTo("INT");
            assertThat(typeMap.get("short_name")).isEqualTo("STRING");
            assertThat(typeMap.get("long_name")).isEqualTo("STRING_LONG");
            assertThat(typeMap.get("enum_code")).isEqualTo("ENUM");
            assertThat(typeMap.get("note")).isEqualTo("TEXT");
            assertThat(typeMap.get("cnt")).isEqualTo("INT");
            assertThat(typeMap.get("price")).isEqualTo("DECIMAL");
            assertThat(typeMap.get("birth")).isEqualTo("DATE");
            // H2: DATETIME 对应 TIMESTAMP token，MySQL: DATETIME
            assertThat(typeMap.get("created_at")).isIn("DATETIME", "TIMESTAMP");
        });
    }
}

