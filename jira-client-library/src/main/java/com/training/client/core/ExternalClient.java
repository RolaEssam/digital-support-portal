package com.training.client.core;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.training.client.core.exception.BusinessException;
import com.training.client.core.exception.CustomException;
import com.training.client.core.exception.DownstreamTimeoutException;
import com.training.client.core.exception.SystemException;
import lombok.extern.slf4j.Slf4j;

import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

/**
 * The single entry point that executes ANY outbound request.
 * Flow: build payload -> send HTTP -> 2xx map to response type / 4xx BusinessException / 5xx SystemException.
 */
@Slf4j
public class ExternalClient {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private ExternalClient() {
    }

    public static <T> T execute(OutboundRequestGenerator<T> request, ClientConfig config) {
        ServiceRequestPayload payload = request.generateRequest(config);
        String requestId = payload.getHeaders().get("X-Request-ID").toString();
        String fullUrl = config.getUrl() + "/" + payload.getPath();

        try {
            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(config.getConnectionTimeout()))
                    .build();
            HttpRequest httpRequest = buildHttpRequest(payload, fullUrl, config);

            long startTime = System.currentTimeMillis();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            log.info("Time taken by {} API {} is {} ms", ClientConfig.SERVICE_NAME, payload.getMethod() + "#" + fullUrl, System.currentTimeMillis() - startTime);

            int statusCode = response.statusCode();
            String responseBody = response.body() == null || response.body().isBlank() ? "{}" : response.body();

            if (statusCode >= 200 && statusCode < 300) {
                return MAPPER.readValue(responseBody, request.getResponseType());
            }
            log.error("{} API {} failed with status '{}' and body '{}'", ClientConfig.SERVICE_NAME, fullUrl, statusCode, responseBody);
            if (statusCode >= 400 && statusCode < 500) {
                throw new BusinessException(requestId, statusCode, responseBody);
            }
            if (statusCode >= 500) {
                throw new SystemException(requestId, statusCode, responseBody);
            }
            throw new CustomException(requestId, "Unexpected response with status: " + statusCode);

        } catch (CustomException e) {
            throw e;
        } catch (HttpConnectTimeoutException | ConnectException e) {
            throw new DownstreamTimeoutException(requestId, "CONNECTION_TIMEOUT", "Cannot reach " + fullUrl);
        } catch (HttpTimeoutException e) {
            throw new DownstreamTimeoutException(requestId, "SOCKET_TIMEOUT", "No response from " + fullUrl);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CustomException(requestId, e.getMessage());
        } catch (Exception e) {
            log.error("Error calling {} API {}", ClientConfig.SERVICE_NAME, fullUrl, e);
            throw new CustomException(requestId, e.getMessage());
        }
    }

    private static HttpRequest buildHttpRequest(ServiceRequestPayload payload, String fullUrl, ClientConfig config) throws Exception {
        HttpRequest.BodyPublisher bodyPublisher = payload.getBody() == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(payload.getBody()));

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                .timeout(Duration.ofSeconds(config.getSocketTimeout()))
                .method(payload.getMethod().name(), bodyPublisher);
        payload.getHeaders().forEach((key, value) -> builder.header(key, value.toString()));
        return builder.build();
    }
}
