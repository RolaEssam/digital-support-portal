package com.training.client.core;

/**
 * Contract every API call implements: how to build the request, and what type the response is.
 */
public interface OutboundRequestGenerator<T> {
    ServiceRequestPayload generateRequest(ClientConfig clientConfig);
    Class<T> getResponseType();
}
