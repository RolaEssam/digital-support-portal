package com.scbbank.supportportal.dto.login;

import com.scbbank.supportportal.dto.login.LoginRequest;
import com.scbbank.supportportal.dto.login.LoginResponse;
import com.scbbank.supportportal.dto.token.TokenPair;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class LoginDtoValidationTest {

    private static final String UUID_OK = "7c9e6679-7425-40de-944b-e07fc1f90ae7";
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private Set<String> invalidFields(LoginRequest r) {
        return validator.validate(r).stream()
                .map(v -> v.getPropertyPath().toString()).collect(Collectors.toSet());
    }

    @Test
    void validRequest_hasNoViolations() {
        assertTrue(invalidFields(new LoginRequest(UUID_OK, "AAAA", "BBBB")).isEmpty());
    }

    @Test
    void badRequestId_isRejected() {
        assertEquals(Set.of("requestId"), invalidFields(new LoginRequest("not-a-uuid", "AAAA", "BBBB")));
        assertEquals(Set.of("requestId"), invalidFields(new LoginRequest(null, "AAAA", "BBBB")));
        assertEquals(Set.of("requestId"), invalidFields(new LoginRequest("", "AAAA", "BBBB")));
    }

    @Test
    void blankUsernameOrPassword_isRejected() {
        assertEquals(Set.of("username"), invalidFields(new LoginRequest(UUID_OK, "", "BBBB")));
        assertEquals(Set.of("password"), invalidFields(new LoginRequest(UUID_OK, "AAAA", null)));
    }

    @Test
    void tooLongCiphertext_isRejected() {
        String huge = "A".repeat(1025);
        assertEquals(Set.of("username", "password"), invalidFields(new LoginRequest(UUID_OK, huge, huge)));
    }

    @Test
    void rsa2048Ciphertext_fitsTheLimit() {
        String b64 = "A".repeat(344);
        assertTrue(invalidFields(new LoginRequest(UUID_OK, b64, b64)).isEmpty());
    }

    @Test
    void request_toString_hidesCiphertext() {
        LoginRequest r = new LoginRequest(UUID_OK, "SECRET-USER", "SECRET-PASS");
        assertFalse(r.toString().contains("SECRET"));
        assertTrue(r.toString().contains(UUID_OK));
    }

    @Test
    void response_of_mapsTokenPair() {
        LoginResponse.UserSummary user = new LoginResponse.UserSummary(1042L, "mona.hassan", "AGENT");
        LoginResponse r = LoginResponse.of(new TokenPair("a", "r", 900), 1800, "/home", user);
        assertEquals("a", r.accessToken());
        assertEquals("r", r.refreshToken());
        assertEquals(900, r.expiresIn());
        assertEquals(1800, r.idleTimeout());
        assertEquals("/home", r.homeRoute());
        assertEquals(user, r.user());
    }

    @Test
    void response_toString_hidesTokens() {
        LoginResponse r = new LoginResponse("secret-a", "secret-r", 1, 1800, "/home",
                new LoginResponse.UserSummary(1042L, "mona.hassan", "AGENT"));
        assertFalse(r.toString().contains("secret"));
    }
}
