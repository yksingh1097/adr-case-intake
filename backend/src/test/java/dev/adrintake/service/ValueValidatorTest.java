package dev.adrintake.service;

import dev.adrintake.model.Catalog;
import dev.adrintake.model.FieldDefinition;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ValueValidatorTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final Catalog CATALOG = CatalogLoader.parse("""
            {
              "catalog_version": "t",
              "statuses": {"extracted": "found"},
              "confidence_levels": ["high"],
              "sections": [{"id": "s", "label": "S"}],
              "fields": [
                {"id":"text","label":"T","section":"s","type":"string","validation":{"pattern":"^[A-Z]{1,4}$"}},
                {"id":"free","label":"F","section":"s","type":"string"},
                {"id":"int","label":"I","section":"s","type":"integer","validation":{"min":0,"max":120}},
                {"id":"num","label":"N","section":"s","type":"number","validation":{"min":0}},
                {"id":"day","label":"D","section":"s","type":"date"},
                {"id":"one","label":"O","section":"s","type":"coded","options":[{"code":"a","label":"A"},{"code":"b","label":"B"}]},
                {"id":"many","label":"M","section":"s","type":"coded_multi","options":[{"code":"a","label":"A"},{"code":"b","label":"B"},{"code":"none","label":"None"}]},
                {"id":"list","label":"L","section":"s","type":"string_list"}
              ]
            }""");

    private static boolean ok(String fieldId, String valueJson) {
        try {
            return ValueValidator.check(CATALOG.require(fieldId), JSON.readTree(valueJson)).isEmpty();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static String why(String fieldId, String valueJson) {
        try {
            return ValueValidator.check(CATALOG.require(fieldId), JSON.readTree(valueJson)).orElse("valid");
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Test
    void missingOrNullValueIsInvalid() {
        assertEquals("value is missing", ValueValidator.check(CATALOG.require("free"), null).orElseThrow());
        assertFalse(ok("free", "null"));
    }

    @Test
    void stringFollowsItsPattern() {
        assertTrue(ok("text", "\"MK\""));
        assertTrue(ok("text", "\"ABCD\""));
        assertFalse(ok("text", "\"M.K.\""));
        assertFalse(ok("text", "\"mk\""));
        assertFalse(ok("text", "\"ABCDE\""));
        assertFalse(ok("text", "\"\""));
        assertFalse(ok("text", "\"M K\""));
        assertFalse(ok("text", "42"));
        assertTrue(ok("free", "\"anything\""));
        assertFalse(ok("free", "true"));
    }

    @Test
    void integerRejectsFractionsStringsAndBooleans() {
        assertTrue(ok("int", "54"));
        assertTrue(ok("int", "54.0"));
        assertTrue(ok("int", "0"));
        assertTrue(ok("int", "120"));
        assertFalse(ok("int", "54.5"));
        assertFalse(ok("int", "\"54\""));
        assertFalse(ok("int", "true"));
        assertFalse(ok("int", "-1"));
        assertFalse(ok("int", "121"));
        assertFalse(ok("int", "1e400"));
    }

    @Test
    void numberHonoursMinAndAllowsFractions() {
        assertTrue(ok("num", "0.25"));
        assertTrue(ok("num", "0"));
        assertFalse(ok("num", "-0.1"));
        assertFalse(ok("num", "\"0.25\""));
    }

    @Test
    void dateMustBeIsoAndReal() {
        assertTrue(ok("day", "\"2026-03-04\""));
        assertTrue(ok("day", "\"2024-02-29\""));
        assertFalse(ok("day", "\"2026-02-29\""));
        assertFalse(ok("day", "\"2026-02-30\""));
        assertFalse(ok("day", "\"2026-13-01\""));
        assertFalse(ok("day", "\"2026-3-4\""));
        assertFalse(ok("day", "\"04/03/2026\""));
        assertFalse(ok("day", "\"4 March 2026\""));
        assertFalse(ok("day", "\"2026-03-04\\n\""));
        assertFalse(ok("day", "20260304"));
    }

    @Test
    void codedMustBeAnOptionCode() {
        assertTrue(ok("one", "\"a\""));
        assertFalse(ok("one", "\"A\""));
        assertFalse(ok("one", "\"c\""));
        assertFalse(ok("one", "[\"a\"]"));
    }

    @Test
    void codedMultiRules() {
        assertTrue(ok("many", "[\"a\"]"));
        assertTrue(ok("many", "[\"a\",\"b\"]"));
        assertTrue(ok("many", "[\"none\"]"));
        assertFalse(ok("many", "[]"));
        assertFalse(ok("many", "[\"c\"]"));
        assertFalse(ok("many", "[\"a\",\"c\"]"));
        assertFalse(ok("many", "\"a\""));
        assertEquals("'none' must stand alone", why("many", "[\"none\",\"a\"]"));
    }

    @Test
    void stringListMayBeEmptyButMustHoldOnlyStrings() {
        assertTrue(ok("list", "[]"));
        assertTrue(ok("list", "[\"rash\",\"angioedema\"]"));
        assertFalse(ok("list", "[1]"));
        assertFalse(ok("list", "[\"a\",null]"));
        assertFalse(ok("list", "\"rash\""));
    }

    @Test
    void theCommittedExampleOutputPassesTheCommittedCatalog() throws IOException {
        Catalog catalog = CatalogLoader.load(RepoPaths.catalog());
        JsonNode example = JSON.readTree(Files.readString(RepoPaths.root().resolve("schema/EXAMPLE-output-D01.json")));
        JsonNode fields = example.get("cases").get(0).get("fields");

        Set<String> seen = new HashSet<>();
        fields.fields().forEachRemaining(entry -> {
            seen.add(entry.getKey());
            FieldDefinition definition = catalog.find(entry.getKey())
                    .orElseThrow(() -> new AssertionError("example field not in catalog: " + entry.getKey()));
            if ("extracted".equals(entry.getValue().get("status").asText())) {
                assertEquals(java.util.Optional.empty(),
                        ValueValidator.check(definition, entry.getValue().get("value")), entry.getKey());
            }
        });
        Set<String> expected = new HashSet<>();
        catalog.fields().forEach(f -> expected.add(f.id()));
        assertEquals(expected, seen, "the example should cover every catalog field");
    }
}
