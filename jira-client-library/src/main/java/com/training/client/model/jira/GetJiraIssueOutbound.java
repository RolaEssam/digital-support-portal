
package com.training.client.model.jira;

import com.training.client.core.AbstractOutboundRequestBuilder;
import com.training.client.core.ApiConstants;
import com.training.client.core.HttpMethod;

import java.util.Map;

public class GetJiraIssueOutbound
        extends AbstractOutboundRequestBuilder<JiraIssueResponse> {

    private final String issueKey;

    public GetJiraIssueOutbound(String issueKey) {

        if (issueKey == null || issueKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Jira issue key must not be empty"
            );
        }

        this.issueKey = issueKey;
    }

    @Override
    protected String getBasePath() {
        return ApiConstants.GET_ISSUE.replace(
                "{issue-key}",
                encodePathParam(issueKey)
        );
    }

    @Override
    protected Map<String, String> getQueryParams() {
        return Map.of();
    }

    @Override
    protected HttpMethod getHttpMethod() {
        return HttpMethod.GET;
    }

    @Override
    public Class<JiraIssueResponse> getResponseType() {
        return JiraIssueResponse.class;
    }
}
