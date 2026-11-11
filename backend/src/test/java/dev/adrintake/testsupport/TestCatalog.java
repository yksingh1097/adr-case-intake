package dev.adrintake.testsupport;

import dev.adrintake.model.Catalog;
import dev.adrintake.service.CatalogLoader;

/** A small catalog with made-up ids, so tests do not depend on the real one. */
public final class TestCatalog {

    private static final String HEAD = """
            {
              "catalog_version": "t1",
              "statuses": {"extracted": "found", "unknown": "not stated", "conflict": "two answers"},
              "confidence_levels": ["high", "medium", "low"],
              "sections": [{"id": "s1", "label": "Person"}, {"id": "s2", "label": "Medicine"}],
              "fields": [
                {"id": "name", "label": "Name", "section": "s1", "type": "string", "validation": {"pattern": "^[A-Z]{1,4}$"}, "guidance": "Capital letters only."},
                {"id": "age", "label": "Age", "section": "s1", "type": "integer", "validation": {"min": 0, "max": 120}},
                {"id": "sex", "label": "Sex", "section": "s1", "type": "coded", "options": [{"code": "m", "label": "Male"}, {"code": "f", "label": "Female"}]},
                {"id": "when", "label": "Start", "section": "s2", "type": "date"},
                {"id": "tags", "label": "Tags", "section": "s2", "type": "string_list"},
                {"id": "kinds", "label": "Kinds", "section": "s2", "type": "coded_multi", "options": [{"code": "a", "label": "A"}, {"code": "b", "label": "B"}, {"code": "none", "label": "None"}]}%s
              ]
            }""";

    private TestCatalog() {
    }

    public static Catalog catalog() {
        return CatalogLoader.parse(HEAD.formatted(""));
    }

    /** The same catalog plus one more field, given as JSON, e.g. {"id": "weight", ...}. */
    public static Catalog catalogWith(String extraFieldJson) {
        return CatalogLoader.parse(HEAD.formatted(",\n" + extraFieldJson));
    }

    public static Catalog catalogFromJson(String json) {
        return CatalogLoader.parse(json);
    }

    public static String json() {
        return HEAD.formatted("");
    }
}
