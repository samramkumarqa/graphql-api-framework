package com.qa.tests;

import com.qa.graphql.client.GraphQLClient;
import com.qa.graphql.client.GraphQLResponse;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

/** Nested queries: the graph must be consistent in both directions. */
public class RelationshipTest {

    private final GraphQLClient graphql = new GraphQLClient();

    @Test(groups = "smoke", description = "Every character in an episode's cast lists that episode")
    public void episodeCastAppearsInEpisode() {
        GraphQLResponse response = graphql.run("GetEpisodeWithCast", Map.of("id", 1)).assertOk();
        Assert.assertEquals(response.path("episode.name"), "Pilot");
        Assert.assertEquals(response.path("episode.episode"), "S01E01");

        List<Map<String, Object>> cast = response.path("episode.characters");
        Assert.assertFalse(cast.isEmpty());
        for (Map<String, Object> character : cast) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> episodes = (List<Map<String, Object>>) character.get("episode");
            boolean inPilot = episodes.stream().anyMatch(e -> "1".equals(e.get("id")));
            Assert.assertTrue(inPilot, character.get("name") + " should list episode 1");
        }
    }

    @Test(description = "Every resident of a location has that location as their last known location")
    public void locationResidentsLiveThere() {
        GraphQLResponse response = graphql.run("GetLocationResidents", Map.of("id", 1)).assertOk();
        Assert.assertEquals(response.path("location.name"), "Earth (C-137)");

        List<Map<String, Object>> residents = response.path("location.residents");
        Assert.assertFalse(residents.isEmpty());
        for (Map<String, Object> resident : residents) {
            @SuppressWarnings("unchecked")
            Map<String, Object> location = (Map<String, Object>) resident.get("location");
            Assert.assertEquals(location.get("id"), "1", resident.get("name") + " should be located at 1");
        }
    }
}
