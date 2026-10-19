package com.scbbank.supportportal.util;

import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class AesGcmUtilTest {

    private final SecretKey testKey = new SecretKeySpec(
            new byte[32], "AES"
    );

    private final AesGcmUtil aesGcmUtil = new AesGcmUtil(testKey);

    @Test
    void shouldEncryptAndDecryptSuccessfully() {
        String plaintext = "test-rsa-private-key";

        String encrypted = aesGcmUtil.encrypt(plaintext);
        String decrypted = aesGcmUtil.decrypt(encrypted);

        assertNotEquals(plaintext, encrypted);
        assertEquals(plaintext, decrypted);
    }

    @Test
    void shouldProduceDifferentCiphertexts() {
        String plaintext = "same-private-key";

        String first = aesGcmUtil.encrypt(plaintext);
        String second = aesGcmUtil.encrypt(plaintext);

        assertNotEquals(first, second);
    }

    @Test
    void shouldRejectTamperedCiphertext() {
        String encrypted = aesGcmUtil.encrypt("sensitive-data");

        byte[] bytes = Base64.getDecoder().decode(encrypted);
        bytes[bytes.length - 1] ^= 1;

        String tampered = Base64.getEncoder().encodeToString(bytes);

        assertThrows(
                IllegalArgumentException.class,
                () -> aesGcmUtil.decrypt(tampered)
        );
    }

    @Test
    void shouldRejectInvalidPayload() {
        assertThrows(
                IllegalArgumentException.class,
                () -> aesGcmUtil.decrypt("not-base64!!!")
        );
    }
}
