package com.scbbank.supportportal.auth.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionsTest {

    @Test
    void errorCodes_matchTheContract() {
        assertEquals("DSP-AUTH-001", ErrorCode.INVALID_CREDENTIALS.getCode());
        assertEquals(HttpStatus.UNAUTHORIZED, ErrorCode.INVALID_CREDENTIALS.getStatus());
        assertEquals("DSP-AUTH-002", ErrorCode.ACCOUNT_LOCKED.getCode());
        assertEquals(HttpStatus.LOCKED, ErrorCode.ACCOUNT_LOCKED.getStatus());
        assertEquals("DSP-AUTH-003", ErrorCode.KEY_EXPIRED.getCode());
        assertEquals("DSP-AUTH-005", ErrorCode.DECRYPTION_FAILED.getCode());
        assertEquals("DSP-AUTH-008", ErrorCode.RATE_LIMITED.getCode());
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ErrorCode.RATE_LIMITED.getStatus());
        assertEquals("DSP-AUTH-010", ErrorCode.VALIDATION_ERROR.getCode());
        assertEquals("DSP-GEN-999", ErrorCode.INTERNAL_ERROR.getCode());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR.getStatus());
    }

    @Test
    void errorCodes_areUnique_andHaveMessageKeys() {
        Set<String> codes = new HashSet<>();
        for (ErrorCode c : ErrorCode.values()) {
            assertTrue(codes.add(c.getCode()), "duplicate code " + c.getCode());
            assertFalse(c.getMessageKey().isBlank());
        }
    }

    @Test
    void invalidCredentials_carriesRemainingAttempts() {
        InvalidCredentialsException ex = new InvalidCredentialsException(3);
        assertEquals(ErrorCode.INVALID_CREDENTIALS, ex.getErrorCode());
        assertEquals(3, ex.getData().get("remainingAttempts"));
    }

    @Test
    void accountLocked_carriesZeroAttemptsAndIsoRetryAt() {
        Instant retry = Instant.parse("2026-10-13T12:30:00Z");
        AccountLockedException ex = new AccountLockedException(retry);

        assertEquals(ErrorCode.ACCOUNT_LOCKED, ex.getErrorCode());
        assertEquals(0, ex.getData().get("remainingAttempts"));
        assertEquals("2026-10-13T12:30:00Z", ex.getData().get("retryAt"));
    }

    @Test
    void plainAuthException_hasEmptyData_andSafeMessage() {
        AuthException ex = new AuthException(ErrorCode.KEY_EXPIRED);
        assertTrue(ex.getData().isEmpty());
        assertEquals("KEY_EXPIRED", ex.getMessage());   // only the enum name, never user input
    }

    @Test
    void subclasses_areAuthExceptions() {
        assertInstanceOf(AuthException.class, new InvalidCredentialsException(1));
        assertInstanceOf(AuthException.class, new AccountLockedException(Instant.now()));
    }
}