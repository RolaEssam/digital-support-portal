package com.scbbank.supportportal.config;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CookieBearerTokenResolverTest {

    private final CookieBearerTokenResolver resolver = new CookieBearerTokenResolver();

    @Test
    void readsTokenFromAuthorizationHeader() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer header-token");
        assertEquals("header-token", resolver.resolve(req));
    }

    @Test
    void readsTokenFromCookie_whenNoHeader() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setCookies(new Cookie("DSP_AT", "cookie-token"));
        assertEquals("cookie-token", resolver.resolve(req));
    }

    @Test
    void headerWinsOverCookie() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer header-token");
        req.setCookies(new Cookie("DSP_AT", "cookie-token"));
        assertEquals("header-token", resolver.resolve(req));
    }

    @Test
    void returnsNull_whenNothingPresent() {
        assertNull(resolver.resolve(new MockHttpServletRequest()));
    }

    @Test
    void ignoresOtherCookies() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setCookies(new Cookie("DSP_RT", "refresh-token"), new Cookie("other", "x"));
        assertNull(resolver.resolve(req));
    }

    @Test
    void ignoresBlankAccessCookie() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setCookies(new Cookie("DSP_AT", "   "));
        assertNull(resolver.resolve(req));
    }

    @Test
    void nonBearerAuthorizationHeader_fallsBackToCookie() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Basic dXNlcjpwYXNz");
        req.setCookies(new Cookie("DSP_AT", "cookie-token"));
        assertEquals("cookie-token", resolver.resolve(req));
    }

    @Test
    void picksAccessCookie_amongSeveralCookies() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setCookies(new Cookie("DSP_RT", "refresh-token"), new Cookie("DSP_AT", "cookie-token"));
        assertEquals("cookie-token", resolver.resolve(req));
    }

    @Test
    void ignoresCookie_onAuthEndpoints_withContextPath() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setContextPath("/api/authentication");
        req.setRequestURI("/api/authentication/api/v1/auth/login");
        req.setCookies(new Cookie("DSP_AT", "old-or-expired-token"));
        assertNull(resolver.resolve(req));
    }

    @Test
    void ignoresCookie_onAuthEndpoints_withoutContextPath() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/v1/auth/public-key");
        req.setCookies(new Cookie("DSP_AT", "old-or-expired-token"));
        assertNull(resolver.resolve(req));
    }

    @Test
    void ignoresHeader_onAuthEndpoints() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/v1/auth/login");
        req.addHeader("Authorization", "Bearer header-token");
        assertNull(resolver.resolve(req));
    }

    @Test
    void stillReadsCookie_onOtherEndpoints() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setContextPath("/api/authentication");
        req.setRequestURI("/api/authentication/api/v1/customers/search");
        req.setCookies(new Cookie("DSP_AT", "cookie-token"));
        assertEquals("cookie-token", resolver.resolve(req));
    }
}