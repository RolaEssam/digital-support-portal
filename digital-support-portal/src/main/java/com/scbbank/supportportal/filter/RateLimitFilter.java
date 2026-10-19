package com.scbbank.supportportal.filter;

import com.scbbank.supportportal.auth.exception.ErrorCode;
import com.scbbank.supportportal.service.PublicKeyRateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final PublicKeyRateLimitService rateLimitService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.equals(request.getContextPath() + "/health") || uri.endsWith("/health");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String clientIp = request.getRemoteAddr();
        String requestUri = request.getRequestURI();

        boolean allowed = rateLimitService.isAllowed(clientIp, requestUri);

        if (!allowed) {
            ErrorCode ec = ErrorCode.RATE_LIMITED;
            response.setStatus(ec.getStatus().value());
            response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"successful\":false,\"messageType\":\"ERROR\",\"errorCode\":\""
                            + ec.getCode()
                            + "\",\"errorMessage\":\"Too many requests. Please try again later.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
