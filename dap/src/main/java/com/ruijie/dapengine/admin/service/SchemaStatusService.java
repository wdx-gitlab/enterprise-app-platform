package com.ruijie.dapengine.admin.service;

import com.ruijie.dapengine.common.enums.SchemaStatus;
import com.ruijie.dapengine.common.model.FieldConfigDTO;
import com.ruijie.dapengine.common.util.FieldSchemaHelper;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 动态计算 Subject 的 schemaStatus。
 * 通过 JDBC DatabaseMetaData 比对物理表与元数据字段集，避免 information_schema 跨 schema/跨库同名表误判。
 */
public class SchemaStatusService {

    private static final String DYN_TABLE_PREFIX = "dap_";

    private final JdbcTemplate jdbcTemplate;

    public SchemaStatusService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 计算 subject 对应动态表的 schemaStatus。
     *
     * @param subjectCode   Subject code（如 CUSTOMER）
     * @param activeFields  有效字段列表（is_delete=0）
     * @return APPLIED 或 PENDING
     */
    public SchemaStatus computeStatus(String subjectCode, List<FieldConfigDTO> activeFields) {
        return describePendingReasons(subjectCode, activeFields).isEmpty()
            ? SchemaStatus.APPLIED
            : SchemaStatus.PENDING;
    }

    /**
     * 返回导致 schemaStatus=PENDING 的原因明细。
     */
    public List<String> describePendingReasons(String subjectCode, List<FieldConfigDTO> activeFields) {
        String tableName = DYN_TABLE_PREFIX + subjectCode.toLowerCase();
        Map<String, ColumnDefinition> columnDefinitionMap = getColumnDefinitionMap(tableName);
        List<String> reasons = new ArrayList<>();
        if (columnDefinitionMap == null) {
            reasons.add("动态表不存在：" + tableName);
            return reasons;
        }
        for (FieldConfigDTO field : activeFields) {
            String fieldName = field.getFieldName() == null ? "" : field.getFieldName().toLowerCase();
            ColumnDefinition definition = columnDefinitionMap.get(fieldName);
            if (definition == null) {
                reasons.add("缺少列：" + field.getFieldName());
                continue;
            }
            if (!isColumnCompatible(field, definition)) {
                reasons.add("列不匹配：" + field.getFieldName()
                    + "，期望=" + buildExpectedDefinition(field)
                    + "，实际=" + buildActualDefinition(definition));
            }
        }
        return reasons;
    }

    /**
     * 判断动态表是否存在。
     *
     * @param tableName 表名（如 dap_customer）
     * @return true 表示表已存在
     */
    public boolean tableExists(String tableName) {
        return getColumnDefinitionMap(tableName) != null;
    }

    /**
     * 返回物理表当前实际存在的列名（小写）。
     * 若表不存在或查询异常返回 null。
     */
    public List<String> getPhysicalColumns(String tableName) {
        Map<String, ColumnDefinition> columnDefinitionMap = getColumnDefinitionMap(tableName);
        if (columnDefinitionMap == null) {
            return null;
        }
        return new java.util.ArrayList<>(columnDefinitionMap.keySet());
    }

    /**
     * 返回列名 → 物理列定义映射，用于 schemaStatus 和 apply-schema diff。
     * 若表不存在或查询异常返回 null。
     */
    public Map<String, ColumnDefinition> getColumnDefinitionMap(String tableName) {
        try {
            return jdbcTemplate.execute((ConnectionCallback<Map<String, ColumnDefinition>>) connection -> {
                DatabaseMetaData metaData = connection.getMetaData();
                String catalog = connection.getCatalog();
                // MySQL 以 catalog 表示数据库，schema 始终为 null。
                // 若 schema 非 null（某些 JDBC 版本会返回 DB 名），与 catalog 同时传入会导致
                // getColumns/getTables 在 MySQL JDBC 5.1.x 上返回空结果，误判为"表不存在"。
                String schema = null;
                String actualTableName = resolveActualTableName(metaData, catalog, schema, tableName);
                if (actualTableName == null) {
                    return null;
                }
                Map<String, ColumnDefinition> result = new LinkedHashMap<>();
                try (ResultSet columns = metaData.getColumns(catalog, schema, actualTableName, null)) {
                    while (columns.next()) {
                        String columnName = columns.getString("COLUMN_NAME");
                        if (columnName == null) {
                            continue;
                        }
                        String typeName = normalizeTypeName(columns.getString("TYPE_NAME"));
                        int jdbcType = columns.getInt("DATA_TYPE");
                        int columnSize = columns.getInt("COLUMN_SIZE");
                        Long maxLength = columns.wasNull() ? null : Long.valueOf(columnSize);
                        result.put(columnName.toLowerCase(), new ColumnDefinition(
                            typeName != null ? typeName : String.valueOf(jdbcType),
                            maxLength));
                    }
                }
                return result.isEmpty() ? null : result;
            });
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 返回列名 → FieldType Token 映射，用于 ADD COLUMN / MODIFY COLUMN diff。
     * 若表不存在或查询异常返回 null。
     * FieldType Token 与 {@link com.ruijie.dapengine.common.enums.FieldType} 枚举名对应。
     *
     * @param tableName 表名（如 dap_customer）
     * @return 列名（小写）→ FieldType 枚举名 的 LinkedHashMap，表不存在时为 null
     */
    public Map<String, String> getColumnTypeMap(String tableName) {
        Map<String, ColumnDefinition> definitions = getColumnDefinitionMap(tableName);
        if (definitions == null) {
            return null;
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, ColumnDefinition> entry : definitions.entrySet()) {
            result.put(entry.getKey(), toFieldTypeToken(entry.getValue().getDataType(), entry.getValue().getMaxLength()));
        }
        return result;
    }

    /**
     * 判断当前物理列是否与配置字段兼容。
     */
    public boolean isColumnCompatible(FieldConfigDTO field, ColumnDefinition definition) {
        if (field == null || definition == null || definition.getDataType() == null) {
            return false;
        }
        String fieldType = field.getFieldType();
        if (FieldSchemaHelper.isVarcharFamily(fieldType)) {
            return isVarcharType(definition.getDataType())
                && Objects.equals(FieldSchemaHelper.normalizeMaxLength(fieldType, field.getMaxLength()), definition.getMaxLengthAsInteger());
        }
        switch (fieldType) {
            case "TEXT":
                return isTextType(definition.getDataType());
            case "INT":
                return isIntegerType(definition.getDataType());
            case "DECIMAL":
                return isDecimalType(definition.getDataType());
            case "DATE":
                return "DATE".equals(definition.getDataType()) || "91".equals(definition.getDataType());
            case "DATETIME":
                return "DATETIME".equals(definition.getDataType())
                    || "TIMESTAMP".equals(definition.getDataType())
                    || "93".equals(definition.getDataType());
            default:
                return Objects.equals(toFieldTypeToken(definition.getDataType(), definition.getMaxLength()), fieldType);
        }
    }

    private String resolveActualTableName(DatabaseMetaData metaData, String catalog, String schema, String tableName) throws SQLException {
        String[] candidates = new String[]{tableName, tableName.toUpperCase(), tableName.toLowerCase()};
        for (String candidate : candidates) {
            try (ResultSet tables = metaData.getTables(catalog, schema, candidate, new String[]{"TABLE"})) {
                while (tables.next()) {
                    String actualTableName = tables.getString("TABLE_NAME");
                    if (actualTableName != null && actualTableName.equalsIgnoreCase(tableName)) {
                        return actualTableName;
                    }
                }
            }
        }
        return null;
    }

    private String normalizeTypeName(String typeName) {
        return typeName == null ? null : typeName.trim().toUpperCase();
    }

    private String buildExpectedDefinition(FieldConfigDTO field) {
        if (field == null) {
            return "UNKNOWN";
        }
        if (FieldSchemaHelper.isVarcharFamily(field.getFieldType())) {
            return field.getFieldType() + "(" + FieldSchemaHelper.normalizeMaxLength(field.getFieldType(), field.getMaxLength()) + ")";
        }
        return field.getFieldType();
    }

    private String buildActualDefinition(ColumnDefinition definition) {
        if (definition == null) {
            return "UNKNOWN";
        }
        if (definition.getMaxLength() != null && isVarcharType(definition.getDataType())) {
            return definition.getDataType() + "(" + definition.getMaxLength() + ")";
        }
        return definition.getDataType();
    }

    /**
     * information_schema DATA_TYPE + CHARACTER_MAXIMUM_LENGTH → FieldType 枚举名字符串。
     */
    private String toFieldTypeToken(String dataType, Long charMaxLen) {
        if (dataType == null) return "UNKNOWN";
        switch (dataType) {
            case "VARCHAR": case "NVARCHAR": case "CHARACTER VARYING": case "12":
                if (charMaxLen != null && charMaxLen <= 64)  return "ENUM";
                if (charMaxLen != null && charMaxLen <= 128) return "STRING";
                return "STRING_LONG";
            case "CLOB": case "TEXT": case "LONGTEXT": case "MEDIUMTEXT": case "2005": case "-1":
                return "TEXT";
            case "BIGINT": case "INTEGER": case "INT": case "SMALLINT":
            case "-5": case "4": case "5":
                return "INT";
            case "DECIMAL": case "NUMERIC": case "3": case "2":
                return "DECIMAL";
            case "DATE": case "91":
                return "DATE";
            case "TIMESTAMP": case "DATETIME": case "93":
                return "DATETIME";
            default:
                return dataType;
        }
    }

    private boolean isVarcharType(String dataType) {
        return "VARCHAR".equals(dataType) || "NVARCHAR".equals(dataType) || "CHARACTER VARYING".equals(dataType) || "12".equals(dataType);
    }

    private boolean isTextType(String dataType) {
        return "CLOB".equals(dataType)
            || "TEXT".equals(dataType)
            || "LONGTEXT".equals(dataType)
            || "MEDIUMTEXT".equals(dataType)
            || "2005".equals(dataType)
            || "-1".equals(dataType);
    }

    private boolean isIntegerType(String dataType) {
        return "BIGINT".equals(dataType)
            || "INTEGER".equals(dataType)
            || "INT".equals(dataType)
            || "SMALLINT".equals(dataType)
            || "-5".equals(dataType)
            || "4".equals(dataType)
            || "5".equals(dataType);
    }

    private boolean isDecimalType(String dataType) {
        return "DECIMAL".equals(dataType)
            || "NUMERIC".equals(dataType)
            || "3".equals(dataType)
            || "2".equals(dataType);
    }

    public static final class ColumnDefinition {
        private final String dataType;
        private final Long maxLength;

        public ColumnDefinition(String dataType, Long maxLength) {
            this.dataType = dataType;
            this.maxLength = maxLength;
        }

        public String getDataType() {
            return dataType;
        }

        public Long getMaxLength() {
            return maxLength;
        }

        public Integer getMaxLengthAsInteger() {
            return maxLength == null ? null : maxLength.intValue();
        }
    }
}
