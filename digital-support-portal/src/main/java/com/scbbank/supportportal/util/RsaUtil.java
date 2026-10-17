package com.scbbank.supportportal.util;

import com.scbbank.supportportal.constants.ApplicationConstants;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Objects;

@Component
public class RsaUtil {

    private static final OAEPParameterSpec OAEP_SPEC =
            new OAEPParameterSpec(
                    "SHA-256",
                    "MGF1",
                    MGF1ParameterSpec.SHA256,
                    PSource.PSpecified.DEFAULT
            );

    public KeyPair generateKeyPair() {

        try {
            KeyPairGenerator generator =
                    KeyPairGenerator.getInstance(
                            ApplicationConstants.RSA_ALGORITHM
                    );

            generator.initialize(
                    ApplicationConstants.RSA_KEY_SIZE
            );

            return generator.generateKeyPair();

        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(
                    "Failed to generate RSA key pair", e
            );
        }
    }

    public PrivateKey decodePrivateKey(String privateKeyBase64) {

        if (privateKeyBase64 == null ||
                privateKeyBase64.isBlank()) {
            throw new IllegalArgumentException(
                    "Private key is required"
            );
        }

        try {
            byte[] privateKeyBytes = Base64.getDecoder()
                    .decode(privateKeyBase64);

            PKCS8EncodedKeySpec keySpec =
                    new PKCS8EncodedKeySpec(privateKeyBytes);

            return KeyFactory.getInstance(
                    ApplicationConstants.RSA_ALGORITHM
            ).generatePrivate(keySpec);

        } catch (GeneralSecurityException |
                 IllegalArgumentException e) {

            throw new IllegalArgumentException(
                    "Invalid RSA private key", e
            );
        }
    }

    public byte[] decrypt(
            byte[] encryptedData,
            PrivateKey privateKey) {

        Objects.requireNonNull(
                encryptedData,
                "Encrypted data is required"
        );

        Objects.requireNonNull(
                privateKey,
                "Private key is required"
        );

        try {
            Cipher cipher = Cipher.getInstance(
                    ApplicationConstants.RSA_OAEP_TRANSFORMATION
            );

            cipher.init(
                    Cipher.DECRYPT_MODE,
                    privateKey,
                    OAEP_SPEC
            );

            return cipher.doFinal(encryptedData);

        } catch (GeneralSecurityException e) {

            throw new IllegalArgumentException(
                    "RSA decryption failed", e
            );
        }
    }

    public String decrypt(
            String encryptedDataBase64,
            PrivateKey privateKey) {

        if (encryptedDataBase64 == null ||
                encryptedDataBase64.isBlank()) {
            throw new IllegalArgumentException(
                    "Encrypted data is required"
            );
        }

        byte[] encryptedData;

        try {
            encryptedData = Base64.getDecoder()
                    .decode(encryptedDataBase64);

        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid RSA ciphertext", e
            );
        }

        byte[] plaintext = decrypt(
                encryptedData,
                privateKey
        );

        return new String(
                plaintext,
                StandardCharsets.UTF_8
        );
    }
}
