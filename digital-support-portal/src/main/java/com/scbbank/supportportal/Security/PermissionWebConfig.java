package com.scbbank.supportportal.Security;

import com.scbbank.supportportal.config.SecurityConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class PermissionWebConfig implements WebMvcConfigurer {

    private final PermissionInterceptor permissionInterceptor;

    public PermissionWebConfig(PermissionInterceptor permissionInterceptor) {
        this.permissionInterceptor = permissionInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Same public area as SecurityConfig.PUBLIC_PATHS (login, key exchange).
        registry.addInterceptor(permissionInterceptor)
                .addPathPatterns("/api/v1/**")
                .excludePathPatterns(SecurityConfig.PUBLIC_AUTH_PATHS);
    }
}
