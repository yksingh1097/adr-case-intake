package dev.adrintake.service;

import java.nio.file.Files;
import java.nio.file.Path;

final class RepoPaths {

    private static final String CATALOG = "catalog/field_catalog.json";

    private RepoPaths() {
    }

    static Path root() {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null) {
            if (Files.exists(dir.resolve(CATALOG))) {
                return dir;
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException("Cannot find " + CATALOG + " in the working directory or any parent");
    }

    static Path catalog() {
        return root().resolve(CATALOG);
    }
}
