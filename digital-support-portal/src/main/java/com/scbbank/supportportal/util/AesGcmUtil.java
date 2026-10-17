package com.scbbank.supportportal.util;

import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

@Component
public class AesGcmUtil {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public AesGcmUtil(SecretKey secretKey) {
        this.secretKey = Objects.requireNonNull(secretKey);
    }

    public String encrypt(String plaintext) {
        Objects.requireNonNull(plaintext, "plaintext is required");

        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    secretKey,
                    new GCMParameterSpec(TAG_LENGTH_BITS, iv)
            );

            byte[] ciphertext = cipher.doFinal(
                    plaintext.getBytes(StandardCharsets.UTF_8)
            );

            // Format: Base64(12-byte IV || ciphertext || authentication tag)
            byte[] combined = new byte[iv.length + ciphertext.length];

            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(
                    ciphertext, 0, combined, iv.length, ciphertext.length
            );

            return Base64.getEncoder().encodeToString(combined);

        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("AES encryption failed", e);
        }
    }

    public String decrypt(String encryptedValue) {
        Objects.requireNonNull(encryptedValue, "encryptedValue is required");

        try {
            byte[] combined = Base64.getDecoder().decode(encryptedValue);

            if (combined.length < IV_LENGTH + 16) {
                throw new IllegalArgumentException("Invalid encrypted payload");
            }

            byte[] iv = new byte[IV_LENGTH];
            byte[] ciphertext = new byte[combined.length - IV_LENGTH];

            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            System.arraycopy(
                    combined, IV_LENGTH,
                    ciphertext, 0, ciphertext.length
            );

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    secretKey,
                    new GCMParameterSpec(TAG_LENGTH_BITS, iv)
            );

            return new String(
                    cipher.doFinal(ciphertext),
                    StandardCharsets.UTF_8
            );

        } catch (IllegalArgumentException | GeneralSecurityException e) {
            throw new IllegalArgumentException(
                    "Invalid or corrupted encrypted payload"
            );
        }
    }
}
