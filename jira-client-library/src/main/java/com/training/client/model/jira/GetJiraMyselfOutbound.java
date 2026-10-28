
package com.training.client.model.jira;

import com.training.client.core.AbstractOutboundRequestBuilder;
import com.training.client.core.ApiConstants;
import com.training.client.core.HttpMethod;

import java.util.Map;

public class GetJiraMyselfOutbound
        extends AbstractOutboundRequestBuilder<JiraUserResponse> {

    @Override
    protected String getBasePath() {
        return ApiConstants.GET_MYSELF;
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
    public Class<JiraUserResponse> getResponseType() {
        return JiraUserResponse.class;
    }
}
