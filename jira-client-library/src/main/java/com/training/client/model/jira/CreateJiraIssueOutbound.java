
package com.training.client.model.jira;

import com.training.client.core.AbstractOutboundRequestBuilder;
import com.training.client.core.ApiConstants;
import com.training.client.core.HttpMethod;

import java.util.Map;
import java.util.Objects;

public class CreateJiraIssueOutbound
        extends AbstractOutboundRequestBuilder<JiraCreateIssueResponse> {

    private final Map<String, Object> requestBody;

    public CreateJiraIssueOutbound(
            Map<String, Object> requestBody
    ) {
        this.requestBody = Objects.requireNonNull(
                requestBody, "Jira request body is required"
        );
    }

    @Override
    protected String getBasePath() {
        return ApiConstants.CREATE_ISSUE;
    }

    @Override
    protected Map<String, String> getQueryParams() {
        return Map.of();
    }

    @Override
    protected HttpMethod getHttpMethod() {
        return HttpMethod.POST;
    }

    @Override
    protected Object getBody() {
        return requestBody;
    }

    @Override
    public Class<JiraCreateIssueResponse> getResponseType() {
        return JiraCreateIssueResponse.class;
    }
}
