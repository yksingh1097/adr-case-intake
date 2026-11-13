package dev.adrintake.config;

import java.nio.file.Files;
import java.nio.file.Path;

/** Finds the repo's data folders (documents, out, fixtures) whether the app starts from the repo root or from backend/. */
public final class RepoDirs {

    private static final String ROOT_MARKER = "catalog";

    private RepoDirs() {
    }

    public static Path resolve(String configured, String folder) {
        if (!configured.isBlank()) {
            return Path.of(configured);
        }
        boolean insideBackend = !Files.isDirectory(Path.of(ROOT_MARKER)) && Files.isDirectory(Path.of("..", ROOT_MARKER));
        return insideBackend ? Path.of("..", folder) : Path.of(folder);
    }
}
