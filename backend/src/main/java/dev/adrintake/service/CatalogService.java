package dev.adrintake.service;

import dev.adrintake.model.Catalog;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

@Service
public class CatalogService {

    private final Catalog catalog;

    public CatalogService(Catalog catalog) {
        this.catalog = catalog;
    }

    public JsonNode rawCatalog() {
        return catalog.rawJson();
    }
}