package com.scbbank.supportportal.dto.login;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank
        @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
        String requestId,

        @NotBlank @Size(max = 1024) String username,   // Base64 RSA-OAEP ciphertext
        @NotBlank @Size(max = 1024) String password) { // Base64 RSA-OAEP ciphertext

    // Never log ciphertext or anything derived from credentials.
    @Override
    public String toString() {
        return "LoginRequest[requestId=" + requestId + "]";
    }
}