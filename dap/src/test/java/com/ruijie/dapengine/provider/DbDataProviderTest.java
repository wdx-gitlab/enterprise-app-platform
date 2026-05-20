package com.ruijie.dapengine.provider;

import com.ruijie.dapengine.common.model.FetchResult;
import com.ruijie.dapengine.common.model.SyncCheckpoint;
import com.ruijie.dapengine.common.model.SyncDataSourceConfig;
import com.ruijie.dapengine.common.model.TestConnectResultDTO;
import com.ruijie.dapengine.common.util.AesCipher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.assertj.core.api.Assertions.*;

/**
 * DbDataProvider 单元测试（以 H2 内存库模拟外部数据库）。
 * 覆盖：testConnect、fetch、AES 加密密码解密、无增量参数时的全量查询。
 */
@RunWith(JUnit4.class)
public class DbDataProviderTest {

    private static final String VALID_KEY = "TestEncryptKey32CharactersLong!!";
    private static final String H2_URL = "jdbc:h2:mem:ext_test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
    private static final String H2_DRIVER = "org.h2.Driver";
    private static final String H2_USER = "sa";
    private static final String H2_PWD = "";

    private AesCipher aesCipher;
    private DbDataProvider provider;

    @Before
    public void setUp() throws Exception {
        aesCipher = new AesCipher(VALID_KEY);
        provider = new DbDataProvider(aesCipher);

        // 预建 H2 测试表和数据
        java.sql.Connection conn = java.sql.DriverManager
                .getConnection(H2_URL, H2_USER, H2_PWD);
        conn.createStatement().execute("DROP TABLE IF EXISTS dap_person");
        conn.createStatement().execute(
                "CREATE TABLE dap_person (id BIGINT, name VARCHAR(64), version BIGINT)");
        conn.createStatement().execute(
                "INSERT INTO dap_person VALUES (1,'Alice',100),(2,'Bob',200),(3,'Carol',300)");
        conn.close();
    }

    private SyncDataSourceConfig buildDbConfig(String sql, boolean encryptPassword) {
        SyncDataSourceConfig ds = new SyncDataSourceConfig();
        ds.setJdbcUrl(H2_URL);
        ds.setUsername(H2_USER);
        if (encryptPassword) {
            ds.setPassword(aesCipher.encrypt(H2_PWD));
        } else {
            ds.setPassword(H2_PWD);
        }
        ds.setQuerySql(sql);
        return ds;
    }

    @Test
    public void testConnect_should_return_sourceFields_from_h2_table() {
        SyncDataSourceConfig ds = buildDbConfig("SELECT id, name, version FROM dap_person", false);
        TestConnectResultDTO result = provider.testConnect(ds);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getSourceFields()).containsExactlyInAnyOrder("ID", "NAME", "VERSION");
        assertThat(result.getSampleRows()).hasSizeLessThanOrEqualTo(5);
        assertThat(result.getSampleRows()).hasSize(3);
    }

    @Test
    public void testConnect_should_decrypt_aes_encrypted_password() {
        SyncDataSourceConfig ds = buildDbConfig("SELECT id, name FROM dap_person", true);
        TestConnectResultDTO result = provider.testConnect(ds);

        // AES 解密后密码正确，连接成功
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getSourceFields()).containsExactlyInAnyOrder("ID", "NAME");
    }

    @Test
    public void testConnect_should_return_failure_on_invalid_url() {
        SyncDataSourceConfig ds = new SyncDataSourceConfig();
        ds.setJdbcUrl("jdbc:h2:mem:NONEXISTENT;invalid_option=oops");
        ds.setUsername(H2_USER);
        ds.setPassword(H2_PWD);
        ds.setQuerySql("SELECT 1 FROM NONEXISTENT_TABLE");

        TestConnectResultDTO result = provider.testConnect(ds);
        // H2 不存在的表会抛异常，期望返回失败
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMsg()).isNotBlank();
    }

    @Test
    public void fetch_should_return_all_rows_with_no_checkpoint() {
        SyncDataSourceConfig ds = buildDbConfig("SELECT id, name, version FROM dap_person", false);
        SyncCheckpoint checkpoint = SyncCheckpoint.empty("test-subject");

        FetchResult result = provider.fetch(ds, checkpoint);

        assertThat(result.isEmpty()).isFalse();
        assertThat(result.getRecords()).hasSize(3);
        assertThat(result.getRecords().get(0)).containsKey("ID");
    }

    @Test
    public void fetch_should_bind_checkpoint_to_incremental_param() {
        // SQL 包含 ? 时触发增量绑定，lastVersion=100 时仅返回 version > 100 的记录
        String sql = "SELECT id, name, version FROM dap_person WHERE version > ?";
        SyncDataSourceConfig ds = buildDbConfig(sql, false);
        SyncCheckpoint checkpoint = new SyncCheckpoint("test-subject", 100L, null, 0L);

        FetchResult result = provider.fetch(ds, checkpoint);

        assertThat(result.isEmpty()).isFalse();
        // version > 100: Bob(200), Carol(300)
        assertThat(result.getRecords()).hasSize(2);
    }

    @Test
    public void type_should_return_DB() {
        assertThat(provider.type()).isEqualTo("DB");
    }
}
