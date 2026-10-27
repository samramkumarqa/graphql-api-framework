package com.qa.tests;

import com.qa.graphql.client.GraphQLClient;
import com.qa.graphql.client.GraphQLResponse;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

/**
 * GraphQL errors. This API (Apollo Server) rejects invalid documents and variables with HTTP 400 and an
 * "errors" array before running anything; a missing record is not an error at all, just null.
 */
public class ErrorHandlingTest {

    private final GraphQLClient graphql = new GraphQLClient();

    @Test(groups = "smoke", description = "Unknown id returns null data and no errors")
    public void unknownIdIsNull() {
        GraphQLResponse response = graphql.query("query($id: ID!) { character(id: $id) { id name } }",
                Map.of("id", 999_999)).assertOk();
        Assert.assertNull(response.path("character"));
    }

    @Test(description = "Unknown ids are dropped from charactersByIds")
    public void unknownIdsDroppedFromList() {
        List<Map<String, Object>> found = graphql.run("CharactersByIds", Map.of("ids", List.of(1, 999_999)))
                .assertOk().path("charactersByIds");
        Assert.assertEquals(found.size(), 1);
        Assert.assertEquals(found.get(0).get("id"), "1");
    }

    @Test(description = "Syntax error: 400 with a parse error")
    public void syntaxError() {
        GraphQLResponse response = graphql.query("{ character(id: 1) { name ");
        Assert.assertEquals(response.status(), 400);
        assertThat(response.firstErrorMessage(), containsString("Syntax Error"));
        Assert.assertNull(response.raw().jsonPath().get("data"));
    }

    @Test(description = "Unknown field: 400 with a validation error naming the field")
    public void unknownField() {
        GraphQLResponse response = graphql.query("{ character(id: 1) { name superpower } }");
        Assert.assertEquals(response.status(), 400);
        assertThat(response.firstErrorMessage(), containsString("Cannot query field \"superpower\" on type \"Character\""));
        Assert.assertEquals(response.firstErrorCode(), "GRAPHQL_VALIDATION_FAILED");
    }

    @Test(description = "Missing required variable: 400")
    public void missingRequiredVariable() {
        GraphQLResponse response = graphql.run("GetCharacter", Map.of());
        Assert.assertEquals(response.status(), 400);
        assertThat(response.firstErrorMessage(), containsString("$id"));
    }

    @Test(description = "Variable of the wrong type: 400")
    public void wrongVariableType() {
        GraphQLResponse response = graphql.run("ListCharacters", Map.of("page", "not-a-number"));
        Assert.assertEquals(response.status(), 400);
        assertThat(response.firstErrorMessage(), containsString("$page"));
    }

    @Test(description = "Queries nested deeper than 4 levels are rejected by the server's depth limit")
    public void depthLimit() {
        String tooDeep = "{ character(id: 1) { episode { characters { episode { characters { name } } } } } }";
        GraphQLResponse response = graphql.query(tooDeep);
        Assert.assertEquals(response.status(), 400);
        assertThat(response.firstErrorMessage(), containsString("exceeds maximum operation depth of 4"));

        // One level less is allowed
        graphql.query("{ character(id: 1) { episode { characters { episode { id } } } } }").assertOk();
    }
}
