
package com.training.client.model.jira;

import com.training.client.core.AbstractOutboundRequestBuilder;
import com.training.client.core.ApiConstants;
import com.training.client.core.HttpMethod;

import java.util.Map;

public class GetJiraIssueWithFieldsOutbound
        extends AbstractOutboundRequestBuilder<JiraIssueResponse> {

    private final String issueKey;
    private final String fields;

    public GetJiraIssueWithFieldsOutbound(
            String issueKey,
            String fields
    ) {
        if (issueKey == null || issueKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Jira issue key is required"
            );
        }

        if (fields == null || fields.isBlank()) {
            throw new IllegalArgumentException(
                    "At least one Jira field is required"
            );
        }

        this.issueKey = issueKey;
        this.fields = fields;
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
        return Map.of("fields", fields);
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
