package com.scbbank.supportportal.util;

import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.spec.MGF1ParameterSpec;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class RsaUtilTest {

    private final RsaUtil rsaUtil = new RsaUtil();

    private static final OAEPParameterSpec OAEP_PARAMS =
            new OAEPParameterSpec(
                    "SHA-256",
                    "MGF1",
                    MGF1ParameterSpec.SHA256,
                    PSource.PSpecified.DEFAULT
            );

    @Test
    void shouldGenerate2048BitKeyPair() {
        KeyPair keyPair = rsaUtil.generateKeyPair();

        assertNotNull(keyPair.getPublic());
        assertNotNull(keyPair.getPrivate());

        java.security.interfaces.RSAPublicKey publicKey =
                (java.security.interfaces.RSAPublicKey)
                        keyPair.getPublic();

        assertEquals(
                2048,
                publicKey.getModulus().bitLength()
        );
    }

    @Test
    void shouldDecryptEncryptedCredentials() throws Exception {
        KeyPair keyPair = rsaUtil.generateKeyPair();
        String original = "test-password-123";

        Cipher cipher = Cipher.getInstance(
                "RSA/ECB/OAEPPadding"
        );

        cipher.init(
                Cipher.ENCRYPT_MODE,
                keyPair.getPublic(),
                OAEP_PARAMS
        );

        byte[] encrypted = cipher.doFinal(
                original.getBytes(StandardCharsets.UTF_8)
        );

        String encryptedBase64 =
                Base64.getEncoder().encodeToString(encrypted);

        String decrypted = rsaUtil.decrypt(
                encryptedBase64,
                keyPair.getPrivate()
        );

        assertEquals(original, decrypted);
    }

    @Test
    void shouldRejectWrongPrivateKey() throws Exception {
        KeyPair correctPair = rsaUtil.generateKeyPair();
        KeyPair wrongPair = rsaUtil.generateKeyPair();

        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPPadding");
        cipher.init(
                Cipher.ENCRYPT_MODE,
                correctPair.getPublic(),
                OAEP_PARAMS
        );

        String encryptedBase64 = Base64.getEncoder()
                .encodeToString(
                        cipher.doFinal(
                                "secret".getBytes(StandardCharsets.UTF_8)
                        )
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> rsaUtil.decrypt(
                        encryptedBase64,
                        wrongPair.getPrivate()
                )
        );
    }

    @Test
    void shouldRejectInvalidCiphertext() {
        KeyPair keyPair = rsaUtil.generateKeyPair();

        assertThrows(
                IllegalArgumentException.class,
                () -> rsaUtil.decrypt(
                        "invalid-base64!!!",
                        keyPair.getPrivate()
                )
        );
    }
}
