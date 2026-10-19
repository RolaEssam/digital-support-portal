package com.scbbank.supportportal.auth.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INVALID_CREDENTIALS("DSP-AUTH-001", HttpStatus.UNAUTHORIZED,         "error.credentials.invalid"),
    ACCOUNT_LOCKED     ("DSP-AUTH-002", HttpStatus.LOCKED,               "error.account.locked"),
    KEY_EXPIRED        ("DSP-AUTH-003", HttpStatus.BAD_REQUEST,          "error.key.expired"),
    DECRYPTION_FAILED  ("DSP-AUTH-005", HttpStatus.BAD_REQUEST,          "error.decryption.failed"),
    RATE_LIMITED       ("DSP-AUTH-008", HttpStatus.TOO_MANY_REQUESTS,    "error.rate.limited"),
    VALIDATION_ERROR   ("DSP-AUTH-010", HttpStatus.BAD_REQUEST,          "error.validation"),
    INTERNAL_ERROR     ("DSP-GEN-999",  HttpStatus.INTERNAL_SERVER_ERROR, "error.internal");

    private final String code;
    private final HttpStatus status;
    private final String messageKey;
}