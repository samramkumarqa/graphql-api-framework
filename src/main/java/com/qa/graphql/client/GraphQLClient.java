package com.qa.graphql.client;

import com.qa.graphql.config.Config;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.EncoderConfig;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * Sends GraphQL operations over HTTP POST. Operations come from .graphql files (see {@link OperationLoader})
 * or raw strings for negative tests; variables are passed as a map and serialised by Jackson,
 * so no query string is ever built by concatenation.
 */
public class GraphQLClient {

    private final RequestSpecification spec;

    public GraphQLClient() {
        this(Config.get("graphql.url"));
    }

    public GraphQLClient(String endpoint) {
        this.spec = new RequestSpecBuilder()
                .setBaseUri(endpoint)
                .setContentType(ContentType.JSON)
                .setAccept("application/json")
                .setConfig(RestAssuredConfig.config()
                        .httpClient(HttpClientConfig.httpClientConfig()
                                .setParam("http.connection.timeout", Config.getInt("connect.timeout.ms"))
                                .setParam("http.socket.timeout", Config.getInt("read.timeout.ms")))
                        .encoderConfig(EncoderConfig.encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false)))
                .addFilter(new AllureRestAssured())
                .build();
    }

    /** Runs a named operation file, e.g. run("GetCharacter", Map.of("id", 1)). */
    public GraphQLResponse run(String operationName, Map<String, Object> variables) {
        return send(new GraphQLRequest(OperationLoader.operation(operationName), variables, operationName));
    }

    public GraphQLResponse run(String operationName) {
        return run(operationName, null);
    }

    /** Sends a raw query string, for ad-hoc and negative tests. */
    public GraphQLResponse query(String query, Map<String, Object> variables) {
        return send(new GraphQLRequest(query, variables, null));
    }

    public GraphQLResponse query(String query) {
        return query(query, null);
    }

    /**
     * The public endpoint sits behind a rate-limiting gateway (Stellate) that answers 429 with a
     * "retryAfter" in seconds. Wait that long and retry, a few times at most, as a well-behaved client should.
     */
    private GraphQLResponse send(GraphQLRequest request) {
        GraphQLResponse response = new GraphQLResponse(given(spec).body(request).when().post());
        for (int attempt = 1; attempt <= MAX_RATE_LIMIT_RETRIES && response.status() == 429; attempt++) {
            sleepSeconds(retryAfterSeconds(response));
            response = new GraphQLResponse(given(spec).body(request).when().post());
        }
        return response;
    }

    private static final int MAX_RATE_LIMIT_RETRIES = 3;

    /** Retry-After header if present, else the gateway's extensions.stellate.details.rules[0].retryAfter, else 5s. */
    static int retryAfterSeconds(GraphQLResponse response) {
        String header = response.raw().getHeader("Retry-After");
        if (header != null && header.trim().matches("\\d+")) {
            return Integer.parseInt(header.trim());
        }
        Object fromBody = response.raw().jsonPath().get("errors[0].extensions.stellate.details.rules[0].retryAfter");
        if (fromBody instanceof Number n) {
            return n.intValue();
        }
        return 5;
    }

    private static void sleepSeconds(int seconds) {
        try {
            Thread.sleep(Math.min(Math.max(seconds, 1), 30) * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting out a rate limit", e);
        }
    }
}
