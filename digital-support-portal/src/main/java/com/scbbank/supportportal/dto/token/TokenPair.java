package com.scbbank.supportportal.dto.token;

public record TokenPair(String accessToken, String refreshToken, long expiresIn) {

    @Override
    public String toString() {
        return "TokenPair[expiresIn=" + expiresIn + "]";
    }
}
