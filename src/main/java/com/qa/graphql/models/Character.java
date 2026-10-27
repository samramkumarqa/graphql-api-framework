package com.qa.graphql.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** Fields not selected by a query are simply null. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Character(
        String id,
        String name,
        String status,
        String species,
        String type,
        String gender,
        String image,
        String created,
        Ref origin,
        Ref location,
        List<Ref> episode) {
}
