package com.scbbank.supportportal.service;

import com.scbbank.supportportal.util.AesGcmUtil;
import com.scbbank.supportportal.util.RsaUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.KeyPair;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PublicKeyService {

    private final RsaUtil rsaUtil;
    private final AesGcmUtil aesGcmUtil;
    private final LoginKeyCacheService loginKeyCacheService;

    public String generatePublicKey(UUID requestId) {

        Objects.requireNonNull(
                requestId,
                "Request ID is required"
        );

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
