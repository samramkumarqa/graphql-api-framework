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
 * GraphQL errors. The public endpoint is a gateway (Stellate) in front of an Apollo Server:
 *  - the gateway rejects documents it can't parse (400) and queries over its depth/complexity limit (413);
 *  - the origin rejects invalid fields and variables with 400 and a validation error;
 *  - a missing record is not an error at all, just null.
 * In every rejected case there is an "errors" array and no data.
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

    @Test(description = "Unparseable document: rejected by the gateway with 400")
    public void syntaxError() {
        GraphQLResponse response = graphql.query("{ character(id: 1) { name ");
        Assert.assertEquals(response.status(), 400);
        assertThat(response.firstErrorMessage(), containsString("did not contain a valid GraphQL request"));
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

    @Test(description = "Over-deep queries are rejected (the gateway's limit answers 413 before the origin's)")
    public void depthLimit() {
        String tooDeep = "{ character(id: 1) { episode { characters { episode { characters { name } } } } } }";
        GraphQLResponse response = graphql.query(tooDeep);
        Assert.assertEquals(response.status(), 413);
        Assert.assertTrue(response.hasErrors(), "expected an errors array: " + response.raw().asString());
        Assert.assertNull(response.raw().jsonPath().get("data"));

        // One level less is allowed
        graphql.query("{ character(id: 1) { episode { characters { episode { id } } } } }").assertOk();
    }
}
