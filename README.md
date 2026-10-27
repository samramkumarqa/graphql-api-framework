# GraphQL API Framework

[![GraphQL Tests](https://github.com/samramkumarqa/graphql-api-framework/actions/workflows/graphql-tests.yml/badge.svg)](https://github.com/samramkumarqa/graphql-api-framework/actions/workflows/graphql-tests.yml)

GraphQL API test automation in **Java 17** with **Rest Assured** and **TestNG**, against the public [Rick and Morty GraphQL API](https://rickandmortyapi.com/documentation/#graphql).

Operations live in real `.graphql` files with shared fragments, and variables are sent as JSON. No query strings are built by concatenation. A small GraphQL-aware response wrapper checks both the HTTP status and the `errors` array, because GraphQL reports most problems in the body.

## Highlights

| Area | What it shows |
| --- | --- |
| Operation files | `src/main/resources/graphql/queries/*.graphql`, with `#import` of shared fragments resolved by `OperationLoader` |
| Variables | Typed variables (`ID!`, `Int`, input objects, lists, booleans) passed as a map |
| GraphQL features | Fragments, aliases, `@include` / `@skip` directives, nested selections, introspection |
| Typed results | `response.as("character", Character.class)` maps data to Java records |
| Pagination | `info.pages == ceil(count / 20)`, next/prev links, first/last page sizes, no overlap between pages |
| Filters | Data-driven filters checked against every result (the API matches case-insensitively by substring) |
| Graph consistency | Episode cast ↔ character episodes, and location residents ↔ character location, checked both ways |
| Error handling | Syntax errors, unknown fields, missing or wrongly typed variables, depth limit; null (not error) for unknown ids |
| Contract tests | Introspection checks that fields, types and filter inputs the suite relies on still exist; JSON Schema on a full response |
| Reporting | Allure, with each GraphQL request and response attached |
| CI | Offline loader tests, then the live suite; weekday run to catch schema changes |

## How this API reports errors

| Situation | Result |
| --- | --- |
| Unknown id | HTTP 200, `data.character = null`, no errors |
| Filter or page with no matches | HTTP 200, empty `results` |
| Syntax error / unknown field / bad variable | HTTP 400, `errors[]`, no `data` |
| Query nested deeper than 4 levels | HTTP 400, `exceeds maximum operation depth of 4` |

## Project structure

```
src/main/java/com/qa/graphql/
├── client/   GraphQLClient, GraphQLRequest, GraphQLResponse, OperationLoader
├── models/   Character, Episode, Info, Ref (records)
└── config/   Config (properties → env vars → -D flags)
src/main/resources/graphql/
├── queries/    GetCharacter, ListCharacters, CompareCharacters, …
└── fragments/  CharacterFields, PageInfo
src/test/java/com/qa/tests/
├── CharacterQueryTest        lookups, data-driven, aliases, directives, selection
├── PaginationAndFilterTest   paging maths, filters, empty results, episodes by season
├── RelationshipTest          graph consistency
├── ErrorHandlingTest         GraphQL errors and nulls
├── SchemaContractTest        introspection contract
└── OperationLoaderTest       offline unit tests
```

## Running

Requires Java 17+ and Maven.

```bash
mvn test                     # full live regression
mvn test -Dsuite=smoke       # smoke group
mvn test -Dsuite=offline     # framework unit tests, no network
mvn allure:serve             # open the Allure report
```

Point at another deployment with `-Dgraphql.url=…` or `GRAPHQL_URL`.
