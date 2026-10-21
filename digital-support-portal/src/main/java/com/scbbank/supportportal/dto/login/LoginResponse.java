package com.scbbank.supportportal.dto.login;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.scbbank.supportportal.dto.token.TokenPair;

/**
 * Login result. Tokens are @JsonIgnore'd: they travel in the Authorization header
 * and the DSP_RT cookie, never in the JSON body.
 */
public record LoginResponse(@JsonIgnore String accessToken,
                            @JsonIgnore String refreshToken,
                            long expiresIn,
                            long idleTimeout,
                            String homeRoute,
                            UserSummary user) {

    public static LoginResponse of(TokenPair pair, long idleTimeout, String homeRoute, UserSummary user) {
        return new LoginResponse(pair.accessToken(), pair.refreshToken(), pair.expiresIn(),
                idleTimeout, homeRoute, user);
    }

    public record UserSummary(long userId, String username, String role) {}

    // Tokens must never reach logs.
    @Override
    public String toString() {
        return "LoginResponse[expiresIn=" + expiresIn + ", idleTimeout=" + idleTimeout
                + ", homeRoute=" + homeRoute + ", user=" + user + "]";
    }
}