package com.scbbank.supportportal.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "auth.token")
public record TokenProperties(
        @NotNull @DefaultValue("15m") Duration accessTtl,
        @NotNull @DefaultValue("8h") Duration refreshTtl,
        @NotBlank @DefaultValue("https://scbbank.com/support-portal") String issuer,
        @NotBlank @DefaultValue("support-portal-api") String audience,
        @DefaultValue("true") boolean cookieSecure,
        String privateKey,
        String publicKey) {
}
