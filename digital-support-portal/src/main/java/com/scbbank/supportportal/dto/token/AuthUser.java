package com.scbbank.supportportal.dto.token;

import java.util.Objects;

public record AuthUser(Long userId, String role) {

    public AuthUser {
        Objects.requireNonNull(userId, "userId is required");
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("role is required");
        }
    }
}
