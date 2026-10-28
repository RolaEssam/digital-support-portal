
package com.training.client.service;

import com.training.client.core.ClientConfig;
import com.training.client.core.ExternalClient;
import com.training.client.model.jira.CreateJiraIssueOutbound;
import com.training.client.model.jira.GetJiraIssueOutbound;
import com.training.client.model.jira.GetJiraIssueWithFieldsOutbound;
import com.training.client.model.jira.GetJiraMyselfOutbound;
import com.training.client.model.jira.GetJiraIssueTypesOutbound;
import com.training.client.model.jira.JiraCreateIssueRequest;
import com.training.client.model.jira.JiraCreateIssueResponse;
import com.training.client.model.jira.JiraIssueResponse;
import com.training.client.model.jira.JiraIssueTypeResponse;
import com.training.client.model.jira.JiraUserResponse;
import com.training.client.oauth.JiraTokenManager;

import java.util.Map;
import java.util.Objects;

public class JiraClientService {

    private final String cloudId;
    private final JiraTokenManager tokenManager;

    public JiraClientService(
            String cloudId,
            JiraTokenManager tokenManager
    ) {
        if (cloudId == null || cloudId.isBlank()) {
            throw new IllegalArgumentException(
                    "Jira Cloud ID is required"
            );
        }

        this.cloudId = cloudId;
        this.tokenManager = Objects.requireNonNull(tokenManager);
    }

    private ClientConfig buildConfig() {

        String accessToken = tokenManager.getValidAccessToken();

        return ClientConfig.builder()
                .url("https://api.atlassian.com/ex/jira/" + cloudId)
                .headers(Map.of(
                        "Authorization", "Bearer " + accessToken,
                        "Accept", "application/json"
                ))
                .connectionTimeout(10)
                .socketTimeout(30)
                .build();
    }

    public JiraUserResponse getCurrentUser() {
        return ExternalClient.execute(
                new GetJiraMyselfOutbound(),
                buildConfig()
        );
    }

    public JiraIssueTypeResponse[] getIssueTypes() {
        return ExternalClient.execute(
                new GetJiraIssueTypesOutbound(),
                buildConfig()
        );
    }

    public JiraIssueResponse getIssue(String issueKey) {
        return ExternalClient.execute(
                new GetJiraIssueOutbound(issueKey),
                buildConfig()
        );
    }

    public JiraIssueResponse getIssueWithFields(
            String issueKey,
            String fields
    ) {
        return ExternalClient.execute(
                new GetJiraIssueWithFieldsOutbound(issueKey, fields),
                buildConfig()
        );
    }

    public JiraCreateIssueResponse createIssue(
            String projectKey,
            String issueTypeId,
            String summary,
            String description
    ) {
        Map<String, Object> requestBody =
                JiraCreateIssueRequest.build(
                        projectKey,
                        issueTypeId,
                        summary,
                        description
                );

        return ExternalClient.execute(
                new CreateJiraIssueOutbound(requestBody),
                buildConfig()
        );
    }
}

