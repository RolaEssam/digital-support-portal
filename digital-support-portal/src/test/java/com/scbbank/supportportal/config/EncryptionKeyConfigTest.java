package com.scbbank.supportportal.config;

import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class EncryptionKeyConfigTest {

    private final EncryptionKeyConfig config =
            new EncryptionKeyConfig();

    @Test
    void shouldAcceptValidAes256Key() {
        byte[] keyBytes = new byte[32];

        // Deterministic test fixture, NOT a real encryption key.
        for (int i = 0; i < keyBytes.length; i++) {
            keyBytes[i] = (byte) (i + 1);
        }

        String encodedKey = Base64.getEncoder()
                .encodeToString(keyBytes);

        SecretKey secretKey = config.aesSecretKey(encodedKey);

        assertNotNull(secretKey);
        assertEquals("AES", secretKey.getAlgorithm());
        assertArrayEquals(keyBytes, secretKey.getEncoded());
    }

    @Test
    void shouldRejectMissingKey() {
        assertThrows(
                IllegalStateException.class,
                () -> config.aesSecretKey(null)
        );

        assertThrows(
                IllegalStateException.class,
                () -> config.aesSecretKey("")
        );

        assertThrows(
                IllegalStateException.class,
                () -> config.aesSecretKey("   ")
        );
    }

    @Test
    void shouldRejectInvalidBase64() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> config.aesSecretKey("not-valid-Base64!!!")
        );

        assertEquals(
                "AES encryption key must be valid Base64",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectIncorrectKeyLength() {
        byte[] shortKey = new byte[16];

        String encodedKey = Base64.getEncoder()
                .encodeToString(shortKey);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> config.aesSecretKey(encodedKey)
        );

        assertEquals(
                "AES encryption key must be 32 bytes",
                exception.getMessage()
        );
    }
}
