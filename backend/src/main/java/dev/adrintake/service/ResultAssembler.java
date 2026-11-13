package dev.adrintake.service;

import dev.adrintake.exception.ExtractionException;
import dev.adrintake.model.Catalog;
import dev.adrintake.model.CaseResult;
import dev.adrintake.model.ExtractionResult;
import dev.adrintake.model.FieldDefinition;
import dev.adrintake.model.FieldResult;
import dev.adrintake.model.FieldType;
import dev.adrintake.model.Provenance;
import dev.adrintake.model.Status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.LongNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Turns the model's answer into a result. Walks the catalog, not the answer: every catalog field ends up in every
 * case, and anything the model got wrong or that cannot be checked becomes unknown with a note saying why.
 */
public final class ResultAssembler {

    public static final String NOTE_NO_ANSWER = "the model gave no answer for this field";

    private final Catalog catalog;

    public ResultAssembler(Catalog catalog) {
        this.catalog = catalog;
    }

    public ExtractionResult assemble(String documentId, String text, JsonNode answer, String model) {
        JsonNode cases = answer.path("cases");
        if (!cases.isArray()) {
            throw new ExtractionException(documentId + ": the model answer has no 'cases' list");
        }
        QuoteLocator locator = new QuoteLocator(text);
        List<CaseResult> results = new ArrayList<>();
        for (JsonNode modelCase : cases) {
            results.add(assembleCase(results.size(), modelCase, locator));
        }
        return new ExtractionResult(documentId, catalog.version(), model, flagsOf(answer.path("flags")), results);
    }

    private CaseResult assembleCase(int index, JsonNode modelCase, QuoteLocator locator) {
        String label = modelCase.path("label").asText("").strip();
        JsonNode answers = modelCase.path("fields");
        Map<String, FieldResult> fields = new LinkedHashMap<>();
        for (FieldDefinition field : catalog.fields()) {
            fields.put(field.id(), assembleField(field, answers.path(field.id()), locator));
        }
        return new CaseResult(index, label.isEmpty() ? "Case " + (index + 1) : label, fields);
    }

    private FieldResult assembleField(FieldDefinition field, JsonNode answer, QuoteLocator locator) {
        if (!answer.isObject()) {
            return FieldResult.unknown(NOTE_NO_ANSWER);
        }
        String statusText = answer.path("status").asText("");
        Optional<Status> status = Status.fromJsonName(statusText);
        if (status.isEmpty()) {
            return FieldResult.unknown("the model gave an invalid status '" + statusText + "'");
        }
        String note = answer.path("note").asText("").strip();
        String noteOrNull = note.isEmpty() ? null : note;
        return switch (status.get()) {
            case UNKNOWN -> FieldResult.unknown(noteOrNull);
            case CONFLICT -> FieldResult.conflict(noteOrNull);
            case EXTRACTED -> extracted(field, answer, noteOrNull, locator);
        };
    }

    private FieldResult extracted(FieldDefinition field, JsonNode answer, String note, QuoteLocator locator) {
        JsonNode value = answer.get("value");
        Optional<String> problem = ValueValidator.check(field, value);
        if (problem.isPresent()) {
            return FieldResult.unknown("value rejected: " + problem.get() + " (the model gave " + value + ")");
        }
        String confidence = answer.path("confidence").asText("");
        if (!catalog.confidenceLevels().contains(confidence)) {
            return FieldResult.unknown("the model gave no valid confidence for the value " + value);
        }
        List<Provenance> provenance = locateAll(answer.path("quotes"), locator);
        if (provenance.isEmpty()) {
            return FieldResult.unknown("no quote for the value " + value + " was found in the document");
        }
        if (field.type() == FieldType.INTEGER && !value.isIntegralNumber()) {
            value = LongNode.valueOf(value.longValue());
        }
        return FieldResult.extracted(value, confidence, provenance, note);
    }

    private static List<Provenance> locateAll(JsonNode quotes, QuoteLocator locator) {
        Set<Provenance> found = new LinkedHashSet<>();
        for (JsonNode quote : quotes) {
            if (quote.isTextual()) {
                locator.locate(quote.asText()).ifPresent(found::add);
            }
        }
        return List.copyOf(found);
    }

    private static List<String> flagsOf(JsonNode flags) {
        Set<String> result = new LinkedHashSet<>();
        for (JsonNode flag : flags) {
            if (flag.isTextual() && !flag.asText().isBlank()) {
                result.add(flag.asText().strip());
            }
        }
        return List.copyOf(result);
    }
}
