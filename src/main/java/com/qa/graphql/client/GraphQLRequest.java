package com.qa.graphql.client;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/** The standard GraphQL-over-HTTP request body. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GraphQLRequest(String query, Map<String, Object> variables, String operationName) {
}
