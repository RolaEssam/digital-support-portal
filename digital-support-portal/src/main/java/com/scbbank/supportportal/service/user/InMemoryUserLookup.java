package com.scbbank.supportportal.service.user;

import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Profile("local")
@Component
public class InMemoryUserLookup implements UserLookup {

    private final Map<String, UserRecord> users;

    public InMemoryUserLookup(PasswordEncoder encoder) {
        this.users = Map.of(
                "mona.hassan", new UserRecord(1042L, "mona.hassan", encoder.encode("Test@1234"), "AGENT", true),
                "ahmed.ali",   new UserRecord(1043L, "ahmed.ali",   encoder.encode("Test@1234"), "ADMIN", true));
    }

    @Override
    public Optional<UserRecord> findByUsername(String username) {
        return Optional.ofNullable(users.get(username));
    }
}
