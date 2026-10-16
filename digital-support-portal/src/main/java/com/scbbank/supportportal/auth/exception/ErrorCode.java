package com.scbbank.supportportal.auth.exception;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INVALID_REQUEST_ID("DSP-AUTH-013", HttpStatus.BAD_REQUEST,"error.request.id.invalid"),
    REQUEST_EXPIRED   ("DSP-AUTH-003", HttpStatus.BAD_REQUEST,"error.request.expired"),
    DECRYPTION_FAILED ("DSP-AUTH-005", HttpStatus.BAD_REQUEST,"error.decryption.failed"),
    INVALID_CREDENTIALS("DSP-AUTH-001", HttpStatus.UNAUTHORIZED,"error.credentials.invalid"),
    USERNAME_REQUIRED("DSP-AUTH-010", HttpStatus.BAD_REQUEST, "error.username.required"),
    PASSWORD_REQUIRED("DSP-AUTH-011", HttpStatus.BAD_REQUEST, "error.password.required"),
    INVALID_LENGTH   ("DSP-AUTH-012", HttpStatus.BAD_REQUEST, "error.credentials.length"),
    ACCOUNT_LOCKED    ("DSP-AUTH-002", HttpStatus.LOCKED,               "error.account.locked"),
    RATE_LIMITED      ("DSP-AUTH-008", HttpStatus.TOO_MANY_REQUESTS,    "error.rate.limited"),
    SERVICE_UNAVAILABLE("DSP-GEN-998", HttpStatus.SERVICE_UNAVAILABLE,  "error.service.unavailable"),
    INTERNAL_ERROR    ("DSP-GEN-999", HttpStatus.INTERNAL_SERVER_ERROR, "error.internal");

    private final String code;
    private final HttpStatus status;
    private final String messageKey;




}