package com.scbbank.supportportal.filter;

import com.scbbank.supportportal.service.PublicKeyRateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final PublicKeyRateLimitService rateLimitService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {

        // Rate limit all application endpoints.
        // TODO(AHMED): Confirm whether health checks and
        // internal monitoring endpoints should be excluded.

        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        // TODO(AHMED): Configure trusted reverse-proxy IP handling.
        // Never trust arbitrary X-Forwarded-For headers.

        String clientIp = request.getRemoteAddr();

        String requestUri = request.getRequestURI();

        boolean allowed = rateLimitService.isAllowed(
                clientIp,
                requestUri
        );

        if (!allowed) {

            // TODO(AHMED): Return the standard ApiResponseDto error
            // with HTTP 429 and error code DSP-AUTH-008.

            response.setStatus(
                    HttpStatus.TOO_MANY_REQUESTS.value()
            );
            return;
        }

        filterChain.doFilter(request, response);
    }
}
