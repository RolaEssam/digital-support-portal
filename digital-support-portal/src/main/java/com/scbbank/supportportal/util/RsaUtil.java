package com.scbbank.supportportal.util;

import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Objects;

@Component
public class RsaUtil {

    private static final int KEY_SIZE = 2048;

    private static final OAEPParameterSpec OAEP_PARAMS =
            new OAEPParameterSpec(
                    "SHA-256",
                    "MGF1",
                    MGF1ParameterSpec.SHA256,
                    PSource.PSpecified.DEFAULT
            );

    public KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator =
                    KeyPairGenerator.getInstance("RSA");

            generator.initialize(KEY_SIZE, new SecureRandom());

            return generator.generateKeyPair();

        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(
                    "RSA key generation failed", e
            );
        }
    }

    public String decrypt(
            String encryptedBase64,
            PrivateKey privateKey) {

        Objects.requireNonNull(
                encryptedBase64, "Encrypted value is required"
        );
        Objects.requireNonNull(
                privateKey, "Private key is required"
        );

        try {
            byte[] encryptedBytes =
                    Base64.getDecoder().decode(encryptedBase64);

            Cipher cipher = Cipher.getInstance(
                    "RSA/ECB/OAEPPadding"
            );

            cipher.init(
                    Cipher.DECRYPT_MODE,
                    privateKey,
                    OAEP_PARAMS
            );

            byte[] plaintext = cipher.doFinal(encryptedBytes);

            return new String(
                    plaintext,
                    StandardCharsets.UTF_8
            );

        } catch (IllegalArgumentException | GeneralSecurityException e) {
            throw new IllegalArgumentException(
                    "RSA decryption failed"
            );
        }
    }


    public PrivateKey decodePrivateKey(String encodedPrivateKey) {

        Objects.requireNonNull(
                encodedPrivateKey,
                "Encoded private key is required"
        );

        try {
            byte[] keyBytes = Base64.getDecoder()
                    .decode(encodedPrivateKey);

            PKCS8EncodedKeySpec keySpec =
                    new PKCS8EncodedKeySpec(keyBytes);

            KeyFactory keyFactory =
                    KeyFactory.getInstance("RSA");

            return keyFactory.generatePrivate(keySpec);

        } catch (IllegalArgumentException |
                 GeneralSecurityException e) {

            throw new IllegalArgumentException(
                    "Invalid RSA private key"
            );
        }
    }

}
