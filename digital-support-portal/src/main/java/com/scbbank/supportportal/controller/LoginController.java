package com.scbbank.supportportal.controller;

import com.scbbank.supportportal.config.TokenProperties;
import com.scbbank.supportportal.constants.LoginApiDocsConstants;
import com.scbbank.supportportal.dto.base.ApiResponseDto;
import com.scbbank.supportportal.dto.login.LoginRequest;
import com.scbbank.supportportal.dto.login.LoginResponse;
import com.scbbank.supportportal.service.LoginService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = LoginApiDocsConstants.TAG)
@RestController
@RequestMapping("/api/v1/auth")
public class LoginController {

    static final String REFRESH_COOKIE = "DSP_RT";

    private final LoginService loginService;
    private final TokenProperties tokenProps;

    public LoginController(LoginService loginService, TokenProperties tokenProps) {
        this.loginService = loginService;
        this.tokenProps = tokenProps;
    }

    @PostMapping("/login")
    @Operation(summary = LoginApiDocsConstants.SUMMARY, description = LoginApiDocsConstants.DESCRIPTION)
    public ResponseEntity<ApiResponseDto<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest http) {
        LoginResponse result = loginService.login(request);

        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, result.refreshToken())
                .httpOnly(true)
                .secure(tokenProps.cookieSecure())
                .sameSite("Strict")
                .path(http.getContextPath() + "/api/v1/auth")
                .maxAge(tokenProps.refreshTtl())
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(ApiResponseDto.success(result.withoutRefreshToken()));
    }
}
