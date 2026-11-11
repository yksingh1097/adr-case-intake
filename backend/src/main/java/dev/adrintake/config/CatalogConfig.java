package dev.adrintake.config;

import dev.adrintake.exception.CatalogException;
import dev.adrintake.model.Catalog;
import dev.adrintake.service.CatalogLoader;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Configuration
public class CatalogConfig {

    private static final List<String> SEARCH = List.of("catalog/field_catalog.json", "../catalog/field_catalog.json");

    @Bean
    public Catalog catalog(@Value("${app.catalog-path:}") String configured) {
        return CatalogLoader.load(resolve(configured));
    }

    static Path resolve(String configured) {
        if (!configured.isBlank()) {
            return Path.of(configured);
        }
        for (String candidate : SEARCH) {
            Path path = Path.of(candidate);
            if (Files.isRegularFile(path)) {
                return path;
            }
        }
        throw new CatalogException("Catalog file not found. Set app.catalog-path, or start the app from the repo root or backend/. Tried: "
                + String.join(", ", SEARCH));
    }
}