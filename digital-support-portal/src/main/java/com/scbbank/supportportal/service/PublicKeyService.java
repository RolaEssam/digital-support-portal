package com.scbbank.supportportal.service;

import com.scbbank.supportportal.util.AesGcmUtil;
import com.scbbank.supportportal.util.RsaUtil;
import org.springframework.stereotype.Service;

import java.security.KeyPair;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

@Service
public class PublicKeyService {

    private final RsaUtil rsaUtil;
    private final AesGcmUtil aesGcmUtil;
    private final LoginKeyCacheService loginKeyCacheService;
    private final PublicKeyRateLimitService rateLimitService;

    public PublicKeyService(
            RsaUtil rsaUtil,
            AesGcmUtil aesGcmUtil,
            LoginKeyCacheService loginKeyCacheService,
            PublicKeyRateLimitService rateLimitService) {

        this.rsaUtil = rsaUtil;
        this.aesGcmUtil = aesGcmUtil;
        this.loginKeyCacheService = loginKeyCacheService;
        this.rateLimitService = rateLimitService;
    }

    public String generatePublicKey(
            UUID requestId,
            String clientIdentifier) {

        Objects.requireNonNull(
                requestId, "Request ID is required"
        );

        if (clientIdentifier == null ||
                clientIdentifier.isBlank()) {
            throw new IllegalArgumentException(
                    "Client identifier is required"
            );
        }

        if (!rateLimitService.isAllowed(clientIdentifier)) {

            // TODO(AHMED): Map rate-limit rejection to
            // HTTP 429 with error code DSP-AUTH-008.

            throw new IllegalStateException(
                    "Public key request rate limit exceeded"
            );
        }

        KeyPair keyPair = rsaUtil.generateKeyPair();

        String privateKeyBase64 = Base64.getEncoder()
                .encodeToString(
                        keyPair.getPrivate().getEncoded()
                );

        String encryptedPrivateKey =
                aesGcmUtil.encrypt(privateKeyBase64);

        boolean stored = loginKeyCacheService.store(
                requestId,
                encryptedPrivateKey
        );

        if (!stored) {

            // TODO(AHMED): Map duplicate requestId to
            // HTTP 409 with error code DSP-AUTH-014.

            throw new IllegalStateException(
                    "Duplicate request ID"
            );
        }

        return Base64.getEncoder().encodeToString(
                keyPair.getPublic().getEncoded()
        );
    }
}
