
package com.scbbank.supportportal.dto.base;

import com.scbbank.supportportal.dto.base.ApiResponseDto;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ApiResponseDtoTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldSerializeSuccessResponse() throws Exception {

        var response = ApiResponseDto.success(
                Map.of(
                        "publicKey", "TEST_PUBLIC_KEY",
                        "expiresIn", 300
                )
        );

        JsonNode json = mapper.readTree(
                mapper.writeValueAsString(response)
        );

        assertTrue(json.get("successful").asBoolean());
        assertEquals("SUCCESS", json.get("messageType").asText());

        assertEquals(
                "TEST_PUBLIC_KEY",
                json.get("data").get("publicKey").asText()
        );

        assertEquals(
                300,
                json.get("data").get("expiresIn").asInt()
        );

        assertFalse(json.has("errorCode"));
        assertFalse(json.has("errorMessage"));
    }

    @Test
    void shouldSerializeErrorResponse() throws Exception {

        var response = ApiResponseDto.error(
                "DSP-AUTH-013",
                "Request ID is missing or is not a valid UUID."
        );

        JsonNode json = mapper.readTree(
                mapper.writeValueAsString(response)
        );

        assertFalse(json.get("successful").asBoolean());
        assertEquals("ERROR", json.get("messageType").asText());

        assertEquals(
                "DSP-AUTH-013",
                json.get("errorCode").asText()
        );

        assertEquals(
                "Request ID is missing or is not a valid UUID.",
                json.get("errorMessage").asText()
        );

        assertFalse(json.has("data"));
    }

    @Test
    void shouldSupportAdditionalErrorData() throws Exception {

        var response = ApiResponseDto.error(
                "DSP-AUTH-002",
                "Account temporarily locked.",
                Map.of(
                        "remainingAttempts", 0,
                        "retryAt", "2026-10-13T12:30:00Z"
                )
        );

        JsonNode json = mapper.readTree(
                mapper.writeValueAsString(response)
        );

        assertFalse(json.get("successful").asBoolean());
        assertEquals("ERROR", json.get("messageType").asText());

        assertEquals(
                "DSP-AUTH-002",
                json.get("errorCode").asText()
        );

        assertEquals(
                0,
                json.get("data").get("remainingAttempts").asInt()
        );

        assertEquals(
                "2026-10-13T12:30:00Z",
                json.get("data").get("retryAt").asText()
        );
    }
}
