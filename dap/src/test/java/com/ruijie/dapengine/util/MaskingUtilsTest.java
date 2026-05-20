package com.ruijie.dapengine.util;

import com.ruijie.dapengine.common.util.MaskingUtils;
import org.junit.Test;

import static org.junit.Assert.*;

public class MaskingUtilsTest {

    @Test
    public void mask_should_return_asterisks() {
        assertEquals("******", MaskingUtils.mask("secretPassword"));
        assertEquals("******", MaskingUtils.mask(null));
        assertEquals("******", MaskingUtils.mask(""));
    }

    @Test
    public void isSensitiveHeaderKey_authorization_should_be_sensitive() {
        assertTrue(MaskingUtils.isSensitiveHeaderKey("Authorization"));
        assertTrue(MaskingUtils.isSensitiveHeaderKey("authorization"));
        assertTrue(MaskingUtils.isSensitiveHeaderKey("AUTHORIZATION"));
    }

    @Test
    public void isSensitiveHeaderKey_token_variants_should_be_sensitive() {
        assertTrue(MaskingUtils.isSensitiveHeaderKey("X-Auth-Token"));
        assertTrue(MaskingUtils.isSensitiveHeaderKey("access_token"));
    }

    @Test
    public void isSensitiveHeaderKey_secret_should_be_sensitive() {
        assertTrue(MaskingUtils.isSensitiveHeaderKey("client_secret"));
        assertTrue(MaskingUtils.isSensitiveHeaderKey("API-Secret"));
    }

    @Test
    public void isSensitiveHeaderKey_apikey_should_be_sensitive() {
        assertTrue(MaskingUtils.isSensitiveHeaderKey("api-key"));
        assertTrue(MaskingUtils.isSensitiveHeaderKey("apikey"));
        assertTrue(MaskingUtils.isSensitiveHeaderKey("API_KEY"));
    }

    @Test
    public void isSensitiveHeaderKey_password_should_be_sensitive() {
        assertTrue(MaskingUtils.isSensitiveHeaderKey("password"));
        assertTrue(MaskingUtils.isSensitiveHeaderKey("X-Password"));
    }

    @Test
    public void isSensitiveHeaderKey_normal_headers_should_not_be_sensitive() {
        assertFalse(MaskingUtils.isSensitiveHeaderKey("Content-Type"));
        assertFalse(MaskingUtils.isSensitiveHeaderKey("Accept"));
        assertFalse(MaskingUtils.isSensitiveHeaderKey("X-User-Id"));
        assertFalse(MaskingUtils.isSensitiveHeaderKey("User-Agent"));
    }

    @Test
    public void isSensitiveHeaderKey_null_and_empty_should_return_false() {
        assertFalse(MaskingUtils.isSensitiveHeaderKey(null));
        assertFalse(MaskingUtils.isSensitiveHeaderKey(""));
        assertFalse(MaskingUtils.isSensitiveHeaderKey("   "));
    }
}
