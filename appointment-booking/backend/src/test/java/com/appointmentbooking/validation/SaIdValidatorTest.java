package com.appointmentbooking.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the South African ID Luhn algorithm.
 * No Spring context required — pure logic test.
 */
@DisplayName("SA ID Validator")
class SaIdValidatorTest {

    private final SaIdValidator validator = new SaIdValidator();

    // ── Valid IDs ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("returns true for null (field is optional)")
    void nullIsValid() {
        assertThat(validator.isValid(null, null)).isTrue();
    }

    @Test
    @DisplayName("returns true for blank string (field is optional)")
    void blankIsValid() {
        assertThat(validator.isValid("  ", null)).isTrue();
    }

    @ParameterizedTest(name = "valid SA ID: {0}")
    @ValueSource(strings = {"8001015009087", "9001010000908"})
    @DisplayName("returns true for Luhn-correct 13-digit IDs")
    void validIds(String id) {
        assertThat(validator.isValid(id, null)).isTrue();
    }

    // ── Invalid IDs ───────────────────────────────────────────────────────────

    @ParameterizedTest(name = "too short/long: {0}")
    @ValueSource(strings = {"123456789012", "12345678901234"})
    @DisplayName("returns false when not exactly 13 digits")
    void wrongLength(String id) {
        assertThat(validator.isValid(id, null)).isFalse();
    }

    @ParameterizedTest(name = "non-numeric: {0}")
    @ValueSource(strings = {"8001015009O87", "800101500908!", "abcdefghijklm"})
    @DisplayName("returns false for IDs containing non-numeric characters")
    void nonNumeric(String id) {
        assertThat(validator.isValid(id, null)).isFalse();
    }

    @ParameterizedTest(name = "bad Luhn: {0}")
    @ValueSource(strings = {"8001015009083", "1111111111119"})
    @DisplayName("returns false when Luhn checksum fails")
    void failsLuhn(String id) {
        assertThat(validator.isValid(id, null)).isFalse();
    }

    // ── Luhn static method ────────────────────────────────────────────────────

    @Test
    @DisplayName("passesLuhn() returns true for known-good value")
    void passesLuhnKnownGood() {
        assertThat(SaIdValidator.passesLuhn("8001015009087")).isTrue();
    }

    @Test
    @DisplayName("passesLuhn() returns false for tampered check digit")
    void passesLuhnTampered() {
        assertThat(SaIdValidator.passesLuhn("8001015009083")).isFalse();
    }
}
