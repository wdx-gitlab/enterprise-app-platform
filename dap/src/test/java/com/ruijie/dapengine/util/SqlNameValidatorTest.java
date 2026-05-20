package com.ruijie.dapengine.util;

import com.ruijie.dapengine.common.exception.DapValidationException;
import com.ruijie.dapengine.common.util.SqlNameValidator;
import org.junit.Test;

import static org.junit.Assert.*;

public class SqlNameValidatorTest {

    @Test
    public void valid_names_should_pass() {
        SqlNameValidator.validate("credit_code");
        SqlNameValidator.validate("parent_code");
        SqlNameValidator.validate("dap_customer");
        SqlNameValidator.validate("a");
        SqlNameValidator.validate("a1");
    }

    @Test(expected = DapValidationException.class)
    public void name_starting_with_uppercase_should_fail() {
        SqlNameValidator.validate("CreditCode");
    }

    @Test(expected = DapValidationException.class)
    public void name_starting_with_digit_should_fail() {
        SqlNameValidator.validate("1field");
    }

    @Test(expected = DapValidationException.class)
    public void name_with_semicolon_should_fail() {
        SqlNameValidator.validate("; DROP TABLE dap_sys_subject");
    }

    @Test(expected = DapValidationException.class)
    public void name_with_dash_should_fail() {
        SqlNameValidator.validate("field-name");
    }

    @Test(expected = DapValidationException.class)
    public void name_exceeding_64_chars_should_fail() {
        // 65-char name: 1 'a' + 64 'b' chars
        StringBuilder sb = new StringBuilder("a");
        for (int i = 0; i < 64; i++) {
            sb.append('b');
        }
        SqlNameValidator.validate(sb.toString());
    }

    @Test(expected = DapValidationException.class)
    public void null_name_should_fail() {
        SqlNameValidator.validate(null);
    }

    @Test(expected = DapValidationException.class)
    public void empty_name_should_fail() {
        SqlNameValidator.validate("");
    }

    @Test
    public void isValid_returns_false_for_invalid_names() {
        assertFalse(SqlNameValidator.isValid("BadName"));
        assertFalse(SqlNameValidator.isValid(null));
        assertFalse(SqlNameValidator.isValid(""));
    }

    @Test
    public void isValid_returns_true_for_valid_names() {
        assertTrue(SqlNameValidator.isValid("valid_name"));
        assertTrue(SqlNameValidator.isValid("a1b2c3"));
    }
}
