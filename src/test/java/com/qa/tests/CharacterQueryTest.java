package com.qa.tests;

import com.qa.graphql.client.GraphQLClient;
import com.qa.graphql.client.GraphQLResponse;
import com.qa.graphql.models.Character;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.MatcherAssert.assertThat;

public class CharacterQueryTest {

    private final GraphQLClient graphql = new GraphQLClient();

    @Test(groups = "smoke", description = "Character by id, using a variable and a shared fragment")
    public void characterById() {
        GraphQLResponse response = graphql.run("GetCharacter", Map.of("id", 1)).assertOk();

        Character rick = response.as("character", Character.class);
        Assert.assertEquals(rick.id(), "1");
        Assert.assertEquals(rick.name(), "Rick Sanchez");
        Assert.assertEquals(rick.species(), "Human");
        Assert.assertEquals(rick.origin().name(), "Earth (C-137)");
        Assert.assertTrue(rick.episode().size() > 40, "Rick appears in most episodes");
        Assert.assertEquals(rick.episode().get(0).episode(), "S01E01");
    }

    @Test(groups = "smoke", description = "Response matches the JSON schema for the selected fields")
    public void characterMatchesSchema() {
        GraphQLResponse response = graphql.run("GetCharacter", Map.of("id", 2)).assertOk();
        assertThat(response.raw().asString(), matchesJsonSchemaInClasspath("schemas/get-character.json"));
    }

    @DataProvider(name = "knownCharacters")
    public Object[][] knownCharacters() {
        return new Object[][] {
                {1, "Rick Sanchez", "Alive", "Male"},
                {2, "Morty Smith", "Alive", "Male"},
                {3, "Summer Smith", "Alive", "Female"},
                {8, "Adjudicator Rick", "Dead", "Male"},
        };
    }

    @Test(dataProvider = "knownCharacters", description = "Data-driven lookups of well-known characters")
    public void knownCharacter(int id, String name, String status, String gender) {
        Character c = graphql.run("GetCharacter", Map.of("id", id)).assertOk().as("character", Character.class);
        Assert.assertEquals(c.name(), name);
        Assert.assertEquals(c.status(), status);
        Assert.assertEquals(c.gender(), gender);
    }

    @Test(description = "Only the selected fields come back (GraphQL returns exactly what you ask for)")
    public void onlySelectedFieldsReturned() {
        GraphQLResponse response = graphql.query("{ character(id: 1) { name } }").assertOk();
        Map<String, Object> character = response.path("character");
        Assert.assertEquals(character.keySet(), java.util.Set.of("name"));
    }

    @Test(description = "Aliases fetch the same field twice in one request")
    public void aliases() {
        GraphQLResponse response = graphql.run("CompareCharacters", Map.of("first", 1, "second", 2)).assertOk();
        Assert.assertEquals(response.path("rick.name"), "Rick Sanchez");
        Assert.assertEquals(response.path("morty.name"), "Morty Smith");
        Assert.assertEquals(response.<String>path("rick.species"), response.path("morty.species"));
    }

    @Test(description = "@include and @skip directives are driven by variables")
    public void directives() {
        Map<String, Object> withOrigin = graphql.run("CharacterWithOptionalOrigin",
                Map.of("id", 1, "withOrigin", true, "skipImage", true)).assertOk().path("character");
        Assert.assertTrue(withOrigin.containsKey("origin"), "origin included");
        Assert.assertFalse(withOrigin.containsKey("image"), "image skipped");

        Map<String, Object> withoutOrigin = graphql.run("CharacterWithOptionalOrigin",
                Map.of("id", 1, "withOrigin", false, "skipImage", false)).assertOk().path("character");
        Assert.assertFalse(withoutOrigin.containsKey("origin"), "origin excluded");
        Assert.assertTrue(withoutOrigin.containsKey("image"), "image present");
    }

    @Test(description = "charactersByIds returns each requested character once")
    public void charactersByIds() {
        List<Character> found = graphql.run("CharactersByIds", Map.of("ids", List.of(1, 2, 3)))
                .assertOk().asList("charactersByIds", Character.class);
        Assert.assertEquals(found.stream().map(Character::id).sorted().toList(), List.of("1", "2", "3"));
    }
}
