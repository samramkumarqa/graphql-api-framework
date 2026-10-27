package com.qa.tests;

import com.qa.graphql.client.OperationLoader;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Offline checks of the .graphql loader: no network needed. */
public class OperationLoaderTest {

    @Test(groups = {"smoke", "offline"}, description = "Imported fragments are appended exactly once and #import lines removed")
    public void fragmentsAreInlinedOnce() {
        String doc = OperationLoader.operation("ListCharacters");

        Assert.assertFalse(doc.contains("#import"), doc);
        Assert.assertTrue(doc.startsWith("query ListCharacters"), doc);
        Assert.assertEquals(count(doc, "fragment CharacterFields on Character"), 1);
        Assert.assertEquals(count(doc, "fragment PageInfo on Info"), 1);
    }

    @Test(groups = "offline", description = "Operations without imports are returned unchanged")
    public void operationWithoutImports() {
        String doc = OperationLoader.operation("CharactersByIds");
        Assert.assertFalse(doc.contains("fragment"), doc);
        Assert.assertTrue(doc.contains("charactersByIds(ids: $ids)"), doc);
    }

    @Test(groups = "offline", description = "A missing operation file fails with a clear message")
    public void missingOperation() {
        IllegalArgumentException e = Assert.expectThrows(IllegalArgumentException.class,
                () -> OperationLoader.operation("NoSuchOperation"));
        Assert.assertTrue(e.getMessage().contains("graphql/queries/NoSuchOperation.graphql"), e.getMessage());
    }

    private static int count(String text, String needle) {
        return text.split(java.util.regex.Pattern.quote(needle), -1).length - 1;
    }
}
