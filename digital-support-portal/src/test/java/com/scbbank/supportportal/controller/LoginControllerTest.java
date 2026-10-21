package com.scbbank.supportportal.controller;

import com.scbbank.supportportal.auth.exception.AuthException;
import com.scbbank.supportportal.auth.exception.ErrorCode;
import com.scbbank.supportportal.config.TokenProperties;
import com.scbbank.supportportal.dto.base.ApiResponseDto;
import com.scbbank.supportportal.dto.login.LoginRequest;
import com.scbbank.supportportal.dto.login.LoginResponse;
import com.scbbank.supportportal.service.LoginService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Plain unit tests: no Spring context, no Redis. They check what the controller
 * puts on the HTTP response after a successful login.
 */
@ExtendWith(MockitoExtension.class)
class LoginControllerCookiesTest {

    private static final String UUID_OK = "123e4567-e89b-12d3-a456-426614174000";
    private static final String ACCESS = "access-jwt-value";
    private static final String REFRESH = "refresh-opaque-value";

    @Mock LoginService loginService;
    @Mock TokenProperties tokenProps;

    private LoginController controller;
    private LoginRequest request;

    @BeforeEach
    void setUp() {
        lenient().when(tokenProps.cookieSecure()).thenReturn(true);

        controller = new LoginController(loginService, tokenProps);
        request = new LoginRequest(UUID_OK, "cipher-user", "cipher-pass");
        LoginResponse loginResponse = new LoginResponse(ACCESS, REFRESH, 900, 1800, "/home",
                new LoginResponse.UserSummary(1042L, "mona.hassan", "AGENT"));
        lenient().when(loginService.login(any())).thenReturn(loginResponse);
    }

    private ResponseEntity<ApiResponseDto<LoginResponse>> callLogin(String contextPath) {
        MockHttpServletRequest http = new MockHttpServletRequest();
        http.setContextPath(contextPath);
        return controller.login(request, http);
    }

    private static String onlyCookie(ResponseEntity<?> res) {
        List<String> all = res.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertNotNull(all, "no Set-Cookie header");
        assertEquals(1, all.size(), "expected exactly one Set-Cookie header: " + all);
        return all.get(0);
    }

    @Test
    void returnsStatus200WithBody() {
        ResponseEntity<ApiResponseDto<LoginResponse>> res = callLogin("/api/authentication");
        assertEquals(200, res.getStatusCode().value());
        assertNotNull(res.getBody());
    }

    @Test
    void accessTokenIsInAuthorizationHeader() {
        ResponseEntity<ApiResponseDto<LoginResponse>> res = callLogin("/api/authentication");
        assertEquals("Bearer " + ACCESS, res.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
    }

    @Test
    void setsExactlyOneCookie_withTheAccessToken() {
        String c = onlyCookie(callLogin("/api/authentication"));
        assertTrue(c.startsWith("DSP_AT=" + ACCESS));
    }

    @Test
    void cookie_hasSecurityFlags_andShortLifetime() {
        String c = onlyCookie(callLogin("/api/authentication"));
        assertTrue(c.contains("HttpOnly"));
        assertTrue(c.contains("Secure"));
        assertTrue(c.contains("SameSite=Strict"));
        assertTrue(c.contains("Max-Age=900"));
    }

    @Test
    void cookie_pathIsContextPath() {
        String c = onlyCookie(callLogin("/api/authentication"));
        assertTrue(c.contains("Path=/api/authentication;"));
    }

    @Test
    void cookie_pathIsRoot_whenNoContextPath() {
        String c = onlyCookie(callLogin(""));
        assertTrue(c.contains("Path=/;"));
    }

    @Test
    void secureFlagFollowsConfiguration() {
        when(tokenProps.cookieSecure()).thenReturn(false);
        String c = onlyCookie(callLogin("/api/authentication"));
        assertFalse(c.contains("Secure"));
    }

    @Test
    void refreshTokenIsNeverSentToTheClient() {
        ResponseEntity<ApiResponseDto<LoginResponse>> res = callLogin("/api/authentication");
        assertFalse(res.getHeaders().toString().contains(REFRESH));
    }

    @Test
    void responseIsNotCacheable() {
        ResponseEntity<ApiResponseDto<LoginResponse>> res = callLogin("/api/authentication");
        assertEquals("no-store", res.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL));
    }

    @Test
    void serviceFailure_isPropagated() {
        when(loginService.login(any())).thenThrow(new AuthException(ErrorCode.KEY_EXPIRED));
        assertThrows(AuthException.class, () -> callLogin("/api/authentication"));
    }
}