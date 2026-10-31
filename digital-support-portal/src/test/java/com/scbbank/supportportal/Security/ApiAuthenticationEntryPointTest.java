package com.scbbank.supportportal.Security;

import com.scbbank.supportportal.auth.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** AC5: the three 401 bodies from the API contract. */
class ApiAuthenticationEntryPointTest {

    private final ApiAuthenticationEntryPoint entryPoint = new ApiAuthenticationEntryPoint(messages());

    private static ResourceBundleMessageSource messages() {
        ResourceBundleMessageSource ms = new ResourceBundleMessageSource();
        ms.setBasename("messages");
        ms.setDefaultEncoding("UTF-8");
        return ms;
    }

    private MockHttpServletResponse run(AuthenticationException ex) throws Exception {
        MockHttpServletResponse res = new MockHttpServletResponse();
        entryPoint.commence(new MockHttpServletRequest("GET", "/api/v1/tickets"), res, ex);
        return res;
    }

    private static void assertBody(MockHttpServletResponse res, int status, String code, String message) throws Exception {
        assertEquals(status, res.getStatus());
        assertTrue(res.getContentType().startsWith("application/json"));
        assertEquals("no-store", res.getHeader("Cache-Control"));
        String body = res.getContentAsString();
        assertTrue(body.contains("\"successful\":false"), body);
        assertTrue(body.contains("\"messageType\":\"ERROR\""), body);
        assertTrue(body.contains("\"errorCode\":\"" + code + "\""), body);
        assertTrue(body.contains("\"errorMessage\":\"" + message + "\""), body);
    }

    @Test
    void missingToken_016() throws Exception {
        assertBody(run(new InsufficientAuthenticationException("no token")),
                401, "DSP-AUTH-016", "Authentication required. Please sign in.");
    }

    @Test
    void expiredToken_017() throws Exception {
        JwtValidationException cause = new JwtValidationException("expired",
                List.of(new OAuth2Error("invalid_token", "Jwt expired at 2026-01-01T00:00:00Z", null)));
        assertBody(run(new InvalidBearerTokenException("Jwt expired", cause)),
                401, "DSP-AUTH-017", "Session expired. Please sign in again.");
    }

    @Test
    void badSignatureOrMalformedToken_018() throws Exception {
        assertBody(run(new InvalidBearerTokenException("bad", new BadJwtException("Signed JWT rejected: Invalid signature"))),
                401, "DSP-AUTH-018", "Authentication required. Please sign in.");
        assertBody(run(new InvalidBearerTokenException("bad", new BadJwtException("Malformed token"))),
                401, "DSP-AUTH-018", "Authentication required. Please sign in.");
    }

    @Test
    void codesMatchTheContract() {
        assertEquals("DSP-AUTH-015", ErrorCode.ACCESS_DENIED.getCode());
        assertEquals(403, ErrorCode.ACCESS_DENIED.getStatus().value());
        assertEquals("DSP-AUTH-016", ErrorCode.AUTH_REQUIRED.getCode());
        assertEquals("DSP-AUTH-017", ErrorCode.TOKEN_EXPIRED.getCode());
        assertEquals("DSP-AUTH-018", ErrorCode.TOKEN_INVALID.getCode());
    }
}
