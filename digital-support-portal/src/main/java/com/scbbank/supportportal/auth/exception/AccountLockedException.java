package com.scbbank.supportportal.auth.exception;

import java.time.Instant;
import java.util.Map;

public class AccountLockedException extends AuthException {

    private final Instant retryAt;

    public AccountLockedException(Instant retryAt) {
        super(ErrorCode.ACCOUNT_LOCKED);
        this.retryAt = retryAt;
    }

    @Override
    public Map<String, Object> getDetails() {
        return Map.of("remainingAttempts", 0, "retryAt", retryAt.toString());
    }
}