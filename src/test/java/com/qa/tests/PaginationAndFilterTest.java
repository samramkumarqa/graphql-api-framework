package com.qa.tests;

import com.qa.graphql.client.GraphQLClient;
import com.qa.graphql.client.GraphQLResponse;
import com.qa.graphql.models.Character;
import com.qa.graphql.models.Info;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PaginationAndFilterTest {

    private static final int PAGE_SIZE = 20;
    private final GraphQLClient graphql = new GraphQLClient();

    @Test(groups = "smoke", description = "Page info is consistent: pages = ceil(count / 20), next/prev links")
    public void pageInfoIsConsistent() {
        GraphQLResponse first = graphql.run("ListCharacters", Map.of("page", 1)).assertOk();
        Info info = first.as("characters.info", Info.class);

        Assert.assertTrue(info.count() > PAGE_SIZE, "more than one page of characters");
        Assert.assertEquals(info.pages().intValue(), (info.count() + PAGE_SIZE - 1) / PAGE_SIZE);
        Assert.assertNull(info.prev(), "first page has no previous page");
        Assert.assertEquals(info.next(), Integer.valueOf(2));
        Assert.assertEquals(first.asList("characters.results", Character.class).size(), PAGE_SIZE);

        GraphQLResponse last = graphql.run("ListCharacters", Map.of("page", info.pages())).assertOk();
        Info lastInfo = last.as("characters.info", Info.class);
        Assert.assertNull(lastInfo.next(), "last page has no next page");
        Assert.assertEquals(lastInfo.prev(), Integer.valueOf(info.pages() - 1));
        int expectedOnLast = info.count() - (info.pages() - 1) * PAGE_SIZE;
        Assert.assertEquals(last.asList("characters.results", Character.class).size(), expectedOnLast);
    }

    @Test(description = "Consecutive pages don't overlap")
    public void pagesDoNotOverlap() {
        Set<String> ids = new HashSet<>();
        for (int page = 1; page <= 3; page++) {
            graphql.run("ListCharacters", Map.of("page", page)).assertOk()
                    .asList("characters.results", Character.class)
                    .forEach(c -> Assert.assertTrue(ids.add(c.id()), "duplicate id " + c.id()));
        }
        Assert.assertEquals(ids.size(), 3 * PAGE_SIZE);
    }

    @DataProvider(name = "filters")
    public Object[][] filters() {
        return new Object[][] {
                {Map.of("status", "Dead")},
                {Map.of("gender", "Female", "species", "Human")},
                {Map.of("name", "rick", "status", "Alive")},
        };
    }

    /**
     * Every result on the page satisfies every filter. This API matches filters as case-insensitive
     * "contains" (a regex), not equality: species "Human" also returns "Humanoid". The check mirrors that.
     */
    @Test(dataProvider = "filters", description = "Filtered results all match the filter")
    public void filterResultsMatch(Map<String, String> filter) {
        List<Character> results = graphql.run("ListCharacters", Map.of("filter", filter)).assertOk()
                .asList("characters.results", Character.class);

        Assert.assertFalse(results.isEmpty(), "expected matches for " + filter);
        for (Character c : results) {
            Map<String, String> actual = Map.of(
                    "name", c.name(), "status", c.status(), "gender", c.gender(), "species", c.species());
            filter.forEach((field, expected) -> Assert.assertTrue(
                    actual.get(field).toLowerCase().contains(expected.toLowerCase()),
                    c.name() + ": " + field + "=" + actual.get(field) + " should contain " + expected));
        }
    }

    @Test(description = "A filter with no matches returns an empty list, not an error")
    public void filterWithNoMatches() {
        GraphQLResponse response = graphql.run("ListCharacters",
                Map.of("filter", Map.of("name", "zzqx-no-such-character"))).assertOk();
        Assert.assertEquals(response.asList("characters.results", Character.class), List.of());
    }

    @Test(description = "A page beyond the last one returns an empty list")
    public void pageOutOfRange() {
        GraphQLResponse response = graphql.run("ListCharacters", Map.of("page", 10_000)).assertOk();
        Assert.assertEquals(response.asList("characters.results", Character.class), List.of());
    }

    @Test(description = "Episodes can be filtered by season code")
    public void episodesBySeason() {
        List<Map<String, Object>> season1 = graphql.run("ListEpisodes", Map.of("filter", Map.of("episode", "S01")))
                .assertOk().path("episodes.results");
        Assert.assertEquals(season1.size(), 11, "season 1 has 11 episodes");
        Assert.assertEquals(season1.get(0).get("name"), "Pilot");
        season1.forEach(e -> Assert.assertTrue(e.get("episode").toString().startsWith("S01"), e.toString()));
    }
}
