package com.qa.graphql.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** A nested object where only a few fields were selected, e.g. origin { id name }. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Ref(String id, String name, String episode) {
}
