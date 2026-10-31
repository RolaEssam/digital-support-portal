package com.scbbank.supportportal.service;

import com.scbbank.supportportal.auth.exception.AccountLockedException;
import com.scbbank.supportportal.auth.exception.AuthException;
import com.scbbank.supportportal.auth.exception.ErrorCode;
import com.scbbank.supportportal.auth.exception.InvalidCredentialsException;
import com.scbbank.supportportal.config.LoginProperties;
import com.scbbank.supportportal.dto.cache.LoginAttemptCache;
import com.scbbank.supportportal.dto.login.LoginRequest;
import com.scbbank.supportportal.dto.login.LoginResponse;
import com.scbbank.supportportal.dto.token.AuthUser;
import com.scbbank.supportportal.dto.token.TokenPair;
import com.scbbank.supportportal.repository.LoginAttemptCacheRepository;
import com.scbbank.supportportal.service.user.UserLookup;
import com.scbbank.supportportal.service.user.UserLookup.UserRecord;
import com.scbbank.supportportal.util.AesGcmUtil;
import com.scbbank.supportportal.util.RedisUtil;
import com.scbbank.supportportal.util.RsaUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LoginServiceTest {

    private static final String USERNAME = "mona.hassan";
    private static final String PASSWORD = "Test@1234";

    @Mock RedisUtil redisUtil;
    @Mock LoginAttemptCacheRepository attempts;
    @Mock UserLookup users;
    @Mock TokenService tokenService;

    // Real classes
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);   // low cost = fast tests
    private final LoginProperties props =
            new LoginProperties(5, Duration.ofMinutes(15), Duration.ofMinutes(15), 100,
                    Duration.ofMinutes(30), "/home");

    private final Map<String, String> fakeRedis = new HashMap<>();
    private final Map<String, LoginAttemptCache> attemptStore = new HashMap<>();

    private RsaUtil rsaUtil;
    private AesGcmUtil aesGcmUtil;
    private LoginKeyCacheService keyCache;
    private PublicKeyService publicKeyService;
    private LoginService service;

    @BeforeEach
    void setUp() throws Exception {
        // Fake Redis: same semantics as setIfAbsent and GETDEL
        when(redisUtil.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenAnswer(i -> {
            String key = i.getArgument(0);
            if (fakeRedis.containsKey(key)) return false;
            fakeRedis.put(key, i.getArgument(1));
            return true;
        });
        when(redisUtil.getAndDelete(anyString()))
                .thenAnswer(i -> fakeRedis.remove(i.<String>getArgument(0)));

        // Fake attempts repository so the counter really increases
        when(attempts.findById(anyString()))
                .thenAnswer(i -> Optional.ofNullable(attemptStore.get(i.<String>getArgument(0))));
        when(attempts.save(any(LoginAttemptCache.class))).thenAnswer(i -> {
            LoginAttemptCache a = i.getArgument(0);
            attemptStore.put(a.getUsername(), a);
            return a;
        });
        doAnswer(i -> attemptStore.remove(i.<String>getArgument(0)))
                .when(attempts).deleteById(anyString());

        when(users.findByUsername(USERNAME)).thenReturn(Optional.of(
                new UserRecord(1042L, USERNAME, encoder.encode(PASSWORD), "AGENT", true)));

        when(tokenService.issueTokens(any(AuthUser.class)))
                .thenReturn(new TokenPair("access-token", "refresh-token", 900));

        // Real crypto classes
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(256);
        SecretKey master = kg.generateKey();

        rsaUtil = new RsaUtil();
        aesGcmUtil = new AesGcmUtil(master);
        keyCache = new LoginKeyCacheService(redisUtil);
        publicKeyService = new PublicKeyService(rsaUtil, aesGcmUtil, keyCache);

        service = new LoginService(keyCache, aesGcmUtil, rsaUtil, users,
                new LoginAttemptService(attempts, props), encoder, tokenService, props);
    }

    // ---------- helpers ----------

    /** Same as the frontend: RSA-OAEP, SHA-256, MGF1-SHA-256. */
    private String encrypt(String publicKeyBase64, String plain) throws Exception {
        PublicKey key = KeyFactory.getInstance("RSA").generatePublic(
                new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyBase64)));
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key,
                new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT));
        return Base64.getEncoder().encodeToString(cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8)));
    }

    /** Step 1 of the real flow: get a public key for a new requestId, then encrypt the credentials. */
    private LoginRequest freshRequest(String username, String password) throws Exception {
        UUID id = UUID.randomUUID();
        String publicKey = publicKeyService.generatePublicKey(id);
        return new LoginRequest(id.toString(),
                encrypt(publicKey, username), encrypt(publicKey, password));
    }

    // ---------- success ----------

    @Test
    void fullFlow_success() throws Exception {
        LoginResponse res = service.login(freshRequest(USERNAME, PASSWORD));

        assertEquals("access-token", res.accessToken());
        assertEquals("refresh-token", res.refreshToken());
        assertEquals(900, res.expiresIn());

        verify(tokenService).issueTokens(argThat(u -> u.userId().equals(1042L) && u.role().equals("AGENT")));
    }

    @Test
    void usernameIsCaseAndSpaceInsensitive() throws Exception {
        LoginResponse res = service.login(freshRequest("  Mona.Hassan ", PASSWORD));
        assertEquals("access-token", res.accessToken());
    }

    @Test
    void privateKeyIsStoredEncrypted_notAsPlainBase64() throws Exception {
        UUID id = UUID.randomUUID();
        publicKeyService.generatePublicKey(id);

        String stored = fakeRedis.values().iterator().next();
        // A plain PKCS8 key in Base64 starts with "MIIE"; the AES-GCM output must not.
        assertFalse(stored.startsWith("MII"));
        // And it must decrypt back into a valid private key.
        assertNotNull(rsaUtil.decodePrivateKey(aesGcmUtil.decrypt(stored)));
    }

    // ---------- requestId / key ----------

    @Test
    void requestId_worksOnlyOnce() throws Exception {
        LoginRequest req = freshRequest(USERNAME, PASSWORD);

        service.login(req);                                   // first time: OK
        AuthException ex = assertThrows(AuthException.class, () -> service.login(req));

        assertEquals(ErrorCode.KEY_EXPIRED, ex.getErrorCode());
    }

    @Test
    void unknownRequestId_returnsKeyExpired() throws Exception {
        LoginRequest real = freshRequest(USERNAME, PASSWORD);
        LoginRequest unknown = new LoginRequest(UUID.randomUUID().toString(), real.username(), real.password());

        AuthException ex = assertThrows(AuthException.class, () -> service.login(unknown));

        assertEquals(ErrorCode.KEY_EXPIRED, ex.getErrorCode());
        verifyNoInteractions(users, tokenService);
    }

    @Test
    void badRequestIdFormat_returnsValidationError() {
        LoginRequest bad = new LoginRequest("not-a-uuid", "AAAA", "AAAA");

        AuthException ex = assertThrows(AuthException.class, () -> service.login(bad));

        assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
    }

    @Test
    void duplicateRequestId_publicKeyIsRejected() {
        UUID id = UUID.randomUUID();
        publicKeyService.generatePublicKey(id);

        assertThrows(IllegalStateException.class, () -> publicKeyService.generatePublicKey(id));
    }

    // ---------- decryption ----------

    @Test
    void garbageCiphertext_returnsDecryptionFailed() {
        UUID id = UUID.randomUUID();
        publicKeyService.generatePublicKey(id);
        LoginRequest bad = new LoginRequest(id.toString(), "bm90LXJlYWw=", "AAAA");

        AuthException ex = assertThrows(AuthException.class, () -> service.login(bad));

        assertEquals(ErrorCode.DECRYPTION_FAILED, ex.getErrorCode());
    }

    @Test
    void encryptedWithAnotherKey_returnsDecryptionFailed() throws Exception {
        UUID id = UUID.randomUUID();
        publicKeyService.generatePublicKey(id);
        String otherPublicKey = publicKeyService.generatePublicKey(UUID.randomUUID());   // a different pair

        LoginRequest bad = new LoginRequest(id.toString(),
                encrypt(otherPublicKey, USERNAME), encrypt(otherPublicKey, PASSWORD));

        AuthException ex = assertThrows(AuthException.class, () -> service.login(bad));
        assertEquals(ErrorCode.DECRYPTION_FAILED, ex.getErrorCode());
    }

    @Test
    void sha1OaepFromWrongClient_isRejected() throws Exception {
        // Catches the classic mismatch: client encrypts with the default SHA-1 OAEP.
        UUID id = UUID.randomUUID();
        String publicKey = publicKeyService.generatePublicKey(id);
        PublicKey key = KeyFactory.getInstance("RSA").generatePublic(
                new X509EncodedKeySpec(Base64.getDecoder().decode(publicKey)));
        Cipher sha1 = Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding");
        sha1.init(Cipher.ENCRYPT_MODE, key);
        String bad = Base64.getEncoder().encodeToString(sha1.doFinal(USERNAME.getBytes(StandardCharsets.UTF_8)));

        AuthException ex = assertThrows(AuthException.class,
                () -> service.login(new LoginRequest(id.toString(), bad, bad)));
        assertEquals(ErrorCode.DECRYPTION_FAILED, ex.getErrorCode());
    }

    // ---------- format ----------

    @Test
    void blankUsername_returnsValidationError() throws Exception {
        AuthException ex = assertThrows(AuthException.class, () -> service.login(freshRequest("   ", PASSWORD)));
        assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
    }

    @Test
    void tooLongPassword_returnsValidationError() throws Exception {
        AuthException ex = assertThrows(AuthException.class,
                () -> service.login(freshRequest(USERNAME, "a".repeat(101))));
        assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
    }

    // ---------- wrong credentials ----------

    @Test
    void wrongPassword_countsDown() throws Exception {
        for (int expected = 4; expected >= 1; expected--) {
            InvalidCredentialsException ex = assertThrows(InvalidCredentialsException.class,
                    () -> service.login(freshRequest(USERNAME, "wrong")));
            assertEquals(expected, ex.getData().get("remainingAttempts"));
        }
    }

    @Test
    void unknownUser_sameErrorAsWrongPassword() throws Exception {
        InvalidCredentialsException unknown = assertThrows(InvalidCredentialsException.class,
                () -> service.login(freshRequest("nobody", PASSWORD)));
        InvalidCredentialsException wrong = assertThrows(InvalidCredentialsException.class,
                () -> service.login(freshRequest(USERNAME, "wrong")));

        assertEquals(wrong.getErrorCode(), unknown.getErrorCode());
    }

    @Test
    void inactiveUser_cannotLogin() throws Exception {
        when(users.findByUsername(USERNAME)).thenReturn(Optional.of(
                new UserRecord(1042L, USERNAME, encoder.encode(PASSWORD), "AGENT", false)));

        assertThrows(InvalidCredentialsException.class, () -> service.login(freshRequest(USERNAME, PASSWORD)));
        verify(tokenService, never()).issueTokens(any());
    }

    @Test
    void failedLogin_stillConsumesTheKey() throws Exception {
        LoginRequest req = freshRequest(USERNAME, "wrong");

        assertThrows(InvalidCredentialsException.class, () -> service.login(req));
        AuthException ex = assertThrows(AuthException.class, () -> service.login(req));

        assertEquals(ErrorCode.KEY_EXPIRED, ex.getErrorCode());   // needs a new public key
    }

    // ---------- lockout ----------

    @Test
    void fifthFailure_locksAccount() throws Exception {
        for (int i = 0; i < 4; i++) {
            assertThrows(InvalidCredentialsException.class, () -> service.login(freshRequest(USERNAME, "wrong")));
        }

        AccountLockedException ex = assertThrows(AccountLockedException.class,
                () -> service.login(freshRequest(USERNAME, "wrong")));

        assertEquals(ErrorCode.ACCOUNT_LOCKED, ex.getErrorCode());
        assertEquals(0, ex.getData().get("remainingAttempts"));
        assertNotNull(ex.getData().get("retryAt"));
    }

    @Test
    void lockedAccount_rejectsCorrectPassword_withoutLookup() throws Exception {
        for (int i = 0; i < 5; i++) {
            try { service.login(freshRequest(USERNAME, "wrong")); } catch (AuthException ignored) { }
        }
        clearInvocations(users);

        assertThrows(AccountLockedException.class, () -> service.login(freshRequest(USERNAME, PASSWORD)));

        verify(users, never()).findByUsername(anyString());
        verify(tokenService, never()).issueTokens(any());
    }

    @Test
    void successfulLogin_resetsCounter() throws Exception {
        assertThrows(InvalidCredentialsException.class, () -> service.login(freshRequest(USERNAME, "wrong")));
        assertEquals(1, attemptStore.get(USERNAME).getFailedCount());

        service.login(freshRequest(USERNAME, PASSWORD));

        assertFalse(attemptStore.containsKey(USERNAME));
    }

    // ---------- no leaks ----------

    @Test
    void toStrings_doNotLeakSecrets() throws Exception {
        LoginRequest req = freshRequest(USERNAME, PASSWORD);
        assertFalse(req.toString().contains(req.password()));
        assertFalse(req.toString().contains(req.username()));

        LoginResponse res = new LoginResponse("secret-access", "secret-refresh", 900, 1800, "/home",
                new LoginResponse.UserSummary(1042L, "mona.hassan", "AGENT"));
    }
}