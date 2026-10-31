package com.scbbank.supportportal.util;

import com.scbbank.supportportal.service.LoginKeyCacheService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.spec.MGF1ParameterSpec;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "auth.encryption.key=AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8=",
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("local")

class CryptoIntegrationTest {

    @Autowired
    private RsaUtil rsaUtil;

    @Autowired
    private AesGcmUtil aesGcmUtil;

    @Autowired
    private LoginKeyCacheService loginKeyCacheService;

    @Test
    void shouldEncryptStoreConsumeAndDecryptCredentials()
            throws Exception {

        UUID requestId = UUID.randomUUID();

        KeyPair keyPair = rsaUtil.generateKeyPair();

        String privateKeyBase64 = Base64.getEncoder()
                .encodeToString(
                        keyPair.getPrivate().getEncoded()
                );

        String encryptedPrivateKey =
                aesGcmUtil.encrypt(privateKeyBase64);

        assertTrue(
                loginKeyCacheService.store(
                        requestId,
                        encryptedPrivateKey
                )
        );

        OAEPParameterSpec params = new OAEPParameterSpec(
                "SHA-256",
                "MGF1",
                MGF1ParameterSpec.SHA256,
                PSource.PSpecified.DEFAULT
        );

        Cipher frontendCipher =
                Cipher.getInstance("RSA/ECB/OAEPPadding");

        frontendCipher.init(
                Cipher.ENCRYPT_MODE,
                keyPair.getPublic(),
                params
        );

        String encryptedCredential = Base64.getEncoder()
                .encodeToString(
                        frontendCipher.doFinal(
                                "test-password".getBytes(
                                        StandardCharsets.UTF_8
                                )
                        )
                );

        String storedValue =
                loginKeyCacheService.consume(requestId);

        assertNotNull(storedValue);

        String decodedPrivateKeyBase64 =
                aesGcmUtil.decrypt(storedValue);

        PrivateKey restoredPrivateKey =
                rsaUtil.decodePrivateKey(
                        decodedPrivateKeyBase64
                );

        String decryptedCredential =
                rsaUtil.decrypt(
                        encryptedCredential,
                        restoredPrivateKey
                );

        assertEquals(
                "test-password",
                decryptedCredential
        );

        assertNull(
                loginKeyCacheService.consume(requestId)
        );
    }
}
