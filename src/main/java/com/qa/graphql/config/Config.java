package com.qa.graphql.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

/**
 * Reads settings in priority order:
 * 1. JVM system property   (mvn test -Dbrowser=firefox)
 * 2. Environment variable  (BROWSER=firefox; dots become underscores, upper case)
 * 3. config.properties on the test classpath
 *
 * Secrets are never stored in config.properties; they come from environment variables only.
 */
public final class Config {

    private static final Properties FILE = load();

    private Config() {
    }

    private static Properties load() {
        Properties props = new Properties();
        try (InputStream in = Config.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read config.properties", e);
        }
        return props;
    }

    /** Returns the value for a key, or null when it is not set anywhere. */
    public static String getOrNull(String key) {
        String value = System.getProperty(key);
        if (isBlank(value)) {
            value = System.getenv(key.toUpperCase().replace('.', '_'));
        }
        if (isBlank(value)) {
            value = FILE.getProperty(key);
        }
        return isBlank(value) ? null : value.trim();
    }

    public static String get(String key) {
        String value = getOrNull(key);
        if (value == null) {
            throw new IllegalStateException("Missing required setting: " + key);
        }
        return value;
    }

    public static int getInt(String key) {
        return Integer.parseInt(get(key));
    }

    public static boolean getBool(String key) {
        return Boolean.parseBoolean(get(key));
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
