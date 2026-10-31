package com.scbbank.supportportal.Security;

import com.scbbank.supportportal.auth.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Builds the 401 body from the API contract:
 *   DSP-AUTH-016 token missing, DSP-AUTH-017 token expired, DSP-AUTH-018 token invalid.
 * Spring Security calls this before any controller runs, so the controller advice cannot do it.
 */
@Component
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final MessageSource messageSource;

    public ApiAuthenticationEntryPoint(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException ex) throws IOException {
        ErrorCode ec = classify(ex);
        String message = messageSource.getMessage(ec.getMessageKey(), null, LocaleContextHolder.getLocale());

        response.setStatus(ec.getStatus().value());
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(
                "{\"successful\":false,\"messageType\":\"ERROR\",\"errorCode\":\"" + ec.getCode()
                        + "\",\"errorMessage\":\"" + escape(message) + "\"}");
    }

    static ErrorCode classify(AuthenticationException ex) {
        if (ex instanceof OAuth2AuthenticationException) {
            // A token was sent but was rejected.
            return isExpired(ex) ? ErrorCode.TOKEN_EXPIRED : ErrorCode.TOKEN_INVALID;
        }
        // No token at all (anonymous caller).
        return ErrorCode.AUTH_REQUIRED;
    }

    /** Spring's timestamp validator reports "Jwt expired at ..."; look for it in the cause chain. */
    private static boolean isExpired(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof JwtValidationException jve
                    && jve.getErrors().stream().anyMatch(e -> containsExpired(e.getDescription()))) {
                return true;
            }
            if (containsExpired(t.getMessage())) {
                return true;
            }
            if (t.getCause() == t) {
                break;
            }
        }
        return false;
    }

    private static boolean containsExpired(String text) {
        return text != null && text.toLowerCase(Locale.ROOT).contains("expired");
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
