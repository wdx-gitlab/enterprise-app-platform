package com.ruijie.dapengine.common.util;

/**
 * 字段 Schema 辅助工具。
 * 统一处理字符串字段默认长度、长度归一化与数据库类型映射，避免前后端和 DDL 规则不一致。
 */
public final class FieldSchemaHelper {

    public static final int DEFAULT_STRING_LENGTH = 128;
    public static final int DEFAULT_STRING_LONG_LENGTH = 1024;
    public static final int DEFAULT_ENUM_LENGTH = 64;
    public static final int DEFAULT_TEXT_LENGTH = 65535;
    public static final int MAX_VARCHAR_LENGTH = 65535;

    private FieldSchemaHelper() {
    }

    /**
     * 按字段类型返回归一化后的最大长度。
     * 对非长度敏感类型返回 null。
     */
    public static Integer normalizeMaxLength(String fieldType, Integer maxLength) {
        if (fieldType == null) {
            return null;
        }
        switch (fieldType) {
            case "STRING":
                return normalizePositive(maxLength, DEFAULT_STRING_LENGTH);
            case "STRING_LONG":
                return normalizePositive(maxLength, DEFAULT_STRING_LONG_LENGTH);
            case "ENUM":
                return normalizePositive(maxLength, DEFAULT_ENUM_LENGTH);
            case "TEXT":
                return DEFAULT_TEXT_LENGTH;
            default:
                return null;
        }
    }

    public static boolean supportsMaxLength(String fieldType) {
        return isVarcharFamily(fieldType) || "TEXT".equals(fieldType);
    }

    public static boolean isVarcharFamily(String fieldType) {
        return "STRING".equals(fieldType)
            || "STRING_LONG".equals(fieldType)
            || "ENUM".equals(fieldType);
    }

    public static String toDbType(String fieldType, Integer maxLength) {
        if (fieldType == null) {
            return "VARCHAR(" + DEFAULT_STRING_LENGTH + ")";
        }
        switch (fieldType) {
            case "STRING":
            case "STRING_LONG":
            case "ENUM":
                return "VARCHAR(" + normalizeMaxLength(fieldType, maxLength) + ")";
            case "TEXT":
                return "TEXT";
            case "INT":
                return "BIGINT";
            case "DECIMAL":
                return "DECIMAL(18,4)";
            case "DATE":
                return "DATE";
            case "DATETIME":
                return "DATETIME";
            default:
                return "VARCHAR(" + DEFAULT_STRING_LENGTH + ")";
        }
    }

    private static Integer normalizePositive(Integer maxLength, int defaultValue) {
        if (maxLength == null || maxLength <= 0) {
            return defaultValue;
        }
        return maxLength;
    }
}
