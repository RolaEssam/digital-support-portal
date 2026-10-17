package com.scbbank.supportportal.filter;

import com.scbbank.supportportal.service.PublicKeyRateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RateLimitFilterTest {

    private PublicKeyRateLimitService rateLimitService;
    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        rateLimitService = mock(PublicKeyRateLimitService.class);
        filter = new RateLimitFilter(rateLimitService);
    }

    @Test
    void shouldAllowRequestWithinLimit() throws Exception {

        MockHttpServletRequest request =
                new MockHttpServletRequest(
                        "POST",
                        "/api/authentication/api/v1/auth/public-key"
                );

        request.setContextPath("/api/authentication");
        request.setRemoteAddr("192.0.2.10");

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        FilterChain chain = mock(FilterChain.class);

        when(rateLimitService.isAllowed(
                "192.0.2.10",
                "/api/authentication/api/v1/auth/public-key"
        )).thenReturn(true);

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());

        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldRejectRequestWhenLimitExceeded() throws Exception {

        MockHttpServletRequest request =
                new MockHttpServletRequest(
                        "POST",
                        "/api/authentication/api/v1/auth/public-key"
                );

        request.setContextPath("/api/authentication");
        request.setRemoteAddr("192.0.2.10");

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        FilterChain chain = mock(FilterChain.class);

        when(rateLimitService.isAllowed(
                "192.0.2.10",
                "/api/authentication/api/v1/auth/public-key"
        )).thenReturn(false);

        filter.doFilter(request, response, chain);

        assertEquals(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                response.getStatus()
        );

        verifyNoInteractions(chain);
    }

    @Test
    void shouldApplyToOtherApiEndpoints() throws Exception {

        MockHttpServletRequest request =
                new MockHttpServletRequest(
                        "POST",
                        "/api/authentication/api/v1/auth/login"
                );

        request.setContextPath("/api/authentication");
        request.setRemoteAddr("192.0.2.11");

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        FilterChain chain = mock(FilterChain.class);

        when(rateLimitService.isAllowed(
                "192.0.2.11",
                "/api/authentication/api/v1/auth/login"
        )).thenReturn(true);

        filter.doFilter(request, response, chain);

        verify(rateLimitService).isAllowed(
                "192.0.2.11",
                "/api/authentication/api/v1/auth/login"
        );

        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldNotContinueWhenRedisFails() throws Exception {

        MockHttpServletRequest request =
                new MockHttpServletRequest(
                        "POST",
                        "/api/authentication/api/v1/auth/public-key"
                );

        request.setContextPath("/api/authentication");
        request.setRemoteAddr("192.0.2.10");

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        FilterChain chain = mock(FilterChain.class);

        when(rateLimitService.isAllowed(
                anyString(), anyString()
        )).thenThrow(
                new IllegalStateException("Redis unavailable")
        );

        assertThrows(
                IllegalStateException.class,
                () -> filter.doFilter(request, response, chain)
        );

        verifyNoInteractions(chain);
    }
}
