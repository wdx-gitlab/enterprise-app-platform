package com.ruijie.authzengine.infrastructure.authz;

import com.ruijie.authzengine.application.spi.BoSchemaColumnInfo;
import com.ruijie.authzengine.application.spi.NativeBoSchemaCollector;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 基于 JDBC {@link DatabaseMetaData} 的 Native BO 元数据采集器。
 *
 * <p>从应用主数据源直接读取表/列信息，适用于权限引擎与业务库同数据源场景。
 * 仅在 Spring 容器中存在 {@link DataSource} 时才会注册为 Bean。
 *
 * <p><b>安全约束</b>：只执行 {@code DatabaseMetaData} 的只读查询，
 * 不执行任何 DDL/DML 语句。表名从参数传入，不进行 SQL 拼接，无注入风险。
 *
 * <p>采集结果仅作为候选展示，<b>不会直接落库</b>，必须经过管理员治理确认。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JdbcNativeBoSchemaCollector implements NativeBoSchemaCollector {

    private final DataSource dataSource;

    /**
     * 通过 JDBC {@code DatabaseMetaData.getColumns()} 采集指定表的列元数据。
     *
     * <p>若表不存在或数据源访问失败，返回空列表，不抛出异常。
     *
     * @param tableName 物理表名，不能为空
     * @return 结构化列信息列表；表不存在或采集失败时返回空列表
     */
    @Override
    public List<BoSchemaColumnInfo> fetchColumns(String tableName) {
        if (!StringUtils.hasText(tableName)) {
            log.warn("[JdbcNativeBoSchemaCollector] 表名为空，跳过采集");
            return new ArrayList<>();
        }
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String catalog = connection.getCatalog();
            String schema = connection.getSchema();

            // 先查主键列集合，后续标记 isPrimaryKey
            Set<String> primaryKeyColumns = collectPrimaryKeyColumns(metaData, catalog, schema, tableName);

            return collectColumns(metaData, catalog, schema, tableName, primaryKeyColumns);
        } catch (SQLException exception) {
            log.warn("[JdbcNativeBoSchemaCollector] 采集表元数据失败，降级为空列表 table={} cause={}",
                tableName, exception.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 查询主键列集合。
     */
    private Set<String> collectPrimaryKeyColumns(
        DatabaseMetaData metaData, String catalog, String schema, String tableName
    ) throws SQLException {
        Set<String> pkColumns = new HashSet<>();
        try (ResultSet pkRs = metaData.getPrimaryKeys(catalog, schema, tableName)) {
            while (pkRs.next()) {
                String columnName = pkRs.getString("COLUMN_NAME");
                if (StringUtils.hasText(columnName)) {
                    pkColumns.add(columnName);
                }
            }
        }
        return pkColumns;
    }

    /**
     * 按表名采集列信息，并依据主键集合标记 {@code isPrimaryKey}。
     */
    private List<BoSchemaColumnInfo> collectColumns(
        DatabaseMetaData metaData, String catalog, String schema, String tableName,
        Set<String> primaryKeyColumns
    ) throws SQLException {
        List<BoSchemaColumnInfo> columns = new ArrayList<>();
        // getColumns 参数：catalog, schemaPattern, tableNamePattern, columnNamePattern
        try (ResultSet rs = metaData.getColumns(catalog, schema, tableName, "%")) {
            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME");
                String typeName = rs.getString("TYPE_NAME");
                int columnSize = rs.getInt("COLUMN_SIZE");
                int decimalDigits = rs.getInt("DECIMAL_DIGITS");
                boolean isNullable = DatabaseMetaData.columnNullable == rs.getInt("NULLABLE");
                String remarks = rs.getString("REMARKS");

                String columnType = buildColumnType(typeName, columnSize, decimalDigits);
                columns.add(BoSchemaColumnInfo.builder()
                    .tableName(tableName)
                    .columnName(columnName)
                    .columnType(columnType)
                    .isPrimaryKey(primaryKeyColumns.contains(columnName))
                    .nullable(isNullable)
                    .comment(remarks)
                    .build());
            }
        }
        return columns;
    }

    /**
     * 拼接列类型字符串，如 {@code VARCHAR(64)}、{@code DECIMAL(18,2)}。
     */
    private String buildColumnType(String typeName, int columnSize, int decimalDigits) {
        if (!StringUtils.hasText(typeName)) {
            return "UNKNOWN";
        }
        // 无长度限制类型（BIGINT, INT, TEXT, DATETIME 等）
        if (columnSize <= 0) {
            return typeName.toUpperCase();
        }
        // 小数类型
        if (decimalDigits > 0) {
            return typeName.toUpperCase() + "(" + columnSize + "," + decimalDigits + ")";
        }
        return typeName.toUpperCase() + "(" + columnSize + ")";
    }
}
