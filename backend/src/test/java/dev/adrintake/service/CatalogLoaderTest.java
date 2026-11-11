package dev.adrintake.service;

import dev.adrintake.exception.CatalogException;
import dev.adrintake.model.Catalog;
import dev.adrintake.model.FieldDefinition;
import dev.adrintake.model.FieldType;
import dev.adrintake.model.Section;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class CatalogLoaderTest {

    private static String catalog(String fieldsJson) {
        return """
                {
                  "catalog_version": "9.9",
                  "statuses": {"extracted": "found", "unknown": "not stated"},
                  "confidence_levels": ["high", "low"],
                  "sections": [{"id": "s1", "label": "One"}, {"id": "s2", "label": "Two"}],
                  "fields": [%s]
                }""".formatted(fieldsJson);
    }

    private static List<String> problemsOf(String json) {
        return assertThrows(CatalogException.class, () -> CatalogLoader.parse(json)).problems();
    }

    private static void assertProblem(List<String> problems, String fragment) {
        assertTrue(problems.stream().anyMatch(p -> p.contains(fragment)),
                "expected a problem containing '" + fragment + "' but got " + problems);
    }

    @Test
    void loadsTheCommittedCatalog() {
        Catalog catalog = CatalogLoader.load(RepoPaths.catalog());

        assertEquals("1.0", catalog.version());
        assertFalse(catalog.fields().isEmpty());
        assertEquals(List.of("high", "medium", "low"), catalog.confidenceLevels());
        assertTrue(catalog.statuses().keySet().containsAll(Set.of("extracted", "unknown", "conflict")));
    }

    @Test
    void everyFieldOfTheCommittedCatalogBelongsToADefinedSection() {
        Catalog catalog = CatalogLoader.load(RepoPaths.catalog());
        Set<String> sectionIds = catalog.sections().stream().map(Section::id).collect(Collectors.toSet());

        catalog.fields().forEach(f -> assertTrue(sectionIds.contains(f.section()), f.id()));
    }

    @Test
    void keepsTheFieldOrderOfTheFile() {
        Catalog catalog = CatalogLoader.load(RepoPaths.catalog());
        List<String> ids = catalog.fields().stream().map(FieldDefinition::id).toList();

        assertEquals(ids.stream().distinct().count(), ids.size());
        assertEquals(ids.get(0), catalog.fields().get(0).id());
        assertEquals(catalog.rawJson().get("fields").get(0).get("id").asText(), ids.get(0));
        assertEquals(catalog.rawJson().get("fields").get(ids.size() - 1).get("id").asText(), ids.get(ids.size() - 1));
    }

    @Test
    void rawJsonIsACopy() {
        Catalog catalog = CatalogLoader.load(RepoPaths.catalog());
        ((com.fasterxml.jackson.databind.node.ObjectNode) catalog.rawJson()).put("catalog_version", "tampered");

        assertEquals("1.0", catalog.rawJson().get("catalog_version").asText());
    }

    @Test
    void parsesEveryTypeAndValidationKey() {
        Catalog catalog = CatalogLoader.parse(catalog("""
                {"id":"a","label":"A","section":"s1","type":"string","validation":{"pattern":"^[A-Z]+$"},"guidance":"g"},
                {"id":"b","label":"B","section":"s1","type":"integer","validation":{"min":0,"max":120}},
                {"id":"c","label":"C","section":"s2","type":"number","validation":{"min":0.5}},
                {"id":"d","label":"D","section":"s2","type":"date"},
                {"id":"e","label":"E","section":"s2","type":"coded","options":[{"code":"x","label":"X"},{"code":"y","label":"Y"}]},
                {"id":"f","label":"F","section":"s2","type":"coded_multi","options":[{"code":"x","label":"X"}]},
                {"id":"g","label":"G","section":"s2","type":"string_list"}
                """));

        assertEquals(7, catalog.fields().size());
        assertEquals("^[A-Z]+$", catalog.require("a").validation().pattern());
        assertEquals("g", catalog.require("a").guidanceText().orElseThrow());
        assertEquals(0.0, catalog.require("b").validation().min());
        assertEquals(120.0, catalog.require("b").validation().max());
        assertEquals(0.5, catalog.require("c").validation().min());
        assertNull(catalog.require("c").validation().max());
        assertEquals(Set.of("x", "y"), catalog.require("e").optionCodes());
        assertEquals(FieldType.STRING_LIST, catalog.require("g").type());
        assertEquals(List.of("a", "b"), catalog.fieldsIn("s1").stream().map(FieldDefinition::id).limit(2).toList());
        assertEquals(2, catalog.fieldsIn("s1").size());
        assertTrue(catalog.find("missing").isEmpty());
    }

    @Test
    void aNewFieldNeedsNoCodeChange() {
        String withNewField = catalog("""
                {"id":"a","label":"A","section":"s1","type":"string"},
                {"id":"patient_weight_kg","label":"Weight (kg)","section":"s2","type":"number","validation":{"min":0,"max":400}}
                """);

        FieldDefinition added = CatalogLoader.parse(withNewField).require("patient_weight_kg");

        assertEquals(FieldType.NUMBER, added.type());
        assertEquals(400.0, added.validation().max());
    }

    @Test
    void rejectsAnUnknownType() {
        assertProblem(problemsOf(catalog("""
                {"id":"a","label":"A","section":"s1","type":"boolean"}""")), "unknown type 'boolean'");
    }

    @Test
    void rejectsAnUnknownSection() {
        assertProblem(problemsOf(catalog("""
                {"id":"a","label":"A","section":"nope","type":"string"}""")), "section 'nope' is not defined");
    }

    @Test
    void rejectsDuplicateFieldIds() {
        assertProblem(problemsOf(catalog("""
                {"id":"a","label":"A","section":"s1","type":"string"},
                {"id":"a","label":"A2","section":"s1","type":"string"}""")), "duplicate field id");
    }

    @Test
    void rejectsACodedFieldWithoutOptions() {
        assertProblem(problemsOf(catalog("""
                {"id":"a","label":"A","section":"s1","type":"coded"}""")), "needs a non-empty options array");
    }

    @Test
    void rejectsOptionsOnANonCodedField() {
        assertProblem(problemsOf(catalog("""
                {"id":"a","label":"A","section":"s1","type":"string","options":[{"code":"x","label":"X"}]}""")),
                "options are only allowed on coded");
    }

    @Test
    void rejectsDuplicateOptionCodes() {
        assertProblem(problemsOf(catalog("""
                {"id":"a","label":"A","section":"s1","type":"coded","options":[{"code":"x","label":"X"},{"code":"x","label":"Again"}]}""")),
                "duplicate option code 'x'");
    }

    @Test
    void rejectsAnInvalidRegularExpression() {
        assertProblem(problemsOf(catalog("""
                {"id":"a","label":"A","section":"s1","type":"string","validation":{"pattern":"[unclosed"}}""")),
                "not a valid regular expression");
    }

    @Test
    void rejectsMinGreaterThanMax() {
        assertProblem(problemsOf(catalog("""
                {"id":"a","label":"A","section":"s1","type":"integer","validation":{"min":10,"max":1}}""")),
                "greater than max");
    }

    @Test
    void rejectsAValidationKeyThatDoesNotApplyToTheType() {
        assertProblem(problemsOf(catalog("""
                {"id":"a","label":"A","section":"s1","type":"date","validation":{"min":0}}""")),
                "only applies to integer and number");
        assertProblem(problemsOf(catalog("""
                {"id":"a","label":"A","section":"s1","type":"integer","validation":{"pattern":"^1$"}}""")),
                "only applies to string fields");
    }

    @Test
    void rejectsUnknownKeysAtEveryLevel() {
        assertProblem(problemsOf(catalog("""
                {"id":"a","label":"A","section":"s1","type":"string","validation":{"maxLength":5}}""")),
                "unknown key 'maxLength'");
        assertProblem(problemsOf(catalog("""
                {"id":"a","label":"A","section":"s1","type":"string","hint":"x"}""")), "unknown key 'hint'");
    }

    @Test
    void rejectsDuplicateKeysInTheJsonFile() {
        String json = """
                {"catalog_version":"1","catalog_version":"2","statuses":{"extracted":"x"},"confidence_levels":["high"],
                 "sections":[{"id":"s1","label":"One"}],"fields":[{"id":"a","label":"A","section":"s1","type":"string"}]}""";

        assertProblem(problemsOf(json), "not valid JSON");
    }

    @Test
    void reportsEveryProblemInOnePass() {
        List<String> problems = problemsOf(catalog("""
                {"id":"a","label":"A","section":"nope","type":"boolean"},
                {"id":"b","label":"B","section":"s1","type":"coded"}"""));

        assertTrue(problems.size() >= 3, problems.toString());
    }

    @Test
    void rejectsMalformedJsonAndNonObjects() {
        assertProblem(problemsOf("{not json"), "not valid JSON");
        assertProblem(problemsOf("[]"), "must be a JSON object");
        assertProblem(problemsOf(""), "must be a JSON object");
    }

    @Test
    void reportsAMissingFileClearly() {
        CatalogException e = assertThrows(CatalogException.class,
                () -> CatalogLoader.load(java.nio.file.Path.of("/no/such/catalog.json")));

        assertTrue(e.getMessage().contains("Cannot read the catalog file"));
    }
}
