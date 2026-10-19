package com.scbbank.supportportal.dto.login;
import com.fasterxml.jackson.annotation.JsonInclude;

import com.scbbank.supportportal.dto.token.TokenPair;
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoginResponse(String accessToken,
                            String refreshToken,
                            long expiresIn,
                            String tokenType) {

    public static LoginResponse of(TokenPair pair) {
        return new LoginResponse(pair.accessToken(), pair.refreshToken(), pair.expiresIn(), "Bearer");
    }

    public LoginResponse withoutRefreshToken() {
        return new LoginResponse(accessToken, null, expiresIn, tokenType);
    }
    // Tokens must never reach logs.
    @Override
    public String toString() {
        return "LoginResponse[expiresIn=" + expiresIn + ", tokenType=" + tokenType + "]";
    }
}