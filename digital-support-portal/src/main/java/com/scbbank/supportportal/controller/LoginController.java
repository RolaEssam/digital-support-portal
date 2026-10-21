package com.scbbank.supportportal.controller;

import com.scbbank.supportportal.config.TokenProperties;
import com.scbbank.supportportal.dto.base.ApiResponseDto;
import com.scbbank.supportportal.dto.login.LoginRequest;
import com.scbbank.supportportal.dto.login.LoginResponse;
import com.scbbank.supportportal.service.LoginService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class LoginController {

    private static final String ACCESS_COOKIE_NAME = "DSP_AT";

    private final LoginService loginService;
    private final TokenProperties tokenProps;

    public LoginController(LoginService loginService, TokenProperties tokenProps) {
        this.loginService = loginService;
        this.tokenProps = tokenProps;
    }

    @PostMapping(value = "/login",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseDto<LoginResponse>> login(@RequestBody @Valid LoginRequest request,
                                                               HttpServletRequest httpRequest) {
        LoginResponse response = loginService.login(request);
        String contextPath = httpRequest.getContextPath();

        // The only cookie: the access token (JWT), same lifetime as the token itself.
        ResponseCookie accessCookie = ResponseCookie.from(ACCESS_COOKIE_NAME, response.accessToken())
                .httpOnly(true)
                .secure(tokenProps.cookieSecure())
                .sameSite("Strict")
                .path(contextPath.isEmpty() ? "/" : contextPath)
                .maxAge(response.expiresIn())
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + response.accessToken())
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(ApiResponseDto.success(response));
    }
}