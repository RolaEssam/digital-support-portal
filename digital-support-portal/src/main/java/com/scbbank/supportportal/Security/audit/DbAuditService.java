package com.scbbank.supportportal.Security.audit;

import com.scbbank.supportportal.Security.audit.AuditEvent;
import com.scbbank.supportportal.entity.AuditEventEntity;
import com.scbbank.supportportal.repository.AuditEventRepository;
import com.scbbank.supportportal.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Writes every audit event to the audit_event table and also to the AUDIT log.
 * A database failure must never change the response the caller gets (still 403),
 * so errors are logged and swallowed here.
 */
@Service
public class DbAuditService  {

    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");
    private static final Logger LOG = LoggerFactory.getLogger(DbAuditService.class);

    private final AuditEventRepository repository;
    private final UserRepository users;

    public DbAuditService(AuditEventRepository repository, UserRepository users) {
        this.repository = repository;
        this.users = users;
    }


    public void record(AuditEvent e) {
        AUDIT.warn("action={} user={} role={} endpoint={} time={}",
                e.action(), clean(e.userId()), clean(e.role()), clean(e.endpoint()), e.timestamp());

        try {
            Long userId = parseId(e.userId());

            AuditEventEntity row = new AuditEventEntity();
            row.setAction(e.action().name());
            row.setUserId(userId);
            row.setUsername(userId == null ? null
                    : users.findById(userId).map(u -> u.getUsername()).orElse(null));
            row.setRole(cut(clean(e.role()), 20));
            row.setResource(cut(clean(e.endpoint()), 255));
            row.setOutcome("DENIED");   // only ACCESS_DENIED exists today; change when you add more actions

            row.setCreatedAt(e.timestamp() != null ? e.timestamp() : Instant.now());
            repository.save(row);
        } catch (Exception ex) {
            LOG.error("Could not save audit event to the database: action={} endpoint={}",
                    e.action(), clean(e.endpoint()), ex);
        }
    }

    /** The JWT subject is the numeric user id. Anything else is stored as null. */
    private static Long parseId(String subject) {
        try {
            return subject == null ? null : Long.valueOf(subject);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** Stops a value from adding fake lines to the log. Null stays null for the database. */
    private static String clean(String value) {
        return value == null ? null : value.replaceAll("[\\r\\n\\t]", "_");
    }

    private static String cut(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}