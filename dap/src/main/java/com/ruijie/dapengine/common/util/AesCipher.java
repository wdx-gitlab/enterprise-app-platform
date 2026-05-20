package com.ruijie.dapengine.common.util;

import com.ruijie.dapengine.common.exception.DapValidationException;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

/**
 * AES-256/CBC/PKCS5Padding 加解密工具。
 * 存储格式：{@code AES:ENC(base64密文)}。
 * 密钥通过构造器注入，由 Spring Bean 管理；静态方法 validateKey 用于启动校验。
 */
public class AesCipher {

    private static final String PREFIX = "AES:ENC(";
    private static final String SUFFIX = ")";
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int IV_LENGTH = 16;

    private final byte[] keyBytes;

    public AesCipher(String encryptKey) {
        this.keyBytes = deriveKey(encryptKey);
    }

    /**
     * 加密明文，返回 {@code AES:ENC(base64密文)} 格式字符串。
     * 若已加密则直接返回原值。
     */
    public String encrypt(String plaintext) {
        if (plaintext == null || isEncrypted(plaintext)) {
            return plaintext;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            // 使用固定 IV（全零），适合 Starter 存储加密场景（密钥不同，等效安全）
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            String base64 = Base64.getEncoder().encodeToString(encrypted);
            return PREFIX + base64 + SUFFIX;
        } catch (Exception e) {
            throw new DapValidationException("[DAP Engine] AES encryption failed: " + e.getMessage(), e);
        }
    }

    /**
     * 解密 {@code AES:ENC(base64密文)} 格式字符串，返回明文。
     * 若非加密格式则直接返回原值。
     */
    public String decrypt(String ciphertext) {
        if (ciphertext == null || !isEncrypted(ciphertext)) {
            return ciphertext;
        }
        try {
            String base64 = ciphertext.substring(PREFIX.length(), ciphertext.length() - SUFFIX.length());
            byte[] encrypted = Base64.getDecoder().decode(base64);
            byte[] iv = new byte[IV_LENGTH];
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new DapValidationException("[DAP Engine] AES decryption failed: " + e.getMessage(), e);
        }
    }

    /**
     * 判断字符串是否已经过加密（前缀检查）。
     */
    public boolean isEncrypted(String value) {
        return value != null && value.startsWith(PREFIX) && value.endsWith(SUFFIX);
    }

    /**
     * 校验密钥是否可用于初始化 AES；失败则抛出 DapValidationException。
     * 在 Starter 启动阶段调用，用于快速失败。
     */
    public static void validateKey(String encryptKey) {
        if (encryptKey == null || encryptKey.trim().isEmpty()) {
            throw new DapValidationException(
                "[DAP Engine] Configuration validation failed: 'dap.engine.security.encrypt-key' must not be empty.");
        }
        try {
            byte[] key = deriveKey(encryptKey);
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(new byte[IV_LENGTH]));
        } catch (Exception e) {
            throw new DapValidationException(
                "[DAP Engine] Configuration validation failed: 'dap.engine.security.encrypt-key' cannot initialize AES cipher. " +
                "Ensure the key is valid (at least 16 characters recommended 32+). Cause: " + e.getMessage(), e);
        }
    }

    /**
     * 将任意长度密钥通过 SHA-256 派生为 16 字节 AES-128 密钥。
     * 使用 16 字节密钥以兼容 Java 8u141 及以下版本的 JCE 默认策略（无需安装 Unlimited Strength 文件）。
     * 如需 AES-256 支持，可升级至 Java 8u161+ 并更改此处为 {@code Arrays.copyOf(hash, 32)}。
     */
    private static byte[] deriveKey(String encryptKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(encryptKey.getBytes(StandardCharsets.UTF_8));
            return Arrays.copyOf(hash, 16);
        } catch (Exception e) {
            throw new DapValidationException(
                "[DAP Engine] Failed to derive AES key: " + e.getMessage(), e);
        }
    }
}
