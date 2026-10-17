package com.scbbank.supportportal.util;

import com.scbbank.supportportal.constants.ApplicationConstants;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

@Component
public class AesGcmUtil {

    private final SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public AesGcmUtil(SecretKey secretKey) {
        this.secretKey = Objects.requireNonNull(
                secretKey,
                "AES secret key is required"
        );
    }

    public String encrypt(String plaintext) {

        if (plaintext == null) {
            throw new IllegalArgumentException(
                    "Plaintext is required"
            );
        }

        try {
            byte[] iv = new byte[
                    ApplicationConstants.AES_IV_LENGTH_BYTES
                    ];

            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(
                    ApplicationConstants.AES_GCM_TRANSFORMATION
            );

            GCMParameterSpec spec = new GCMParameterSpec(
                    ApplicationConstants.AES_TAG_LENGTH_BITS,
                    iv
            );

            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    secretKey,
                    spec
            );

            byte[] encrypted = cipher.doFinal(
                    plaintext.getBytes(StandardCharsets.UTF_8)
            );

            byte[] combined = ByteBuffer.allocate(
                            iv.length + encrypted.length
                    )
                    .put(iv)
                    .put(encrypted)
                    .array();

            return Base64.getEncoder()
                    .encodeToString(combined);

        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(
                    "AES encryption failed", e
            );
        }
    }

    public String decrypt(String encryptedBase64) {

        if (encryptedBase64 == null ||
                encryptedBase64.isBlank()) {
            throw new IllegalArgumentException(
                    "Encrypted data is required"
            );
        }

        try {
            byte[] combined = Base64.getDecoder()
                    .decode(encryptedBase64);

            int ivLength =
                    ApplicationConstants.AES_IV_LENGTH_BYTES;

            int tagLength =
                    ApplicationConstants.AES_TAG_LENGTH_BITS / 8;

            if (combined.length < ivLength + tagLength) {
                throw new IllegalArgumentException(
                        "Invalid encrypted data"
                );
            }

            ByteBuffer buffer = ByteBuffer.wrap(combined);

            byte[] iv = new byte[ivLength];
            buffer.get(iv);

            byte[] ciphertext =
                    new byte[buffer.remaining()];

            buffer.get(ciphertext);

            Cipher cipher = Cipher.getInstance(
                    ApplicationConstants.AES_GCM_TRANSFORMATION
            );

            GCMParameterSpec spec = new GCMParameterSpec(
                    ApplicationConstants.AES_TAG_LENGTH_BITS,
                    iv
            );

            cipher.init(
                    Cipher.DECRYPT_MODE,
                    secretKey,
                    spec
            );

            byte[] plaintext = cipher.doFinal(ciphertext);

            return new String(
                    plaintext,
                    StandardCharsets.UTF_8
            );

        } catch (GeneralSecurityException |
                 IllegalArgumentException e) {

            throw new IllegalArgumentException(
                    "AES decryption failed", e
            );
        }
    }
}
