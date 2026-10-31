package com.scbbank.supportportal.Security.audit;

import java.time.Instant;

public record AuditEvent(AuditAction action,
                         String userId,
                         String role,
                         String endpoint,
                         Instant timestamp) {
}