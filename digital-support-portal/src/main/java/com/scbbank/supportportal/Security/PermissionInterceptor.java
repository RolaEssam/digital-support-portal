package com.scbbank.supportportal.Security;

import com.scbbank.supportportal.Security.audit.AuditAction;
import com.scbbank.supportportal.Security.audit.AuditEvent;
import com.scbbank.supportportal.auth.exception.AuthException;
import com.scbbank.supportportal.auth.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

import com.scbbank.supportportal.Security.audit.AuditAction;
import com.scbbank.supportportal.Security.audit.AuditEvent;
import com.scbbank.supportportal.Security.audit.DbAuditService;


@Component
public class PermissionInterceptor implements HandlerInterceptor {

    private static final String CLAIM_ROLE = "role";

    private final DbAuditService auditService;

    public PermissionInterceptor(DbAuditService auditService) {
        this.auditService = auditService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true; // static resources etc., not an API handler
        }

        RequiresPermission required = AnnotatedElementUtils.findMergedAnnotation(
                handlerMethod.getMethod(), RequiresPermission.class);
        if (required == null) {
            required = AnnotatedElementUtils.findMergedAnnotation(
                    handlerMethod.getBeanType(), RequiresPermission.class);
        }

        String userId = null;
        String roleClaim = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            userId = jwtAuth.getToken().getSubject();
            roleClaim = jwtAuth.getToken().getClaimAsString(CLAIM_ROLE);
        }

        // No annotation = deny. A forgotten annotation must never open an endpoint.
        boolean allowed = required != null && isAllowed(roleClaim, required);
        if (!allowed) {
            auditService.record(new AuditEvent(
                    AuditAction.ACCESS_DENIED,
                    userId,
                    roleClaim,
                    request.getMethod() + " " + request.getRequestURI(),
                    Instant.now()));
            throw new AuthException(ErrorCode.ACCESS_DENIED);
        }
        return true;
    }

    private static boolean isAllowed(String roleClaim, RequiresPermission required) {
        Optional<Role> role = Role.fromClaim(roleClaim);
        return role.isPresent()
                && Arrays.stream(required.value()).anyMatch(p -> p.isAllowedFor(role.get()));
    }
}
