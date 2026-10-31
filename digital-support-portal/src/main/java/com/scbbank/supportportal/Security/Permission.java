package com.scbbank.supportportal.Security;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum Permission {

    // ---------- Agent ----------
    CALL_API(Role.AGENT),
    CALL_WRITE(Role.AGENT),
    CUSTOMER_API(Role.AGENT),
    TICKET_API(Role.AGENT),
    TICKET_WRITE(Role.AGENT),
    AGENT_GUIDE_API(Role.AGENT),

    // ---------- Admin + Reviewer ----------
    GUIDE_READ(Role.ADMIN, Role.REVIEWER),
    REVIEW_API(Role.ADMIN, Role.REVIEWER),

    // ---------- Admin only ----------
    GUIDE_WRITE(Role.ADMIN),
    RULES_API(Role.ADMIN),
    RULES_WRITE(Role.ADMIN),
    TRIAGE_API(Role.ADMIN),
    TRIAGE_WRITE(Role.ADMIN),
    ANALYTICS_API(Role.ADMIN),
    AUDIT_API(Role.ADMIN),

    // ---------- Reviewer only ----------
    REVIEW_DECIDE(Role.REVIEWER);

    private final Set<Role> allowedRoles;

    Permission(Role... roles) {
        EnumSet<Role> set = EnumSet.noneOf(Role.class);
        set.addAll(Arrays.asList(roles));
        this.allowedRoles = Collections.unmodifiableSet(set);
    }

    public Set<Role> getAllowedRoles() {
        return allowedRoles;
    }

    public boolean isAllowedFor(Role role) {
        return role != null && allowedRoles.contains(role);
    }
}