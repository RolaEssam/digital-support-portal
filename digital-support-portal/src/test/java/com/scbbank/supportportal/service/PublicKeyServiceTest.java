
package com.scbbank.supportportal.service;

import com.scbbank.supportportal.util.AesGcmUtil;
import com.scbbank.supportportal.util.RedisUtil;
import com.scbbank.supportportal.util.RsaUtil;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
        "auth.encryption.key=AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8="
})
@ActiveProfiles("local")
class PublicKeyServiceTest {

    @Autowired
    private PublicKeyService publicKeyService;

    @Autowired
    private AesGcmUtil aesGcmUtil;

    @Autowired
    private RsaUtil rsaUtil;

    @Autowired
    private RedisUtil redisUtil;

    private String privateKeyRedisKey(UUID requestId) {
        return "auth:login-key:" + requestId;
    }

    @Test
    void shouldGeneratePublicKeyAndStoreEncryptedPrivateKey()
            throws Exception {

        UUID requestId = UUID.randomUUID();

        try {
            String publicKeyBase64 =
                    publicKeyService.generatePublicKey(requestId);

            assertNotNull(publicKeyBase64);

            PublicKey publicKey = KeyFactory.getInstance("RSA")
                    .generatePublic(
                            new X509EncodedKeySpec(
                                    Base64.getDecoder().decode(publicKeyBase64)
                            )
                    );

            assertEquals(
                    2048,
                    ((RSAPublicKey) publicKey).getModulus().bitLength()
            );

            String encryptedPrivateKey =
                    redisUtil.get(privateKeyRedisKey(requestId));

            assertNotNull(encryptedPrivateKey);

            String privateKeyBase64 =
                    aesGcmUtil.decrypt(encryptedPrivateKey);

            PrivateKey privateKey =
                    rsaUtil.decodePrivateKey(privateKeyBase64);

            assertNotNull(privateKey);

            assertEquals(
                    ((RSAPublicKey) publicKey).getModulus(),
                    ((RSAPrivateKey) privateKey).getModulus()
            );

            Long ttl = redisUtil.getExpire(
                    privateKeyRedisKey(requestId)
            );

            assertNotNull(ttl);
            assertTrue(ttl > 0 && ttl <= 300);

        } finally {
            redisUtil.delete(privateKeyRedisKey(requestId));
        }
    }

    @Test
    void shouldRejectDuplicateRequestId() {

        UUID requestId = UUID.randomUUID();

        try {
            String firstPublicKey =
                    publicKeyService.generatePublicKey(requestId);

            IllegalStateException exception = assertThrows(
                    IllegalStateException.class,
                    () -> publicKeyService.generatePublicKey(requestId)
            );

            assertEquals(
                    "Duplicate request ID",
                    exception.getMessage()
            );

            assertNotNull(firstPublicKey);

            // The original private key must not be overwritten.
            assertNotNull(
                    redisUtil.get(privateKeyRedisKey(requestId))
            );

        } finally {
            redisUtil.delete(privateKeyRedisKey(requestId));
        }
    }

    @Test
    void shouldRejectMissingRequestId() {

        assertThrows(
                NullPointerException.class,
                () -> publicKeyService.generatePublicKey(null)
        );
    }

    @Test
    void shouldNotReturnPublicKeyWhenRedisStorageFails() {

        RsaUtil rsa = new RsaUtil();

        javax.crypto.SecretKey testKey =
                new javax.crypto.spec.SecretKeySpec(
                        new byte[32],
                        "AES"
                );

        AesGcmUtil aes = new AesGcmUtil(testKey);

        LoginKeyCacheService failingCache =
                mock(LoginKeyCacheService.class);

        when(failingCache.store(any(UUID.class), anyString()))
                .thenThrow(
                        new IllegalStateException(
                                "Redis storage unavailable"
                        )
                );

        PublicKeyService service = new PublicKeyService(
                rsa,
                aes,
                failingCache
        );

        assertThrows(
                IllegalStateException.class,
                () -> service.generatePublicKey(UUID.randomUUID())
        );

        verify(failingCache, times(1))
                .store(any(UUID.class), anyString());
    }
}
