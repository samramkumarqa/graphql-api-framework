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

    private GraphQLResponse send(GraphQLRequest request) {
        return new GraphQLResponse(given(spec).body(request).when().post());
    }
}
