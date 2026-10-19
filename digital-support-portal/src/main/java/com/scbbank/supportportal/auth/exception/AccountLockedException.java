package com.scbbank.supportportal.auth.exception;

import com.scbbank.supportportal.auth.exception.ErrorCode;

import java.time.Instant;
import java.util.Map;

public class AccountLockedException extends AuthException {

    public AccountLockedException(Instant retryAt) {
        super(ErrorCode.ACCOUNT_LOCKED,
                Map.of("remainingAttempts", 0, "retryAt", retryAt.toString())); // ISO-8601 UTC
    }
}


