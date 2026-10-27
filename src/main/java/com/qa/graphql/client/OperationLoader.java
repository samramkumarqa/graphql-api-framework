package com.qa.graphql.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads GraphQL operations from src/main/resources/graphql/queries/*.graphql.
 *
 * Supports the common `#import "fragments/Name.graphql"` convention: imported fragment files are appended
 * once each (recursively), so operations stay small and fragments are shared, as in real client code.
 */
public final class OperationLoader {

    private static final String ROOT = "graphql/";
    private static final Pattern IMPORT = Pattern.compile("^#import\\s+\"([^\"]+)\"\\s*$", Pattern.MULTILINE);
    private static final Map<String, String> CACHE = new ConcurrentHashMap<>();

    private OperationLoader() {
    }

    /** @param name file name under graphql/queries without extension, e.g. "GetCharacter" */
    public static String operation(String name) {
        return CACHE.computeIfAbsent(name, n -> {
            Set<String> imported = new LinkedHashSet<>();
            String body = resolve("queries/" + n + ".graphql", imported);
            StringBuilder document = new StringBuilder(body.trim());
            for (String fragment : imported) {
                document.append("\n\n").append(stripImports(read(fragment)).trim());
            }
            return document.toString();
        });
    }

    private static String resolve(String path, Set<String> imported) {
        String text = read(path);
        Matcher m = IMPORT.matcher(text);
        while (m.find()) {
            String dependency = m.group(1);
            if (imported.add(dependency)) {
                resolve(dependency, imported); // fragments may import other fragments
            }
        }
        return stripImports(text);
    }

    private static String stripImports(String text) {
        return IMPORT.matcher(text).replaceAll("");
    }

    private static String read(String path) {
        try (InputStream in = OperationLoader.class.getClassLoader().getResourceAsStream(ROOT + path)) {
            if (in == null) {
                throw new IllegalArgumentException("GraphQL file not found on classpath: " + ROOT + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
