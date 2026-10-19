package com.scbbank.supportportal.controller;

import com.scbbank.supportportal.constants.ApplicationConstants;
import com.scbbank.supportportal.constants.PublicKeyApiDocsConstants;
import com.scbbank.supportportal.dto.base.ApiResponseDto;
import com.scbbank.supportportal.dto.publickey.GetPublicKeyRequest;
import com.scbbank.supportportal.dto.publickey.GetPublicKeyResponse;
import com.scbbank.supportportal.service.PublicKeyService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(
        name = "Authentication",
        description = "Authentication and login encryption APIs"
)
public class PublicKeyController {

    private final PublicKeyService publicKeyService;

    @Operation(
            summary = PublicKeyApiDocsConstants.SUMMARY,
            description = PublicKeyApiDocsConstants.DESCRIPTION
    )
    @ApiResponses(value = {

            @ApiResponse(
                    responseCode = "200",
                    description = "Public key generated successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = ApiResponseDto.class
                            ),
                            examples = @ExampleObject(
                                    value = PublicKeyApiDocsConstants.SUCCESS_EXAMPLE
                            )
                    )
            ),

            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid or missing requestId (DSP-AUTH-013)"
            ),

            @ApiResponse(
                    responseCode = "409",
                    description = "Duplicate requestId (DSP-AUTH-014)"
            ),

            @ApiResponse(
                    responseCode = "429",
                    description = "Rate limit exceeded (DSP-AUTH-008)",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = PublicKeyApiDocsConstants.RATE_LIMIT_EXAMPLE
                            )
                    )
            ),

            @ApiResponse(
                    responseCode = "503",
                    description = "Public-key generation unavailable (DSP-GEN-998)"
            )
    })
    @PostMapping(
            value = "/public-key",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponseDto<GetPublicKeyResponse>> getPublicKey(
            @Valid @RequestBody GetPublicKeyRequest request) {

        String publicKey = publicKeyService.generatePublicKey(
                request.requestId()
        );

        GetPublicKeyResponse response = new GetPublicKeyResponse(
                publicKey,
                ApplicationConstants.PUBLIC_KEY_EXPIRY_SECONDS
        );

        // TODO(AHMED): Map invalid/missing requestId to
        // HTTP 400, DSP-AUTH-013.

        // TODO(AHMED): Map duplicate requestId to
        // HTTP 409, DSP-AUTH-014.

        // TODO(AHMED): Ensure global rate limit rejection
        // returns HTTP 429 with DSP-AUTH-008.

        // TODO(AHMED): Ensure this endpoint is publicly accessible
        // in Spring Security configuration.

        return ResponseEntity.ok(
                ApiResponseDto.success(response)
        );
    }
}
