package com.qa.tests;

import com.qa.graphql.client.GraphQLClient;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Introspection-based contract test: the fields and filter inputs this suite relies on still exist
 * with the expected types. Catches breaking schema changes before the functional tests fail confusingly.
 */
public class SchemaContractTest {

    private final GraphQLClient graphql = new GraphQLClient();

    @Test(groups = "smoke", description = "Character type exposes the fields our operations select")
    public void characterTypeContract() {
        Map<String, String> fields = fieldTypes("Character");
        Assert.assertTrue(fields.keySet().containsAll(Set.of(
                "id", "name", "status", "species", "type", "gender", "image", "created", "origin", "location", "episode")),
                "Character fields: " + fields.keySet());
        Assert.assertEquals(fields.get("id"), "ID");
        Assert.assertEquals(fields.get("name"), "String");
        Assert.assertEquals(fields.get("origin"), "Location");
    }

    @Test(description = "FilterCharacter input accepts the filters our tests use")
    public void filterInputContract() {
        List<Map<String, Object>> inputs = graphql.run("IntrospectType", Map.of("name", "FilterCharacter"))
                .assertOk().path("__type.inputFields");
        Set<String> names = inputs.stream().map(f -> (String) f.get("name")).collect(Collectors.toSet());
        Assert.assertTrue(names.containsAll(Set.of("name", "status", "species", "gender")), "inputs: " + names);
    }

    @Test(description = "Info type supports pagination")
    public void infoTypeContract() {
        Assert.assertEquals(fieldTypes("Info"), Map.of("count", "Int", "pages", "Int", "next", "Int", "prev", "Int"));
    }

    /** field name -> named type, unwrapping NON_NULL / LIST once. */
    @SuppressWarnings("unchecked")
    private Map<String, String> fieldTypes(String typeName) {
        List<Map<String, Object>> fields = graphql.run("IntrospectType", Map.of("name", typeName))
                .assertOk().path("__type.fields");
        return fields.stream().collect(Collectors.toMap(
                f -> (String) f.get("name"),
                f -> {
                    Map<String, Object> type = (Map<String, Object>) f.get("type");
                    Object name = type.get("name");
                    if (name == null) {
                        name = ((Map<String, Object>) type.get("ofType")).get("name");
                    }
                    return String.valueOf(name);
                }));
    }
}
