package com.appointmentbooking.util;

import com.appointmentbooking.config.AppProperties;
import com.appointmentbooking.exception.EncryptionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for AES-256-CBC EncryptionService.
 * No Spring context needed — direct instantiation with test properties.
 */
@DisplayName("EncryptionService — AES-256-CBC")
class EncryptionServiceTest {

    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties();
        AppProperties.Encryption enc = new AppProperties.Encryption();
        enc.setKey("TestEncryptionKey32CharsLong!");     // 30 chars — padded to 32 bytes internally
        props.setEncryption(enc);
        encryptionService = new EncryptionService(props);
    }

    // ── encrypt() ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("encrypt returns non-empty string")
    void encryptReturnsString() {
        String result = encryptionService.encrypt("8001015009087");
        assertThat(result).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("encrypt returns null for null input")
    void encryptNullReturnsNull() {
        assertThat(encryptionService.encrypt(null)).isNull();
    }

    @Test
    @DisplayName("encrypt returns null for blank input")
    void encryptBlankReturnsNull() {
        assertThat(encryptionService.encrypt("   ")).isNull();
    }

    @Test
    @DisplayName("encrypt output contains IV separator ':'")
    void encryptContainsSeparator() {
        String result = encryptionService.encrypt("test");
        assertThat(result).contains(":");
    }

    @Test
    @DisplayName("encrypt produces different ciphertext each call (random IV)")
    void encryptIsNonDeterministic() {
        String a = encryptionService.encrypt("8001015009087");
        String b = encryptionService.encrypt("8001015009087");
        assertThat(a).isNotEqualTo(b);
    }

    // ── decrypt() ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("decrypt correctly recovers the original plaintext")
    void decryptRoundTrip() {
        String plaintext  = "8001015009087";
        String encrypted  = encryptionService.encrypt(plaintext);
        String decrypted  = encryptionService.decrypt(encrypted);
        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    @DisplayName("decrypt returns null for null input")
    void decryptNullReturnsNull() {
        assertThat(encryptionService.decrypt(null)).isNull();
    }

    @Test
    @DisplayName("decrypt returns null for blank input")
    void decryptBlankReturnsNull() {
        assertThat(encryptionService.decrypt("")).isNull();
    }

    @Test
    @DisplayName("decrypt throws EncryptionException for malformed input (no separator)")
    void decryptMalformedThrows() {
        assertThatThrownBy(() -> encryptionService.decrypt("notvalidformat"))
                .isInstanceOf(EncryptionException.class);
    }

    @Test
    @DisplayName("round-trip works for various plaintext values")
    void roundTripVariousValues() {
        String[] values = {"hello world", "12345", "Special!@#$%^&*()", "south africa"};
        for (String v : values) {
            assertThat(encryptionService.decrypt(encryptionService.encrypt(v)))
                    .as("round-trip for: " + v)
                    .isEqualTo(v);
        }
    }
}
