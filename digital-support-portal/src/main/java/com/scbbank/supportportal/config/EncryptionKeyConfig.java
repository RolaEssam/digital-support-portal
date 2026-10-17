package com.scbbank.supportportal.config;

import com.scbbank.supportportal.constants.ApplicationConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Configuration
public class EncryptionKeyConfig {

    @Bean
    public SecretKey aesSecretKey(
            @Value("${auth.encryption.key:}") String encodedKey) {

        if (encodedKey == null || encodedKey.isBlank()) {
            throw new IllegalStateException(
                    "AES encryption key is missing"
            );
        }

        byte[] keyBytes;

        try {
            keyBytes = Base64.getDecoder().decode(encodedKey);

        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "AES encryption key must be valid Base64", e
            );
        }

        if (keyBytes.length !=
                ApplicationConstants.AES_KEY_SIZE_BYTES) {

            throw new IllegalStateException(
                    "AES encryption key must be "
                            + ApplicationConstants.AES_KEY_SIZE_BYTES
                            + " bytes"
            );
        }

        return new SecretKeySpec(
                keyBytes,
                ApplicationConstants.AES_ALGORITHM
        );
    }
}
