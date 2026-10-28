
package com.training.client.oauth;

import java.util.Objects;

public class InMemoryJiraTokenStore implements JiraTokenStore {

    private StoredJiraTokens tokens;

    public InMemoryJiraTokenStore(StoredJiraTokens initialTokens) {
        this.tokens = Objects.requireNonNull(initialTokens);
    }

    @Override
    public synchronized StoredJiraTokens load() {
        return tokens;
    }

    @Override
    public synchronized void save(StoredJiraTokens newTokens) {
        this.tokens = Objects.requireNonNull(newTokens);
    }
}
