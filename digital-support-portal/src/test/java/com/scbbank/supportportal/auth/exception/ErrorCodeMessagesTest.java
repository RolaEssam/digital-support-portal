package com.scbbank.supportportal.auth.exception;

import com.scbbank.supportportal.dto.base.ApiResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;


import java.time.Instant;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        ResourceBundleMessageSource ms = new ResourceBundleMessageSource();
        ms.setBasename("messages");
        ms.setDefaultEncoding("UTF-8");
        handler = new GlobalExceptionHandler(ms);
    }

    private JsonNode json(ResponseEntity<ApiResponseDto<?>> res) throws Exception {
        return mapper.readTree(mapper.writeValueAsString(res.getBody()));
    }

    @Test
    void invalidCredentialsReturns401WithRemainingAttempts() throws Exception {
        ResponseEntity<ApiResponseDto<?>> res = handler.handleAuth(new InvalidCredentialsException(2));

        assertEquals(401, res.getStatusCode().value());
        JsonNode body = json(res);
        assertFalse(body.get("successful").asBoolean());
        assertEquals("DSP-AUTH-001", body.get("errorCode").asText());
        assertEquals(2, body.get("data").get("remainingAttempts").asInt());
    }

    @Test
    void lockedAccountReturns423WithRetryAt() throws Exception {
        Instant retryAt = Instant.parse("2026-10-17T20:00:00Z");
        ResponseEntity<ApiResponseDto<?>> res = handler.handleAuth(new AccountLockedException(retryAt));

        assertEquals(423, res.getStatusCode().value());
        JsonNode body = json(res);
        assertEquals("DSP-AUTH-002", body.get("errorCode").asText());
        assertEquals(retryAt.toString(), body.get("data").get("retryAt").asText());
    }

    @Test
    void validationErrorReturns400() throws Exception {
        MethodArgumentNotValidException ex = Mockito.mock(MethodArgumentNotValidException.class);
        ResponseEntity<ApiResponseDto<?>> res = handler.handleValidation(ex);

        assertEquals(400, res.getStatusCode().value());
        assertEquals("DSP-AUTH-010", json(res).get("errorCode").asText());
    }

    @Test
    void unexpectedErrorReturns500WithoutLeakingDetails() throws Exception {
        ResponseEntity<ApiResponseDto<?>> res =
                handler.handleUnexpected(new RuntimeException("db password=secret"));

        assertEquals(500, res.getStatusCode().value());
        JsonNode body = json(res);
        assertEquals("DSP-GEN-999", body.get("errorCode").asText());
        assertFalse(body.toString().contains("secret"));
    }
}