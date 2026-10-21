package com.scbbank.supportportal.service;

import com.scbbank.supportportal.auth.exception.AuthException;
import com.scbbank.supportportal.auth.exception.ErrorCode;
import com.scbbank.supportportal.config.LoginProperties;
import com.scbbank.supportportal.dto.login.LoginRequest;
import com.scbbank.supportportal.dto.login.LoginResponse;
import com.scbbank.supportportal.dto.token.AuthUser;
import com.scbbank.supportportal.dto.token.TokenPair;
import com.scbbank.supportportal.service.user.UserLookup;
import com.scbbank.supportportal.service.user.UserLookup.UserRecord;
import com.scbbank.supportportal.util.AesGcmUtil;
import com.scbbank.supportportal.util.RsaUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.PrivateKey;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class LoginService {

    private final LoginKeyCacheService keyCache;
    private final AesGcmUtil aesGcmUtil;
    private final RsaUtil rsaUtil;
    private final UserLookup users;
    private final LoginAttemptService loginAttempts;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final LoginProperties props;
    private final String dummyHash;

    public LoginService(LoginKeyCacheService keyCache,
                        AesGcmUtil aesGcmUtil,
                        RsaUtil rsaUtil,
                        UserLookup users,
                        LoginAttemptService loginAttempts,
                        PasswordEncoder passwordEncoder,
                        TokenService tokenService,
                        LoginProperties props) {
        this.keyCache = keyCache;
        this.aesGcmUtil = aesGcmUtil;
        this.rsaUtil = rsaUtil;
        this.users = users;
        this.loginAttempts = loginAttempts;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.props = props;
        this.dummyHash = passwordEncoder.encode("not-a-real-password");
    }

    public LoginResponse login(LoginRequest req) {
        UUID requestId = parseRequestId(req.requestId());

        PrivateKey key = consumePrivateKey(requestId);

        String rawUsername = decrypt(req.username(), key);
        String password = decrypt(req.password(), key);
        validatePlaintext(rawUsername, password);
        String username = rawUsername.trim().toLowerCase(Locale.ROOT);

        Instant now = Instant.now();
        loginAttempts.assertUnlocked(username, now);

        Optional<UserRecord> found = users.findByUsername(username);
        String hash = found.map(UserRecord::passwordHash).orElse(dummyHash);
        boolean passwordOk = passwordEncoder.matches(password, hash);

        if (found.isEmpty() || !passwordOk || !found.get().active()) {
            throw loginAttempts.recordFailure(username, now);
        }

        loginAttempts.reset(username);
        UserRecord user = found.get();
        TokenPair pair = tokenService.issueTokens(new AuthUser(user.id(), user.role()));
        return LoginResponse.of(
                pair,
                props.idleTimeout().toSeconds(),
                props.homeRoute(),
                new LoginResponse.UserSummary(user.id(), username, String.valueOf(user.role())));
    }

    private UUID parseRequestId(String requestId) {
        try {
            return UUID.fromString(requestId);
        } catch (IllegalArgumentException e) {
            throw new AuthException(ErrorCode.VALIDATION_ERROR);
        }
    }

    private PrivateKey consumePrivateKey(UUID requestId) {
        String encryptedKey = keyCache.consume(requestId);
        if (encryptedKey == null || encryptedKey.isBlank()) {
            throw new AuthException(ErrorCode.KEY_EXPIRED);
        }
        try {
            return rsaUtil.decodePrivateKey(aesGcmUtil.decrypt(encryptedKey));
        } catch (RuntimeException e) {
            throw new AuthException(ErrorCode.INTERNAL_ERROR);
        }
    }

    private void validatePlaintext(String rawUsername, String password) {
        if (rawUsername.isBlank() || password.isEmpty()
                || rawUsername.length() > props.maxFieldLength()
                || password.length() > props.maxFieldLength()) {
            throw new AuthException(ErrorCode.VALIDATION_ERROR);
        }
    }

    private String decrypt(String base64, PrivateKey key) {
        try {
            return rsaUtil.decrypt(base64, key);
        } catch (IllegalArgumentException e) {
            throw new AuthException(ErrorCode.DECRYPTION_FAILED);
        }
    }
}
