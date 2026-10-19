package com.scbbank.supportportal.service.user;

import java.util.Optional;

public interface UserLookup {

    Optional<UserRecord> findByUsername(String username);

    record UserRecord(Long id, String username, String passwordHash, String role, boolean active) {
        @Override
        public String toString() {   // never print the hash
            return "UserRecord[id=" + id + ", username=" + username + ", role=" + role + "]";
        }
    }
}