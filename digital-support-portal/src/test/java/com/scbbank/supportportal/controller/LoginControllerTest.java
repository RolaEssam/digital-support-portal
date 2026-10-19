package com.scbbank.supportportal.controller;

import com.scbbank.supportportal.config.TokenProperties;
import com.scbbank.supportportal.dto.login.LoginRequest;
import com.scbbank.supportportal.dto.login.LoginResponse;
import com.scbbank.supportportal.service.LoginService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Duration;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LoginControllerTest {

    private static final String UUID_OK = "7c9e6679-7425-40de-944b-e07fc1f90ae7";

    private LoginService loginService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        loginService = mock(LoginService.class);
        TokenProperties props = mock(TokenProperties.class);
        when(props.refreshTtl()).thenReturn(Duration.ofHours(8));
        when(props.cookieSecure()).thenReturn(true);
        mvc = MockMvcBuilders.standaloneSetup(new LoginController(loginService, props)).build();
    }

    private static String body(String requestId, String user, String pass) {
        return """
               {"requestId":"%s","username":"%s","password":"%s"}
               """.formatted(requestId, user, pass);
    }

    @Test
    void success_setsCookie_andKeepsRefreshTokenOutOfBody() throws Exception {
        when(loginService.login(any(LoginRequest.class)))
                .thenReturn(new LoginResponse("ACCESS-JWT", "REFRESH-RAW", 900, "Bearer"));

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(UUID_OK, "AAAA", "BBBB")))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("Set-Cookie", containsString("DSP_RT=REFRESH-RAW")))
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
                .andExpect(header().string("Set-Cookie", containsString("Secure")))
                .andExpect(header().string("Set-Cookie", containsString("SameSite=Strict")))
                .andExpect(header().string("Set-Cookie", containsString("Path=/api/v1/auth")))
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=28800")))
                .andExpect(jsonPath("$.successful").value(true))
                .andExpect(jsonPath("$.messageType").value("SUCCESS"))
                .andExpect(jsonPath("$.data.accessToken").value("ACCESS-JWT"))
                .andExpect(jsonPath("$.data.expiresIn").value(900))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(content().string(not(containsString("REFRESH-RAW"))));
    }

    @Test
    void invalidRequestId_isRejected_beforeReachingTheService() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("not-a-uuid", "AAAA", "BBBB")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(loginService);
    }

    @Test
    void missingFields_areRejected() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestId\":\"" + UUID_OK + "\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(loginService);
    }

    @Test
    void malformedJson_isRejected() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{bad json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void onlyPostIsAllowed() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/auth/login"))
                .andExpect(status().isMethodNotAllowed());
    }
}