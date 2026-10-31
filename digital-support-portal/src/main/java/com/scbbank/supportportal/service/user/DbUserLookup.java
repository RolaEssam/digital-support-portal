package com.scbbank.supportportal.service.user;

import com.scbbank.supportportal.repository.UserRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class DbUserLookup implements UserLookup {

    private final UserRepository users;

    public DbUserLookup(UserRepository users) {
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserRecord> findByUsername(String username) {
        return users.findByUsername(username)
                .map(u -> new UserRecord(
                        u.getId(),
                        u.getUsername(),
                        u.getPasswordHash(),
                        u.getRole().name(),true));

    }
}