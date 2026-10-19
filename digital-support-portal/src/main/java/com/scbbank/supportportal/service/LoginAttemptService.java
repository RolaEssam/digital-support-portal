package com.scbbank.supportportal.service;

import com.scbbank.supportportal.auth.exception.AccountLockedException;
import com.scbbank.supportportal.auth.exception.AuthException;
import com.scbbank.supportportal.auth.exception.InvalidCredentialsException;
import com.scbbank.supportportal.config.LoginProperties;
import com.scbbank.supportportal.dto.cache.LoginAttemptCache;
import com.scbbank.supportportal.repository.LoginAttemptCacheRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class LoginAttemptService {

    private final LoginAttemptCacheRepository attempts;
    private final LoginProperties props;

    public LoginAttemptService(LoginAttemptCacheRepository attempts, LoginProperties props) {
        this.attempts = attempts;
        this.props = props;
    }

    public void assertUnlocked(String username, Instant now) {
        LoginAttemptCache attempt = load(username);
        if (attempt.isLocked(now)) {
            throw new AccountLockedException(attempt.getLockedUntil());
        }
    }

    public AuthException recordFailure(String username, Instant now) {
        LoginAttemptCache attempt = load(username);
        attempt.recordFailure(now, props.maxAttempts(), props.lockDuration(), props.attemptWindow());
        attempts.save(attempt);

        if (attempt.isLocked(now)) {
            return new AccountLockedException(attempt.getLockedUntil());
        }
        return new InvalidCredentialsException(
                Math.max(0, props.maxAttempts() - attempt.getFailedCount()));
    }

    public void reset(String username) {
        attempts.deleteById(username);
    }

    private LoginAttemptCache load(String username) {
        return attempts.findById(username).orElseGet(() -> new LoginAttemptCache(username));
    }
}
