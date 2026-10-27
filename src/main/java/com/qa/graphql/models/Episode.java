package com.qa.graphql.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Episode(String id, String name, @JsonProperty("air_date") String airDate, String episode) {
}
