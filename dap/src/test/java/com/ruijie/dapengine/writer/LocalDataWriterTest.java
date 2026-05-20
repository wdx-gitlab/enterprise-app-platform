package com.ruijie.dapengine.writer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruijie.dapengine.autoconfigure.DapEngineJdbcTemplate;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * LocalDataWriter 测试。
 * - upsert 场景：H2 内存库集成验证。
 * - fullRefresh 场景：Mockito mock JdbcTemplate，验证 SQL 调用（CREATE TABLE LIKE / RENAME TABLE 为 MySQL 专有语法）。
 */
@RunWith(JUnit4.class)
public class LocalDataWriterTest {

    private static final String TENANT_ID = "test-tenant";
    private static final String APP_CODE = "test-app";
    private static final String SUBJECT_CODE = "test_subject";
    private static final String TABLE_NAME = "dap_" + SUBJECT_CODE;

    private LocalDataWriter writer;
    private JdbcTemplate jdbc;

    @Before
    public void setUp() {
        SimpleDriverDataSource ds = new SimpleDriverDataSource();
        ds.setDriverClass(org.h2.Driver.class);
        ds.setUrl("jdbc:h2:mem:writer_test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL");
        ds.setUsername("sa");
        ds.setPassword("");

        jdbc = new JdbcTemplate(ds);

        // 创建测试动态表（模拟 apply-schema 后的状态）
        jdbc.execute("DROP TABLE IF EXISTS `" + TABLE_NAME + "`");
        jdbc.execute("CREATE TABLE `" + TABLE_NAME + "` (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, " +
                "tenant_id VARCHAR(64) NOT NULL DEFAULT '', " +
                "app_code VARCHAR(64) NOT NULL DEFAULT '', " +
                "code VARCHAR(128) NOT NULL, " +
                "name VARCHAR(255), " +
                "tags TEXT, " +
                "dap_version BIGINT NOT NULL DEFAULT 0, " +
                "dap_sync_time DATETIME, " +
                "is_delete TINYINT NOT NULL DEFAULT 0, " +
                "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "created_by VARCHAR(64) NOT NULL DEFAULT '', " +
                "updated_by VARCHAR(64) NOT NULL DEFAULT '', " +
                "CONSTRAINT uk_code UNIQUE (tenant_id, app_code, code))");

        DapEngineJdbcTemplate dapJdbc = new DapEngineJdbcTemplate(ds);
        writer = new LocalDataWriter(dapJdbc, TENANT_ID, APP_CODE, new ObjectMapper());
    }

    @After
    public void tearDown() {
        jdbc.execute("DROP TABLE IF EXISTS `" + TABLE_NAME + "`");
        jdbc.execute("DROP TABLE IF EXISTS `" + TABLE_NAME + "_tmp`");
    }

    private Map<String, Object> makeRecord(String code, String name) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("code", code);
        row.put("name", name);
        return row;
    }

    @Test
    public void should_upsert_insert_new_records() {
        List<Map<String, Object>> records = new ArrayList<>();
        records.add(makeRecord("C001", "Alice"));
        records.add(makeRecord("C002", "Bob"));

        int count = writer.upsert(SUBJECT_CODE, records, "operator1");
        assertThat(count).isGreaterThan(0);

        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT code, name, created_by FROM `" + TABLE_NAME + "` ORDER BY code");
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).get("CODE")).isEqualTo("C001");
        assertThat(rows.get(1).get("CODE")).isEqualTo("C002");
        assertThat(rows.get(0).get("CREATED_BY")).isEqualTo("operator1");
    }

    @Test
    public void should_upsert_update_but_preserve_created_by() {
        // 首次插入
        List<Map<String, Object>> initial = new ArrayList<>();
        initial.add(makeRecord("C001", "Alice"));
        writer.upsert(SUBJECT_CODE, initial, "original-creator");

        // 第二次更新（不同 operator）
        List<Map<String, Object>> updated = new ArrayList<>();
        updated.add(makeRecord("C001", "Alice Updated"));
        writer.upsert(SUBJECT_CODE, updated, "new-operator");

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT code, name, created_by, updated_by FROM `" + TABLE_NAME + "` WHERE code='C001'");
        // name 已更新
        assertThat(row.get("NAME")).isEqualTo("Alice Updated");
        // created_by 保留初始值（ON DUPLICATE KEY UPDATE 排除了 created_by）
        assertThat(row.get("CREATED_BY")).isEqualTo("original-creator");
        // updated_by 变为新 operator
        assertThat(row.get("UPDATED_BY")).isEqualTo("new-operator");
    }

    @Test
    public void should_return_0_for_empty_records() {
        int count = writer.upsert(SUBJECT_CODE, new ArrayList<>(), "op");
        assertThat(count).isEqualTo(0);
    }

    @Test
    public void should_reject_invalid_subject_code() {
        assertThatThrownBy(() -> writer.upsert("INVALID-CODE!!!", new ArrayList<>(), "op"))
                .isInstanceOf(com.ruijie.dapengine.common.exception.DapValidationException.class);
    }

    @Test
    public void should_serialize_list_and_map_field_values_to_json_string() {
        // 模拟 HTTP API 返回 JSON 数组字段（如 departmentHrbpList: []）
        // Jackson convertValue 会将 JSON 数组转成 java.util.List，写入时须序列化为 JSON 字符串
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("code", "ORG001");
        row.put("name", "组织架构");
        row.put("tags", Arrays.asList("A", "B", "C"));  // List 类型

        // 不应抛出 SQLException（Java 序列化导致 Incorrect string value）
        int count = writer.upsert(SUBJECT_CODE, java.util.Collections.singletonList(row), "op");
        assertThat(count).isGreaterThan(0);

        Map<String, Object> saved = jdbc.queryForMap(
                "SELECT tags FROM `" + TABLE_NAME + "` WHERE code='ORG001'");
        // 写入后应为 JSON 字符串，而非乱码
        String tagsValue = (String) saved.get("TAGS");
        assertThat(tagsValue).isEqualTo("[\"A\",\"B\",\"C\"]");
    }

    // -------------------------------------------------------------------------
    // T053: FULL_REFRESH 场景（Mockito mock，CREATE TABLE LIKE / RENAME TABLE 为 MySQL 专有语法）
    // -------------------------------------------------------------------------

    @Test
    public void should_full_refresh_invoke_create_like_and_rename() {
        DapEngineJdbcTemplate mockDapJdbc = Mockito.mock(DapEngineJdbcTemplate.class);
        JdbcTemplate mockJdbc = Mockito.mock(JdbcTemplate.class);
        when(mockDapJdbc.getJdbcTemplate()).thenReturn(mockJdbc);
        when(mockJdbc.batchUpdate(anyString(), anyList(), anyInt(), any()))
                .thenReturn(new int[][] {{1}});

        LocalDataWriter mockWriter = new LocalDataWriter(mockDapJdbc, TENANT_ID, APP_CODE);
        List<Map<String, Object>> records = new ArrayList<>();
        records.add(makeRecord("C001", "Alice"));

        String bakTable = mockWriter.fullRefresh(SUBJECT_CODE, records, "op");

        assertThat(bakTable).startsWith(TABLE_NAME + "_bak_");
        // 验证创建 tmp 表使用 CREATE TABLE LIKE
        verify(mockJdbc).execute(contains("CREATE TABLE `" + TABLE_NAME + "_tmp` LIKE"));
        // 验证原子 RENAME TABLE
        verify(mockJdbc).execute(contains("RENAME TABLE `" + TABLE_NAME + "` TO `" + bakTable + "`"));
    }

    @Test
    public void should_full_refresh_return_bak_table_for_empty_records() {
        DapEngineJdbcTemplate mockDapJdbc = Mockito.mock(DapEngineJdbcTemplate.class);
        JdbcTemplate mockJdbc = Mockito.mock(JdbcTemplate.class);
        when(mockDapJdbc.getJdbcTemplate()).thenReturn(mockJdbc);

        LocalDataWriter mockWriter = new LocalDataWriter(mockDapJdbc, TENANT_ID, APP_CODE);

        String bakTable = mockWriter.fullRefresh(SUBJECT_CODE, new ArrayList<>(), "op");

        assertThat(bakTable).startsWith(TABLE_NAME + "_bak_");
        // 无记录时不调用 batchUpdate
        verify(mockJdbc, never()).batchUpdate(anyString(), anyList(), anyInt(), any());
        // DROP + CREATE LIKE + RENAME 均被调用
        verify(mockJdbc).execute(contains("CREATE TABLE `" + TABLE_NAME + "_tmp` LIKE"));
        verify(mockJdbc).execute(contains("RENAME TABLE `" + TABLE_NAME + "`"));
    }
}
