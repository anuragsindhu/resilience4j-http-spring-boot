package com.example.http.client.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public final class ResourceUtils {

    private ResourceUtils() {
        // Utility class
    }

    public static InputStream resolveStream(String location) {
        if (location == null || location.isBlank()) {
            throw new IllegalArgumentException("Resource location must not be blank");
        }

        if (location.startsWith("classpath:")) {
            String path = location.substring("classpath:".length());
            InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);

            if (stream == null) {
                throw new IllegalArgumentException("Classpath resource not found: " + location);
            }

            return stream;

        } else if (location.startsWith("file:")) {
            String path = location.substring("file:".length());
            return loadFileStream(path, location);

        } else if (Files.exists(Paths.get(location))) {
            // Plain file path fallback
            return loadFileStream(location, location);

        } else {
            throw new IllegalArgumentException("Unsupported resource location: " + location);
        }
    }

    private static InputStream loadFileStream(String path, String originalLocation) {
        try {
            return new FileInputStream(new File(path));
        } catch (Exception e) {
            throw new IllegalArgumentException("File resource not found: " + originalLocation, e);
        }
    }
}
