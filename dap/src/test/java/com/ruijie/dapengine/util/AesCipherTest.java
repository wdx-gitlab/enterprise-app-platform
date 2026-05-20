package com.ruijie.dapengine.util;

import com.ruijie.dapengine.common.exception.DapValidationException;
import com.ruijie.dapengine.common.util.AesCipher;
import org.junit.Test;

import static org.junit.Assert.*;

public class AesCipherTest {

    private static final String KEY = "ThisIsA32CharEncryptKeyForTest!!";
    private final AesCipher cipher = new AesCipher(KEY);

    @Test
    public void encrypt_then_decrypt_should_return_original() {
        String plaintext = "mySecretPassword123";
        String encrypted = cipher.encrypt(plaintext);
        String decrypted = cipher.decrypt(encrypted);
        assertEquals(plaintext, decrypted);
    }

    @Test
    public void encrypted_value_should_have_prefix() {
        String encrypted = cipher.encrypt("test");
        assertTrue("应以 AES:ENC( 开头", encrypted.startsWith("AES:ENC("));
        assertTrue("应以 ) 结尾", encrypted.endsWith(")"));
    }

    @Test
    public void isEncrypted_should_return_true_for_encrypted_value() {
        String encrypted = cipher.encrypt("hello");
        assertTrue(cipher.isEncrypted(encrypted));
    }

    @Test
    public void isEncrypted_should_return_false_for_plaintext() {
        assertFalse(cipher.isEncrypted("plaintext"));
        assertFalse(cipher.isEncrypted(null));
        assertFalse(cipher.isEncrypted(""));
    }

    @Test
    public void encrypt_already_encrypted_value_should_return_as_is() {
        String encrypted = cipher.encrypt("hello");
        String encryptedAgain = cipher.encrypt(encrypted);
        assertEquals("已加密值不应二次加密", encrypted, encryptedAgain);
    }

    @Test
    public void plaintext_should_not_appear_in_encrypted_result() {
        String plaintext = "sensitivePassword";
        String encrypted = cipher.encrypt(plaintext);
        assertFalse("明文不应出现在加密结果中", encrypted.contains(plaintext));
    }

    @Test
    public void decrypt_null_should_return_null() {
        assertNull(cipher.decrypt(null));
    }

    @Test
    public void decrypt_non_encrypted_should_return_as_is() {
        String plain = "not_encrypted";
        assertEquals(plain, cipher.decrypt(plain));
    }

    @Test(expected = DapValidationException.class)
    public void validateKey_null_should_throw() {
        AesCipher.validateKey(null);
    }

    @Test(expected = DapValidationException.class)
    public void validateKey_empty_should_throw() {
        AesCipher.validateKey("");
    }

    @Test
    public void validateKey_valid_key_should_not_throw() {
        // 短密钥也允许（通过 SHA-256 派生为 32 字节）
        AesCipher.validateKey("short");
        AesCipher.validateKey(KEY);
    }
}
