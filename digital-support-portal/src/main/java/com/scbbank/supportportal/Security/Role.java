package com.scbbank.supportportal.Security;

import java.util.Arrays;
import java.util.Optional;

public enum Role {
    AGENT,
    ADMIN,
    REVIEWER,
    SRE,
    SYSTEM;

    /** Unknown, null or blank claim values give an empty result, which callers must treat as "deny". */
    public static Optional<Role> fromClaim(String claim) {
        if (claim == null || claim.isBlank()) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(r -> r.name().equals(claim))
                .findFirst();
    }
}