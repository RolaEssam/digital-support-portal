
package com.training.client.oauth;

public interface JiraTokenStore {

    StoredJiraTokens load();

    void save(StoredJiraTokens tokens);
}

