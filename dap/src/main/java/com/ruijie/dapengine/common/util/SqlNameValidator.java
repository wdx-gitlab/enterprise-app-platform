package com.ruijie.dapengine.common.util;

import com.ruijie.dapengine.common.exception.DapValidationException;

import java.util.regex.Pattern;

/**
 * SQL 表名/列名合法性校验工具，防止 DDL 注入。
 * 合法格式：小写字母开头，仅含 [a-z0-9_]，长度 1–64。
 */
public final class SqlNameValidator {

    private static final Pattern VALID_NAME = Pattern.compile("^[a-z][a-z0-9_]{0,63}$");
    private static final int MAX_ERROR_DISPLAY_LENGTH = 30;

    private SqlNameValidator() {
    }

    /**
     * 校验表名或列名是否合法。
     *
     * @param name 待校验的名称
     * @throws DapValidationException 若名称不符合白名单规则
     */
    public static void validate(String name) {
        if (name == null || name.isEmpty()) {
            throw new DapValidationException(
                "[DAP Engine] Invalid SQL name: name must not be null or empty.");
        }
        if (!VALID_NAME.matcher(name).matches()) {
            String display = name.length() > MAX_ERROR_DISPLAY_LENGTH
                ? name.substring(0, MAX_ERROR_DISPLAY_LENGTH) + "..."
                : name;
            throw new DapValidationException(
                "[DAP Engine] Invalid SQL name: '" + display +
                "'. Only lowercase letters, digits, and underscores are allowed, " +
                "starting with a lowercase letter, max 64 characters.");
        }
    }

    /**
     * 返回名称是否合法（不抛异常版本，适合条件判断）。
     */
    public static boolean isValid(String name) {
        return name != null && VALID_NAME.matcher(name).matches();
    }
}
