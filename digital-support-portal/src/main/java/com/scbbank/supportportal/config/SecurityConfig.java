package com.scbbank.supportportal.config;

import com.scbbank.supportportal.Security.ApiAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    public static final String[] PUBLIC_AUTH_PATHS = {
            "/api/v1/auth/public-key",
            "/api/v1/auth/login"
    };
    /** Paths that need no access token. Spring matches them without the context path. */
    private static final String[] PUBLIC_PATHS = {
            PUBLIC_AUTH_PATHS[0],
            PUBLIC_AUTH_PATHS[1],
            "/api/v1/auth/**",
            "/error",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**"
    };

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /** Reads the access token from the Authorization header or the DSP_AT cookie. */
    @Bean
    public BearerTokenResolver bearerTokenResolver() {
        return new CookieBearerTokenResolver();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   BearerTokenResolver bearerTokenResolver,
                                                   ApiAuthenticationEntryPoint authEntryPoint) throws Exception {
        http
                // Tokens are validated on every request; there is no server-side session.
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .anyRequest().authenticated())

                .exceptionHandling(e -> e.authenticationEntryPoint(authEntryPoint))
                .oauth2ResourceServer(o -> o
                        .authenticationEntryPoint(authEntryPoint)
                        .bearerTokenResolver(bearerTokenResolver)
                        .jwt(Customizer.withDefaults()));
        return http.build();
    }
}