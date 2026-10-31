package com.scbbank.supportportal.Security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AC4: one test per row/role of the permission matrix in the Features doc.
 * EXPECTED is written by hand from the doc on purpose. If it were built from the enum,
 * the test could never fail. When the doc changes, change BOTH this table and Permission.
 */
class PermissionMatrixTest {

    private static final Map<Permission, Set<Role>> EXPECTED = Map.ofEntries(
            Map.entry(Permission.CALL_API,        EnumSet.of(Role.AGENT)),
            Map.entry(Permission.CALL_WRITE,      EnumSet.of(Role.AGENT)),
            Map.entry(Permission.CUSTOMER_API,    EnumSet.of(Role.AGENT)),
            Map.entry(Permission.TICKET_API,      EnumSet.of(Role.AGENT)),
            Map.entry(Permission.TICKET_WRITE,    EnumSet.of(Role.AGENT)),
            Map.entry(Permission.AGENT_GUIDE_API, EnumSet.of(Role.AGENT)),
            Map.entry(Permission.GUIDE_READ,      EnumSet.of(Role.ADMIN, Role.REVIEWER)),
            Map.entry(Permission.REVIEW_API,      EnumSet.of(Role.ADMIN, Role.REVIEWER)),
            Map.entry(Permission.GUIDE_WRITE,     EnumSet.of(Role.ADMIN)),
            Map.entry(Permission.RULES_API,       EnumSet.of(Role.ADMIN)),
            Map.entry(Permission.RULES_WRITE,     EnumSet.of(Role.ADMIN)),
            Map.entry(Permission.TRIAGE_API,      EnumSet.of(Role.ADMIN)),
            Map.entry(Permission.TRIAGE_WRITE,    EnumSet.of(Role.ADMIN)),
            Map.entry(Permission.ANALYTICS_API,   EnumSet.of(Role.ADMIN)),
            Map.entry(Permission.AUDIT_API,       EnumSet.of(Role.ADMIN)),
            Map.entry(Permission.REVIEW_DECIDE,   EnumSet.of(Role.REVIEWER)));

    static Stream<Object[]> everyCell() {
        return Stream.of(Permission.values())
                .flatMap(p -> Stream.of(Role.values()).map(r -> new Object[]{p, r}));
    }

    @Test
    void everyPermissionHasARowInTheTable() {
        // Fails when someone adds a Permission without adding it to the matrix test.
        assertEquals(EnumSet.allOf(Permission.class), EXPECTED.keySet());
    }

    @ParameterizedTest(name = "{0} for {1}")
    @MethodSource("everyCell")
    void matrixCell(Permission permission, Role role) {
        boolean expected = EXPECTED.get(permission).contains(role);
        assertEquals(expected, permission.isAllowedFor(role),
                permission + " for " + role + " should be " + (expected ? "allowed" : "denied"));
    }

    @Test
    void sreAndSystemHaveNoPortalPermission() {
        for (Permission p : Permission.values()) {
            assertTrue(!p.isAllowedFor(Role.SRE), p + " must be denied for SRE");
            assertTrue(!p.isAllowedFor(Role.SYSTEM), p + " must be denied for SYSTEM");
        }
    }

    @Test
    void agentIsDeniedEveryGuideAdminApi() {   // AC1
        for (Permission p : Set.of(Permission.GUIDE_READ, Permission.GUIDE_WRITE, Permission.REVIEW_API,
                Permission.RULES_API, Permission.TRIAGE_API, Permission.ANALYTICS_API, Permission.AUDIT_API)) {
            assertTrue(!p.isAllowedFor(Role.AGENT), p + " must be denied for AGENT");
        }
    }

    @Test
    void adminAndReviewerAreDeniedCustomerAndTicketApis() {   // AC2
        for (Role r : Set.of(Role.ADMIN, Role.REVIEWER)) {
            assertTrue(!Permission.CUSTOMER_API.isAllowedFor(r));
            assertTrue(!Permission.TICKET_API.isAllowedFor(r));
        }
    }
    @Test
    void reviewerCanReadButNeverChangeGuides() {
        assertTrue(Permission.GUIDE_READ.isAllowedFor(Role.REVIEWER));
        assertTrue(!Permission.GUIDE_WRITE.isAllowedFor(Role.REVIEWER), "reviewer must not write guides");
        assertTrue(Permission.GUIDE_WRITE.isAllowedFor(Role.ADMIN));
    }

    @Test
    void nullRoleIsNeverAllowed() {
        for (Permission p : Permission.values()) {
            assertTrue(!p.isAllowedFor(null));
        }
    }

    @Test
    void roleClaimParsing() {
        assertEquals(Role.AGENT, Role.fromClaim("AGENT").orElseThrow());
        assertTrue(Role.fromClaim("agent").isEmpty(), "claim is case-sensitive");
        assertEquals(Role.ADMIN, Role.fromClaim("ADMIN").orElseThrow());
        assertTrue(Role.fromClaim("GUIDE_ADMIN").isEmpty(), "unknown roles are not mapped");
        assertTrue(Role.fromClaim(null).isEmpty());
        assertTrue(Role.fromClaim(" ").isEmpty());
    }
}
