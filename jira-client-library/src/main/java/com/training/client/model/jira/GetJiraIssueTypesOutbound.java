
package com.training.client.model.jira;

import com.training.client.core.AbstractOutboundRequestBuilder;
import com.training.client.core.ApiConstants;
import com.training.client.core.HttpMethod;

import java.util.Map;

public class GetJiraIssueTypesOutbound
        extends AbstractOutboundRequestBuilder<JiraIssueTypeResponse[]> {

    @Override
    protected String getBasePath() {
        return ApiConstants.GET_ISSUE_TYPES;
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
    public Class<JiraIssueTypeResponse[]> getResponseType() {
        return JiraIssueTypeResponse[].class;
    }
}

