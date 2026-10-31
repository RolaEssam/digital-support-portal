package com.scbbank.supportportal.Security;

import com.scbbank.supportportal.Security.audit.AuditAction;
import com.scbbank.supportportal.Security.audit.AuditEvent;
import com.scbbank.supportportal.Security.audit.DbAuditService;
import com.scbbank.supportportal.auth.exception.AuthException;
import com.scbbank.supportportal.auth.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.method.HandlerMethod;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Plain unit tests (no Spring context), like the rest of this project. */
class PermissionInterceptorTest {

    private final List<AuditEvent> audited = new ArrayList<>();
    // Fake audit service: collects events in the list instead of writing to the database.
    private final DbAuditService fakeAudit = new DbAuditService(null, null) {
        @Override
        public void record(AuditEvent e) {
            audited.add(e);
        }
    };
    private final PermissionInterceptor interceptor = new PermissionInterceptor(fakeAudit);

    @BeforeEach
    void clear() {
        audited.clear();
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    // ---- fake controllers --------------------------------------------------------------

    static class Fake {
        @RequiresPermission(Permission.GUIDE_READ) public void guides() { }
        @RequiresPermission(Permission.GUIDE_WRITE) public void guideWrite() { }
        @RequiresPermission(Permission.TICKET_API) public void tickets() { }
        @RequiresPermission({Permission.TICKET_API, Permission.REVIEW_API}) public void either() { }
        public void forgotten() { }
    }

    @RequiresPermission(Permission.AUDIT_API)
    static class ClassLevel {
        public void inherits() { }
        @RequiresPermission(Permission.TICKET_API) public void overrides() { }
    }

    // ---- helpers ------------------------------------------------------------------------

    private static HandlerMethod handler(Object bean, String method) throws Exception {
        return new HandlerMethod(bean, bean.getClass().getMethod(method));
    }

    private static void loginAs(String userId, String role) {
        Jwt.Builder b = Jwt.withTokenValue("t").header("alg", "RS256").subject(userId);
        if (role != null) {
            b.claim("role", role);
        } else {
            b.claim("x", "y");
        }
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(b.build()));
    }

    private boolean call(Object bean, String method) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/guides");
        return interceptor.preHandle(req, new MockHttpServletResponse(), handler(bean, method));
    }

    private void assertDenied(Object bean, String method) {
        AuthException ex = assertThrows(AuthException.class, () -> call(bean, method));
        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
    }

    // ---- AC1 ----------------------------------------------------------------------------

    @Test
    void agentCallingGuideAdminApiIsDeniedAndAudited() {
        loginAs("1042", "AGENT");
        assertDenied(new Fake(), "guides");

        assertEquals(1, audited.size());
        AuditEvent e = audited.get(0);
        assertEquals(AuditAction.ACCESS_DENIED, e.action());
        assertEquals("1042", e.userId());
        assertEquals("AGENT", e.role());
        assertEquals("GET /api/v1/guides", e.endpoint());
        assertNotNull(e.timestamp());
    }
    @Test
    void reviewerCanReadGuidesButWritingIsDeniedAndAudited() throws Exception {
        loginAs("1044", "REVIEWER");
        assertTrue(call(new Fake(), "guides"));
        assertTrue(audited.isEmpty());

        assertDenied(new Fake(), "guideWrite");
        assertEquals(1, audited.size());
        assertEquals("REVIEWER", audited.get(0).role());
    }
    @Test
    void guideAdminCanCallGuideApi() throws Exception {
        loginAs("1043", "ADMIN");
        assertTrue(call(new Fake(), "guides"));
        assertTrue(call(new Fake(), "guideWrite"));
        assertTrue(audited.isEmpty(), "allowed calls are not audited as denied");
    }

    // ---- AC2 ----------------------------------------------------------------------------

    @Test
    void guideAdminAndReviewerCallingTicketApiAreDeniedAndAudited() {
        loginAs("1043", "ADMIN");
        assertDenied(new Fake(), "tickets");
        loginAs("1044", "REVIEWER");
        assertDenied(new Fake(), "tickets");
        assertEquals(2, audited.size());
    }

    @Test
    void agentCanCallTicketApi() throws Exception {
        loginAs("1042", "AGENT");
        assertTrue(call(new Fake(), "tickets"));
    }

    // ---- annotation rules -----------------------------------------------------------------

    @Test
    void anyOfSeveralPermissionsIsEnough() throws Exception {
        loginAs("1042", "AGENT");
        assertTrue(call(new Fake(), "either"));
        loginAs("1044", "REVIEWER");
        assertTrue(call(new Fake(), "either"));
        loginAs("1043", "ADMIN");
        assertTrue(call(new Fake(), "either"));
    }

    @Test
    void endpointWithoutAnnotationIsDeniedForEveryRole() {
        for (Role r : Role.values()) {
            loginAs("1", r.name());
            assertDenied(new Fake(), "forgotten");
        }
    }

    @Test
    void classLevelAnnotationAppliesAndMethodLevelWins() throws Exception {
        loginAs("1043", "ADMIN");
        assertTrue(call(new ClassLevel(), "inherits"));
        assertDenied(new ClassLevel(), "overrides");   // method says TICKET_API, admin is not allowed

        loginAs("1042", "AGENT");
        assertTrue(call(new ClassLevel(), "overrides"));
        assertDenied(new ClassLevel(), "inherits");
    }

    // ---- bad tokens that reached the interceptor -------------------------------------------

    @Test
    void unknownRoleIsDenied() {
        loginAs("9", "SUPERUSER");
        assertDenied(new Fake(), "tickets");
    }

    @Test
    void tokenWithoutRoleClaimIsDenied() {
        loginAs("9", null);
        assertDenied(new Fake(), "tickets");
    }

    @Test
    void noAuthenticationIsDenied() {
        assertDenied(new Fake(), "tickets");
        assertNotNull(audited.get(0));
    }

    @Test
    void nonHandlerMethodsAreIgnored() throws Exception {
        assertTrue(interceptor.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), new Object()));
    }
}