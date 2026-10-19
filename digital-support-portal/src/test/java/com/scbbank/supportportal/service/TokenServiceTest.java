package com.scbbank.supportportal.service;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.scbbank.supportportal.auth.exception.AuthException;
import com.scbbank.supportportal.auth.exception.ErrorCode;
import com.scbbank.supportportal.config.TokenProperties;
import com.scbbank.supportportal.dto.cache.RefreshTokenCache;
import com.scbbank.supportportal.dto.token.AuthUser;
import com.scbbank.supportportal.dto.token.TokenPair;
import com.scbbank.supportportal.repository.RefreshTokenCacheRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class TokenServiceTest {

    private final Map<String, RefreshTokenCache> store = new HashMap<>();
    private RefreshTokenCacheRepository repo;
    private TokenProperties props;
    private JwtDecoder decoder;
    private TokenService service;

    @BeforeEach
    void setUp() throws Exception {
        RSAKey jwk = new RSAKeyGenerator(2048).keyID("test").generate();
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));
        decoder = NimbusJwtDecoder.withPublicKey(jwk.toRSAPublicKey()).build();

        props = mock(TokenProperties.class);
        when(props.accessTtl()).thenReturn(Duration.ofMinutes(15));
        when(props.refreshTtl()).thenReturn(Duration.ofHours(8));
        when(props.issuer()).thenReturn("https://scbbank.com/support-portal");
        when(props.audience()).thenReturn("support-portal-api");

        repo = mock(RefreshTokenCacheRepository.class);
        when(repo.findById(anyString())).thenAnswer(i -> Optional.ofNullable(store.get(i.<String>getArgument(0))));
        when(repo.save(any(RefreshTokenCache.class))).thenAnswer(i -> {
            RefreshTokenCache t = i.getArgument(0);
            store.put(t.getTokenHash(), t);
            return t;
        });
        doAnswer(i -> store.remove(i.<RefreshTokenCache>getArgument(0).getTokenHash()))
                .when(repo).delete(any(RefreshTokenCache.class));
        when(repo.findByUserId(anyLong())).thenAnswer(i -> store.values().stream()
                .filter(t -> t.getUserId().equals(i.<Long>getArgument(0))).toList());
        doAnswer(i -> {
            ((Iterable<RefreshTokenCache>) i.getArgument(0)).forEach(t -> store.remove(t.getTokenHash()));
            return null;
        }).when(repo).deleteAll(anyIterable());

        service = new TokenService(encoder, repo, props);
    }

    private TokenPair login() {
        return service.issueTokens(new AuthUser(1042L, "AGENT"));
    }

    // ---------- issueTokens ----------

    @Test
    void accessToken_hasOnlyExpectedClaims_signedRs256() {
        Jwt jwt = decoder.decode(login().accessToken());

        assertEquals("RS256", jwt.getHeaders().get("alg"));
        assertEquals("1042", jwt.getSubject());
        assertEquals("AGENT", jwt.getClaimAsString("role"));
        assertEquals("https://scbbank.com/support-portal", jwt.getClaimAsString("iss"));
        assertTrue(jwt.getAudience().contains("support-portal-api"));
        assertNotNull(jwt.getId());
        assertEquals(Set.of("sub", "role", "iss", "aud", "jti", "iat", "exp"), jwt.getClaims().keySet());
    }

    @Test
    void accessToken_expiresIn15Minutes() {
        TokenPair pair = login();
        assertEquals(900, pair.expiresIn());

        Jwt jwt = decoder.decode(pair.accessToken());
        assertEquals(900, Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()).getSeconds());
    }

    @Test
    void refreshToken_isOpaque256Bit_andStoredOnlyAsSha256Hash() {
        TokenPair pair = login();

        assertEquals(43, pair.refreshToken().length());              // 32 bytes, Base64URL, no padding
        assertFalse(store.containsKey(pair.refreshToken()));          // raw token is never a key
        assertTrue(store.containsKey(TokenService.hash(pair.refreshToken())));
        assertTrue(store.values().stream().noneMatch(t -> t.toString().contains(pair.refreshToken())));
    }

    @Test
    void refreshToken_storedWithUserRoleAndSessionEnd() {
        TokenPair pair = login();
        RefreshTokenCache saved = store.get(TokenService.hash(pair.refreshToken()));

        assertEquals(1042L, saved.getUserId());
        assertEquals("AGENT", saved.getRole());
        assertTrue(saved.getExpiresAt().isAfter(Instant.now().plus(Duration.ofHours(7))));
    }

    @Test
    void tokens_areDifferentEachLogin() {
        assertNotEquals(login().refreshToken(), login().refreshToken());
    }

    @Test
    void accessToken_isCappedAtSessionEnd() {
        when(props.refreshTtl()).thenReturn(Duration.ofMinutes(5));   // shorter than accessTtl
        TokenPair pair = login();
        assertTrue(pair.expiresIn() <= 300);
    }

    // ---------- refresh ----------

    @Test
    void refresh_returnsNewTokens_andRotates() {
        TokenPair first = login();

        TokenPair second = service.refresh(first.refreshToken());

        assertNotEquals(first.refreshToken(), second.refreshToken());
        assertFalse(store.containsKey(TokenService.hash(first.refreshToken())));
        assertTrue(store.containsKey(TokenService.hash(second.refreshToken())));
        assertEquals("1042", decoder.decode(second.accessToken()).getSubject());
    }

    @Test
    void refresh_reusedToken_isRejected() {
        TokenPair first = login();
        service.refresh(first.refreshToken());

        AuthException ex = assertThrows(AuthException.class, () -> service.refresh(first.refreshToken()));
        assertEquals(ErrorCode.INVALID_CREDENTIALS, ex.getErrorCode());
    }

    @Test
    void refresh_doesNotExtendSession() {
        TokenPair first = login();
        Instant end = store.get(TokenService.hash(first.refreshToken())).getExpiresAt();

        TokenPair second = service.refresh(first.refreshToken());

        assertEquals(end, store.get(TokenService.hash(second.refreshToken())).getExpiresAt());
    }

    @Test
    void refresh_unknownBlankOrNull_isRejected() {
        for (String bad : new String[]{"unknown", "", "   ", null}) {
            AuthException ex = assertThrows(AuthException.class, () -> service.refresh(bad));
            assertEquals(ErrorCode.INVALID_CREDENTIALS, ex.getErrorCode());
        }
    }

    @Test
    void refresh_expiredSession_isRejected_andDeleted() {
        String raw = "expired-raw-token";
        String hash = TokenService.hash(raw);
        store.put(hash, new RefreshTokenCache(hash, 1042L, "AGENT", Instant.now().minusSeconds(10), 1));

        assertThrows(AuthException.class, () -> service.refresh(raw));
        assertFalse(store.containsKey(hash));
    }

    // ---------- revokeAll ----------

    @Test
    void revokeAll_removesEverySessionOfTheUser_only() {
        TokenPair a = login();
        TokenPair b = login();
        TokenPair other = service.issueTokens(new AuthUser(7L, "ADMIN"));

        service.revokeAll(1042L);

        assertFalse(store.containsKey(TokenService.hash(a.refreshToken())));
        assertFalse(store.containsKey(TokenService.hash(b.refreshToken())));
        assertTrue(store.containsKey(TokenService.hash(other.refreshToken())));
    }

    // ---------- hash ----------

    @Test
    void hash_isLowercaseHex64_andDeterministic() {
        String h = TokenService.hash("abc");
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", h);
        assertEquals(h, TokenService.hash("abc"));
    }
}