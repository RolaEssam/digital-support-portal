package com.scbbank.supportportal.controller;

import com.scbbank.supportportal.service.PublicKeyService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PublicKeyControllerTest {

    private final PublicKeyService publicKeyService =
            mock(PublicKeyService.class);

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new PublicKeyController(publicKeyService))
            .build();

    @Test
    void shouldReturnPublicKeySuccessfully() throws Exception {

        UUID requestId = UUID.randomUUID();

        when(publicKeyService.generatePublicKey(
                eq(requestId)
        )).thenReturn("TEST_PUBLIC_KEY");

        mockMvc.perform(
                        post("/api/v1/auth/public-key")
                                .contentType("application/json")
                                .content("""
                                {
                                  "requestId": "%s"
                                }
                                """.formatted(requestId))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successful").value(true))
                .andExpect(jsonPath("$.messageType").value("SUCCESS"))
                .andExpect(jsonPath("$.data.publicKey").value("TEST_PUBLIC_KEY"))
                .andExpect(jsonPath("$.data.expiresIn").value(300));

        verify(publicKeyService).generatePublicKey(
                eq(requestId)
        );
    }

    @Test
    void shouldRejectInvalidRequestId() throws Exception {

        mockMvc.perform(
                        post("/api/v1/auth/public-key")
                                .contentType("application/json")
                                .content("""
                                {
                                  "requestId": "invalid-uuid"
                                }
                                """)
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(publicKeyService);
    }

    @Test
    void shouldRejectMissingRequestId() throws Exception {

        mockMvc.perform(
                        post("/api/v1/auth/public-key")
                                .contentType("application/json")
                                .content("{}")
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(publicKeyService);
    }
}
