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
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "auth.encryption.key=AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8="
})
@ActiveProfiles("local")
class PublicKeyServiceTest {

    @Autowired
    private PublicKeyService publicKeyService;

    @Autowired
    private LoginKeyCacheService loginKeyCacheService;

    @Autowired
    private AesGcmUtil aesGcmUtil;

    @Autowired
    private RsaUtil rsaUtil;

    @Autowired
    private RedisUtil redisUtil;

    private String privateKeyRedisKey(UUID requestId) {
        return "auth:login-key:" + requestId;
    }

    private String rateLimitRedisKey(String clientId) {
        return "auth:public-key:rate-limit:" + clientId;
    }

    @Test
    void shouldGeneratePublicKeyAndStoreEncryptedPrivateKey()
            throws Exception {

        UUID requestId = UUID.randomUUID();
        String clientId = UUID.randomUUID().toString();

        try {
            String publicKeyBase64 =
                    publicKeyService.generatePublicKey(
                            requestId, clientId
                    );

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

            // Verify public/private keys belong to the same RSA pair.
            java.security.interfaces.RSAPrivateKey rsaPrivateKey =
                    (java.security.interfaces.RSAPrivateKey) privateKey;

            assertEquals(
                    ((RSAPublicKey) publicKey).getModulus(),
                    rsaPrivateKey.getModulus()
            );

            Long ttl = redisUtil.getExpire(
                    privateKeyRedisKey(requestId)
            );

            assertNotNull(ttl);
            assertTrue(ttl > 0 && ttl <= 300);

        } finally {
            redisUtil.delete(privateKeyRedisKey(requestId));
            redisUtil.delete(rateLimitRedisKey(clientId));
        }
    }

    @Test
    void shouldRejectDuplicateRequestId() {

        UUID requestId = UUID.randomUUID();
        String clientId = UUID.randomUUID().toString();

        try {
            publicKeyService.generatePublicKey(requestId, clientId);

            IllegalStateException exception = assertThrows(
                    IllegalStateException.class,
                    () -> publicKeyService.generatePublicKey(
                            requestId, clientId
                    )
            );

            assertEquals(
                    "Duplicate request ID",
                    exception.getMessage()
            );

        } finally {
            redisUtil.delete(privateKeyRedisKey(requestId));
            redisUtil.delete(rateLimitRedisKey(clientId));
        }
    }

    @Test
    void shouldRejectRequestWhenRateLimitExceeded() {

        UUID requestId = UUID.randomUUID();
        String clientId = UUID.randomUUID().toString();

        try {
            redisUtil.set(
                    rateLimitRedisKey(clientId),
                    "20",
                    java.time.Duration.ofSeconds(60)
            );

            IllegalStateException exception = assertThrows(
                    IllegalStateException.class,
                    () -> publicKeyService.generatePublicKey(
                            requestId, clientId
                    )
            );

            assertEquals(
                    "Public key request rate limit exceeded",
                    exception.getMessage()
            );

            assertNull(
                    redisUtil.get(privateKeyRedisKey(requestId))
            );

        } finally {
            redisUtil.delete(privateKeyRedisKey(requestId));
            redisUtil.delete(rateLimitRedisKey(clientId));
        }
    }

    @Test
    void shouldRejectMissingRequestId() {

        String clientId = UUID.randomUUID().toString();

        try {
            assertThrows(
                    NullPointerException.class,
                    () -> publicKeyService.generatePublicKey(
                            null, clientId
                    )
            );
        } finally {
            redisUtil.delete(rateLimitRedisKey(clientId));
        }
    }

    @Test
    void shouldRejectMissingClientIdentifier() {

        UUID requestId = UUID.randomUUID();

        assertThrows(
                IllegalArgumentException.class,
                () -> publicKeyService.generatePublicKey(
                        requestId, ""
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> publicKeyService.generatePublicKey(
                        requestId, null
                )
        );
    }


    @Test
    void shouldNotReturnPublicKeyWhenRedisStorageFails() {

        RsaUtil rsaUtil = new RsaUtil();

        javax.crypto.SecretKey testKey =
                new javax.crypto.spec.SecretKeySpec(
                        new byte[32], "AES"
                );

        AesGcmUtil aesGcmUtil = new AesGcmUtil(testKey);

        LoginKeyCacheService failingCache =
                org.mockito.Mockito.mock(LoginKeyCacheService.class);

        PublicKeyRateLimitService allowedRateLimiter =
                org.mockito.Mockito.mock(PublicKeyRateLimitService.class);

        org.mockito.Mockito.when(
                allowedRateLimiter.isAllowed(
                        org.mockito.ArgumentMatchers.anyString()
                )
        ).thenReturn(true);

        org.mockito.Mockito.when(
                failingCache.store(
                        org.mockito.ArgumentMatchers.any(UUID.class),
                        org.mockito.ArgumentMatchers.anyString()
                )
        ).thenThrow(
                new IllegalStateException("Redis storage unavailable")
        );

        PublicKeyService service = new PublicKeyService(
                rsaUtil,
                aesGcmUtil,
                failingCache,
                allowedRateLimiter
        );

        assertThrows(
                IllegalStateException.class,
                () -> service.generatePublicKey(
                        UUID.randomUUID(),
                        "test-client"
                )
        );
    }

}
