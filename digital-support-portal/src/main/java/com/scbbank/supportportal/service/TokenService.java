package com.scbbank.supportportal.service;

import com.scbbank.supportportal.auth.exception.AuthException;
import com.scbbank.supportportal.auth.exception.ErrorCode;
import com.scbbank.supportportal.config.TokenProperties;
import com.scbbank.supportportal.dto.cache.RefreshTokenCache;
import com.scbbank.supportportal.dto.token.AuthUser;
import com.scbbank.supportportal.dto.token.TokenPair;
import com.scbbank.supportportal.repository.RefreshTokenCacheRepository;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * Access token: JWT signed RS256, claims sub / role / iss / aud / jti / iat / exp.
 * Refresh token: random 256-bit opaque value, stored in Redis only as a SHA-256 hash,
 * rotated on every use. A token that was already used is rejected.
 * The session has an absolute end (auth.token.refresh-ttl): rotation never extends it.
 */
@Service
public class TokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final JwtEncoder jwtEncoder;
    private final RefreshTokenCacheRepository refreshTokens;
    private final TokenProperties props;

    public TokenService(JwtEncoder jwtEncoder,
                        RefreshTokenCacheRepository refreshTokens,
                        TokenProperties props) {
        this.jwtEncoder = jwtEncoder;
        this.refreshTokens = refreshTokens;
        this.props = props;
    }

    public TokenPair issueTokens(AuthUser user) {
        Instant now = Instant.now();
        return buildPair(user.userId(), user.role(), now, now.plus(props.refreshTtl()));
    }

    public TokenPair refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw invalid();
        }

        RefreshTokenCache current = refreshTokens.findById(hash(rawRefreshToken))
                .orElseThrow(TokenService::invalid);

        refreshTokens.delete(current);

        Instant now = Instant.now();
        if (!current.getExpiresAt().isAfter(now)) {
            throw invalid();
        }
        return buildPair(current.getUserId(), current.getRole(), now, current.getExpiresAt());
    }

    public void revokeAll(Long userId) {
        refreshTokens.deleteAll(refreshTokens.findByUserId(userId));
    }

    private TokenPair buildPair(Long userId, String role, Instant now, Instant sessionEnd) {
        Instant accessExpiry = now.plus(props.accessTtl());
        if (accessExpiry.isAfter(sessionEnd)) {
            accessExpiry = sessionEnd;
        }

        String accessToken = encodeAccessToken(userId, role, now, accessExpiry);

        String rawRefreshToken = newOpaqueToken();
        long ttlSeconds = Math.max(1, Duration.between(now, sessionEnd).toSeconds());
        refreshTokens.save(new RefreshTokenCache(
                hash(rawRefreshToken), userId, role, sessionEnd, ttlSeconds));

        long expiresIn = Duration.between(now, accessExpiry).toSeconds();
        return new TokenPair(accessToken, rawRefreshToken, expiresIn);
    }

    private String encodeAccessToken(Long userId, String role, Instant issuedAt, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(props.issuer())
                .audience(List.of(props.audience()))
                .subject(String.valueOf(userId))
                .id(UUID.randomUUID().toString())
                .claim("role", role)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private static String newOpaqueToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private static AuthException invalid() {
        return new AuthException(ErrorCode.INVALID_CREDENTIALS);
    }
}
