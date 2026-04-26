package com.novelverse.app.domain;

import com.novelverse.app.domain.utils.InputValidator;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link InputValidator}.
 * These run on the JVM — no Android runtime needed.
 */
public class InputValidatorTest {

    // ── Email validation ──────────────────────────────────────────────────────

    @Test
    public void validEmail_returnsTrue() {
        assertTrue(InputValidator.isValidEmail("user@example.com"));
        assertTrue(InputValidator.isValidEmail("name+tag@sub.domain.org"));
    }

    @Test
    public void invalidEmail_returnsFalse() {
        assertFalse(InputValidator.isValidEmail(""));
        assertFalse(InputValidator.isValidEmail("notanemail"));
        assertFalse(InputValidator.isValidEmail("missing@"));
        assertFalse(InputValidator.isValidEmail("@nodomain.com"));
    }

    @Test
    public void sanitizeEmail_trimsAndLowercases() {
        assertEquals("user@example.com", InputValidator.sanitizeEmail("  User@Example.COM  "));
    }

    @Test
    public void sanitizeEmail_nullInput_returnsEmpty() {
        assertEquals("", InputValidator.sanitizeEmail(null));
    }

    // ── Password validation ───────────────────────────────────────────────────

    @Test
    public void validPassword_atLeast8Chars_returnsTrue() {
        assertTrue(InputValidator.isValidPassword("password"));
        assertTrue(InputValidator.isValidPassword("Str0ng!Pass"));
    }

    @Test
    public void shortPassword_returnsFalse() {
        assertFalse(InputValidator.isValidPassword("1234567")); // 7 chars — boundary
        assertFalse(InputValidator.isValidPassword(""));
        assertFalse(InputValidator.isValidPassword(null));
    }

    // ── Username validation ───────────────────────────────────────────────────

    @Test
    public void validUsername_inRange_returnsTrue() {
        assertTrue(InputValidator.isValidUsername("abc"));         // min 3
        assertTrue(InputValidator.isValidUsername("exactly20char12345")); // 18 chars
    }

    @Test
    public void invalidUsername_tooShort_returnsFalse() {
        assertFalse(InputValidator.isValidUsername("ab")); // 2 chars
        assertFalse(InputValidator.isValidUsername(""));
        assertFalse(InputValidator.isValidUsername(null));
    }

    @Test
    public void invalidUsername_tooLong_returnsFalse() {
        assertFalse(InputValidator.isValidUsername("thisusernameiswaytoolong123")); // >20
    }

    @Test
    public void sanitizeUsername_trimsWhitespace() {
        assertEquals("hello", InputValidator.sanitizeUsername("  hello  "));
    }

    @Test
    public void sanitizeUsername_nullInput_returnsEmpty() {
        assertEquals("", InputValidator.sanitizeUsername(null));
    }
}
