package com.ruijie.uspportal.auth.util;

import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/**
 * Local portal account password codec.
 *
 * <p>Current MVP rule is aligned with the seed SQL script:
 * <ul>
 *     <li>encode type: SHA256_UPPER</li>
 *     <li>raw password bytes: UTF-8</li>
 *     <li>digest algorithm: SHA-256</li>
 *     <li>stored hash: uppercase hex string</li>
 * </ul>
 */
public final class LocalAccountPasswordCodec {

    public static final String ENCODE_TYPE_SHA256_UPPER = "SHA256_UPPER";

    private LocalAccountPasswordCodec() {
    }

    public static String encode(String rawPassword, String encodeType) {
        String resolvedEncodeType = StringUtils.hasText(encodeType) ? encodeType : ENCODE_TYPE_SHA256_UPPER;
        if (ENCODE_TYPE_SHA256_UPPER.equalsIgnoreCase(resolvedEncodeType)) {
            return encodeSha256Upper(rawPassword);
        }
        throw new IllegalArgumentException("Unsupported password encode type: " + resolvedEncodeType);
    }

    public static boolean matches(String rawPassword, String encodedPassword, String encodeType) {
        if (!StringUtils.hasText(encodedPassword)) {
            return false;
        }
        return encodedPassword.equals(encode(rawPassword, encodeType));
    }

    public static String encodeSha256Upper(String rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("rawPassword must not be null");
        }
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] digest = messageDigest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            return toUpperHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm not available", ex);
        }
    }

    private static String toUpperHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte current : bytes) {
            builder.append(String.format(Locale.ROOT, "%02X", current));
        }
        return builder.toString();
    }
}

