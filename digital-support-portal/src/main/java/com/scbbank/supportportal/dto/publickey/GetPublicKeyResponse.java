package com.scbbank.supportportal.dto.publickey;

public record GetPublicKeyResponse(
        String publicKey,
        int expiresIn
) {
}
