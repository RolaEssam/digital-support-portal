package com.scbbank.supportportal.service.user;

import com.scbbank.supportportal.service.user.UserLookup.UserRecord;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryUserLookupTest {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final InMemoryUserLookup lookup = new InMemoryUserLookup(encoder);

    @Test
    void knownUser_isFound_withHashedPassword() {
        UserRecord u = lookup.findByUsername("mona.hassan").orElseThrow();

        assertEquals(1042L, u.id());
        assertEquals("AGENT", u.role());
        assertTrue(u.active());
        assertNotEquals("Test@1234", u.passwordHash());
        assertTrue(encoder.matches("Test@1234", u.passwordHash()));
    }

    @Test
    void unknownUser_isEmpty() {
        assertTrue(lookup.findByUsername("nobody").isEmpty());
    }

    @Test
    void toString_doesNotLeakHash() {
        UserRecord u = lookup.findByUsername("mona.hassan").orElseThrow();
        assertFalse(u.toString().contains(u.passwordHash()));
    }
}