package com.ruijie.dapengine.provider;

import com.alibaba.druid.pool.DruidDataSource;
import com.ruijie.dapengine.common.model.FetchResult;
import com.ruijie.dapengine.common.model.SyncCheckpoint;
import com.ruijie.dapengine.common.model.SyncDataSourceConfig;
import com.ruijie.dapengine.common.model.TestConnectResultDTO;
import com.ruijie.dapengine.common.util.AesCipher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DB DataProvider 实现。
 *
 * <p>通过临时 Druid 连接池（最大 1 连接）执行外部数据库查询：
 * <ul>
 *   <li>testConnect：setMaxRows(5)，快速验证可达性，返回 sourceFields + ≤5 行样例数据</li>
 *   <li>fetch：绑定 {@code checkpoint.lastVersion} 到 querySql 的 {@code ?} 占位符，全量拉取</li>
 * </ul>
 * 每次调用均 try-with-resources 创建/销毁临时连接池，不持有长期连接。</p>
 */
public class DbDataProvider implements DataProvider {

    /** testConnect 最多返回的样例行数（通过 setMaxRows 实现，不修改 SQL） */
    private static final int MAX_SAMPLE_ROWS = 5;

    /** 临时连接池连接超时（毫秒） */
    private static final int CONNECT_TIMEOUT_MS = 3000;

    /** 临时连接池最大连接数 */
    private static final int POOL_SIZE = 1;

    private final AesCipher aesCipher;

    public DbDataProvider(AesCipher aesCipher) {
        this.aesCipher = aesCipher;
    }

    @Override
    public String type() {
        return "DB";
    }

    @Override
    public TestConnectResultDTO testConnect(SyncDataSourceConfig config) {
        DruidDataSource dataSource = null;
        try {
            dataSource = buildDataSource(config);
            String querySql = config.getQuerySql();
            List<String> sourceFields = new ArrayList<>();
            List<Map<String, Object>> sampleRows = new ArrayList<>();

            // 将用户可读占位符 ${lastSyncTime} 替换为 JDBC 占位符 ?
            String execSql = querySql != null ? querySql.replace("${lastSyncTime}", "?") : querySql;
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(execSql)) {
                ps.setMaxRows(MAX_SAMPLE_ROWS);
                // DELTA 模式 querySql 含 ${lastSyncTime}，testConnect 时绑定 epoch 作为参考值
                // 使用 Timestamp 绑定，JDBC 自动兼容 DATETIME/TIMESTAMP 列，无需用户手写 FROM_UNIXTIME
                if (execSql != null && execSql.contains("?")) {
                    ps.setTimestamp(1, new Timestamp(0L));
                }
                try (ResultSet rs = ps.executeQuery()) {
                    ResultSetMetaData metaData = rs.getMetaData();
                    int colCount = metaData.getColumnCount();
                    for (int i = 1; i <= colCount; i++) {
                        sourceFields.add(metaData.getColumnName(i));
                    }
                    while (rs.next()) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        for (int i = 1; i <= colCount; i++) {
                            row.put(metaData.getColumnName(i), rs.getObject(i));
                        }
                        sampleRows.add(row);
                    }
                }
            }
            return TestConnectResultDTO.success("DB", sourceFields, sampleRows);
        } catch (Exception e) {
            return TestConnectResultDTO.failure("DB", e.getMessage());
        } finally {
            closeQuietly(dataSource);
        }
    }

    @Override
    public FetchResult fetch(SyncDataSourceConfig config, SyncCheckpoint checkpoint) {
        DruidDataSource dataSource = null;
        try {
            dataSource = buildDataSource(config);
            String querySql = config.getQuerySql();
            List<Map<String, Object>> rows = new ArrayList<>();

            // 将用户可读占位符 ${lastSyncTime} 替换为 JDBC 占位符 ?
            String execSql = querySql != null ? querySql.replace("${lastSyncTime}", "?") : querySql;
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(execSql)) {
                // DELTA 模式：绑定 lastVersion 到 ${lastSyncTime} 对应的 ?
                // 使用 Timestamp 绑定，JDBC 自动兼容 DATETIME/TIMESTAMP 列，用户直接写 >= ${lastSyncTime} 即可
                if (execSql != null && execSql.contains("?") && checkpoint != null) {
                    ps.setTimestamp(1, new Timestamp(checkpoint.getLastVersion()));
                }
                try (ResultSet rs = ps.executeQuery()) {
                    ResultSetMetaData metaData = rs.getMetaData();
                    int colCount = metaData.getColumnCount();
                    while (rs.next()) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        for (int i = 1; i <= colCount; i++) {
                            row.put(metaData.getColumnName(i), rs.getObject(i));
                        }
                        rows.add(row);
                    }
                }
            }
            return FetchResult.of(rows);
        } catch (Exception e) {
            throw new RuntimeException("[DAP Engine] DB fetch failed: " + e.getMessage(), e);
        } finally {
            closeQuietly(dataSource);
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private DruidDataSource buildDataSource(SyncDataSourceConfig config) {
        String password = config.getPassword();
        // 运行时解密 password
        if (password != null && aesCipher.isEncrypted(password)) {
            password = aesCipher.decrypt(password);
        }
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setName("dap-db-probe");
        dataSource.setUrl(config.getJdbcUrl());
        dataSource.setUsername(config.getUsername());
        dataSource.setPassword(password);
        dataSource.setInitialSize(1);
        dataSource.setMinIdle(1);
        dataSource.setMaxActive(POOL_SIZE);
        dataSource.setMaxWait(CONNECT_TIMEOUT_MS);
        dataSource.setTestOnBorrow(false);
        dataSource.setTestOnReturn(false);
        dataSource.setTestWhileIdle(false);
        return dataSource;
    }

    private void closeQuietly(DruidDataSource ds) {
        if (ds != null && !ds.isClosed()) {
            try {
                ds.close();
            } catch (Exception ignored) {
            }
        }
    }
}
