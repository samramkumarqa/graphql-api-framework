package com.qa.graphql.client;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

import java.util.List;
import java.util.Map;

/**
 * GraphQL-aware view of an HTTP response.
 *
 * GraphQL reports most problems inside the body (an "errors" array) rather than through HTTP status codes,
 * so tests should check both. {@link #assertOk()} does that in one call.
 */
public class GraphQLResponse {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final Response raw;
    private final JsonPath json;

    GraphQLResponse(Response raw) {
        this.raw = raw;
        this.json = raw.jsonPath();
    }

    public Response raw() {
        return raw;
    }

    public int status() {
        return raw.statusCode();
    }

    public long timeMs() {
        return raw.time();
    }

    /** Value under data, e.g. path("character.name"). */
    public <T> T path(String dataPath) {
        return json.get("data." + dataPath);
    }

    /** Object under data mapped to a type, e.g. as("character", Character.class). Returns null for a null field. */
    public <T> T as(String dataPath, Class<T> type) {
        Object value = json.get("data." + dataPath);
        return value == null ? null : MAPPER.convertValue(value, type);
    }

    /** List under data mapped to element type, e.g. asList("characters.results", Character.class). */
    public <T> List<T> asList(String dataPath, Class<T> elementType) {
        Object value = json.get("data." + dataPath);
        JavaType listType = MAPPER.getTypeFactory().constructCollectionType(List.class, elementType);
        return value == null ? null : MAPPER.convertValue(value, listType);
    }

    public boolean hasErrors() {
        List<?> errors = json.getList("errors");
        return errors != null && !errors.isEmpty();
    }

    public List<Map<String, Object>> errors() {
        List<Map<String, Object>> errors = json.getList("errors");
        return errors == null ? List.of() : errors;
    }

    public String firstErrorMessage() {
        return errors().isEmpty() ? null : String.valueOf(errors().get(0).get("message"));
    }

    @SuppressWarnings("unchecked")
    public String firstErrorCode() {
        if (errors().isEmpty()) return null;
        Object extensions = errors().get(0).get("extensions");
        return extensions instanceof Map<?, ?> map ? (String) ((Map<String, Object>) map).get("code") : null;
    }

    /** HTTP 200 and no GraphQL errors; fails with the error messages otherwise. */
    public GraphQLResponse assertOk() {
        if (status() != 200 || hasErrors()) {
            throw new AssertionError("Expected a successful GraphQL response but got HTTP " + status()
                    + " with errors: " + errors() + "\nBody: " + raw.asString());
        }
        return this;
    }
}
