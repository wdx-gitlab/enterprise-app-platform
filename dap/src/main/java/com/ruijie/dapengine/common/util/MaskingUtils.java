package com.ruijie.dapengine.common.util;

import java.util.regex.Pattern;

/**
 * 敏感值掩码工具。
 */
public final class MaskingUtils {

    private static final String MASK = "******";
    private static final Pattern SENSITIVE_HEADER_PATTERN =
        Pattern.compile(".*(authorization|token|secret|api[-_]?key|password|passwd|credential).*",
            Pattern.CASE_INSENSITIVE);

    private MaskingUtils() {
    }

    /**
     * 对任意敏感值返回掩码字符串 {@code ******}。
     * null 值也返回 {@code ******}。
     */
    public static String mask(String value) {
        return MASK;
    }

    /**
     * 判断 HTTP Header 键名是否为敏感键（忽略大小写）。
     * 匹配规则：包含 authorization、token、secret、api-key、api_key、apikey、password、passwd、credential 等。
     */
    public static boolean isSensitiveHeaderKey(String headerName) {
        if (headerName == null || headerName.trim().isEmpty()) {
            return false;
        }
        return SENSITIVE_HEADER_PATTERN.matcher(headerName).matches();
    }
}
