package com.appointmentbooking.util;

import com.appointmentbooking.config.AppProperties;
import com.appointmentbooking.exception.EncryptionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-CBC symmetric encryption service.
 *
 * Security design:
 *  - A fresh random 16-byte IV is generated for every encryption call.
 *    The same plaintext produces a different ciphertext each time,
 *    preventing frequency analysis.
 *  - The IV is prepended (hex-encoded) to the stored value, separated by ":".
 *    Format: "ivHex:base64Ciphertext"
 *  - The key is sourced from {@code app.encryption.key} and trimmed to 32 bytes.
 *    Validated at startup via {@link AppProperties}.
 *  - Uses JDK's built-in javax.crypto — no third-party crypto library needed.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EncryptionService {

    private static final String ALGORITHM     = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final int    IV_LENGTH      = 16;
    private static final int    KEY_LENGTH     = 32; // 256-bit

    private final AppProperties appProperties;
    private final SecureRandom  secureRandom = new SecureRandom();

    /**
     * Encrypt a plaintext string.
     *
     * @param plaintext the value to encrypt (e.g. a SA ID number)
     * @return "ivHex:base64Ciphertext", or {@code null} if input is null/blank
     */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            return null;
        }
        try {
            byte[] iv  = generateIv();
            byte[] key = getKey();

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE,
                        new SecretKeySpec(key, ALGORITHM),
                        new IvParameterSpec(iv));

            byte[] encrypted  = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            String ivHex      = bytesToHex(iv);
            String cipherB64  = Base64.getEncoder().encodeToString(encrypted);

            return ivHex + ":" + cipherB64;

        } catch (Exception e) {
            log.error("Encryption failed", e);
            throw new EncryptionException("Failed to encrypt value");
        }
    }

    /**
     * Decrypt a value produced by {@link #encrypt}.
     *
     * @param encryptedValue "ivHex:base64Ciphertext"
     * @return original plaintext, or {@code null} if input is null/blank
     */
    public String decrypt(String encryptedValue) {
        if (encryptedValue == null || encryptedValue.isBlank()) {
            return null;
        }
        try {
            String[] parts = encryptedValue.split(":", 2);
            if (parts.length != 2) {
                throw new EncryptionException("Invalid encrypted value format");
            }

            byte[] iv         = hexToBytes(parts[0]);
            byte[] ciphertext = Base64.getDecoder().decode(parts[1]);
            byte[] key        = getKey();

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE,
                        new SecretKeySpec(key, ALGORITHM),
                        new IvParameterSpec(iv));

            byte[] decrypted = cipher.doFinal(ciphertext);
            return new String(decrypted, StandardCharsets.UTF_8);

        } catch (EncryptionException e) {
            throw e;
        } catch (Exception e) {
            log.error("Decryption failed", e);
            throw new EncryptionException("Failed to decrypt value");
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private byte[] getKey() {
        String keyStr = appProperties.getEncryption().getKey();
        byte[] keyBytes = keyStr.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[KEY_LENGTH];
        System.arraycopy(keyBytes, 0, result, 0, Math.min(keyBytes.length, KEY_LENGTH));
        return result;
    }

    private byte[] generateIv() {
        byte[] iv = new byte[IV_LENGTH];
        secureRandom.nextBytes(iv);
        return iv;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                                 + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
