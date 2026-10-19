package com.scbbank.supportportal.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "auth.login")
public record LoginProperties(
        @Positive @DefaultValue("5") int maxAttempts,
        @NotNull @DefaultValue("15m") Duration lockDuration,
        @NotNull @DefaultValue("15m") Duration attemptWindow,
        @Positive @Max(1024) @DefaultValue("100") int maxFieldLength) {
}
