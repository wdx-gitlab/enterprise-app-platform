package com.ruijie.authzengine.infrastructure.authz;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * BO 字段映射支持工具。
 *
 * <p>统一收口 attributes[].fieldName / columnName / code 的字段定位顺序，以及
 * {@code isPk=true} 属性派生主键列集合的逻辑，供 PDP 与 Starter 执行层复用。</p>
 */
public final class BoFieldMappingSupport {

    public static final String ROW_FILTER_PK_COLUMNS_KEY = "pkColumnNames";

    private BoFieldMappingSupport() {
    }

    public static String resolveTargetKey(Map<String, Object> fieldMetadata) {
        if (fieldMetadata == null || fieldMetadata.isEmpty()) {
            return null;
        }
        String fieldName = trimToNull(fieldMetadata.get("fieldName"));
        if (fieldName != null) {
            return fieldName;
        }
        String columnName = trimToNull(fieldMetadata.get("columnName"));
        if (columnName != null) {
            return columnName;
        }
        return trimToNull(fieldMetadata.get("code"));
    }

    public static List<String> resolvePrimaryKeyColumns(List<Map<String, Object>> attributes, String primaryTableName) {
        if (attributes == null || attributes.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        String normalizedPrimaryTableName = trimToNull(primaryTableName);
        Set<String> pkColumns = new LinkedHashSet<>();
        for (Map<String, Object> attribute : attributes) {
            if (attribute == null || !toBoolean(attribute.get("isPk"))) {
                continue;
            }
            String attributeTable = trimToNull(attribute.get("tableName"));
            if (normalizedPrimaryTableName != null && attributeTable != null
                && !normalizedPrimaryTableName.equalsIgnoreCase(attributeTable)) {
                continue;
            }
            String columnName = trimToNull(attribute.get("columnName"));
            if (columnName == null) {
                columnName = trimToNull(attribute.get("fieldName"));
            }
            if (columnName == null) {
                columnName = trimToNull(attribute.get("code"));
            }
            if (columnName != null) {
                pkColumns.add(columnName);
            }
        }
        return new ArrayList<>(pkColumns);
    }

    private static boolean toBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return value != null && Boolean.parseBoolean(String.valueOf(value));
    }

    private static String trimToNull(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return StringUtils.hasText(text) ? text : null;
    }
}