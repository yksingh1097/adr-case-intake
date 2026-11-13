package dev.adrintake.service;

import dev.adrintake.model.Catalog;
import dev.adrintake.testsupport.TestCatalog;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SchemaAndPromptTest {

    private static final String TEMPLATE = "Statuses:\n{{statuses}}\nLevels: {{confidence_levels}}\nFields:\n{{fields}}";
    private static final String EXTRA_FIELD = """
            {"id": "weight", "label": "Weight", "section": "s1", "type": "number", "guidance": "Body weight in kilograms."}""";

    private static JsonNode fieldsSchema(Catalog catalog) {
        return SchemaBuilder.build(catalog).path("properties").path("cases").path("items").path("properties").path("fields");
    }

    @Test
    void schemaHasEveryCatalogFieldAsRequired() {
        JsonNode fields = fieldsSchema(TestCatalog.catalog());

        assertEquals(6, fields.path("required").size());
        assertTrue(fields.path("properties").has("age"));
        assertTrue(fields.path("properties").has("kinds"));
    }

    @Test
    void schemaTypesFollowTheCatalogTypes() {
        JsonNode props = fieldsSchema(TestCatalog.catalog()).path("properties");

        assertEquals("integer", props.path("age").path("properties").path("value").path("type").asText());
        assertEquals("m", props.path("sex").path("properties").path("value").path("enum").get(0).asText());
        assertEquals("array", props.path("kinds").path("properties").path("value").path("type").asText());
        assertEquals("none", props.path("kinds").path("properties").path("value").path("items").path("enum").get(2).asText());
        assertEquals("conflict", props.path("age").path("properties").path("status").path("enum").get(2).asText());
    }

    @Test
    void newCatalogFieldAppearsInSchemaAndPromptWithoutCodeChange() {
        Catalog wider = TestCatalog.catalogWith(EXTRA_FIELD);

        assertTrue(fieldsSchema(wider).path("properties").has("weight"));
        String system = new PromptBuilder(wider, TEMPLATE).system();
        assertTrue(system.contains("weight (Weight)"));
        assertTrue(system.contains("Body weight in kilograms."));
    }

    @Test
    void promptListsOptionsRulesAndSections() {
        String system = new PromptBuilder(TestCatalog.catalog(), TEMPLATE).system();

        assertTrue(system.contains("## Person"));
        assertTrue(system.contains("m = Male; f = Female"));
        assertTrue(system.contains("Must match the pattern ^[A-Z]{1,4}$"));
        assertTrue(system.contains("Minimum 0"));
        assertTrue(system.contains("Maximum 120"));
        assertTrue(system.contains("high, medium, low"));
        assertTrue(system.contains("conflict: two answers"));
    }

    @Test
    void templateWithoutAPlaceholderIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new PromptBuilder(TestCatalog.catalog(), "Fields: {{fields}}"));
    }

    @Test
    void realTemplateHasAllPlaceholders() throws Exception {
        String template = java.nio.file.Files.readString(RepoPaths.root().resolve("backend/src/main/resources/prompts/system.txt"));

        String system = new PromptBuilder(TestCatalog.catalog(), template).system();

        assertFalse(system.contains("{{"));
    }

    @Test
    void reportIsWrappedInMarkers() {
        String prompt = new PromptBuilder(TestCatalog.catalog(), TEMPLATE).prompt("Some text");

        assertTrue(prompt.contains("<report>\nSome text\n</report>"));
    }
}
