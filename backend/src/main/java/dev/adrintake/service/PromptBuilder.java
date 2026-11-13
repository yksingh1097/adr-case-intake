package dev.adrintake.service;

import dev.adrintake.model.Catalog;
import dev.adrintake.model.FieldDefinition;
import dev.adrintake.model.FieldType;
import dev.adrintake.model.Option;
import dev.adrintake.model.Section;
import dev.adrintake.model.Validation;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builds the model instructions from a text template and the catalog. The template holds the general rules;
 * everything about a field comes from the catalog, so a new catalog field reaches the model without a code change.
 */
public final class PromptBuilder {

    static final String STATUSES = "{{statuses}}";
    static final String CONFIDENCE_LEVELS = "{{confidence_levels}}";
    static final String FIELDS = "{{fields}}";

    private final String system;

    public PromptBuilder(Catalog catalog, String template) {
        for (String token : List.of(STATUSES, CONFIDENCE_LEVELS, FIELDS)) {
            if (!template.contains(token)) {
                throw new IllegalArgumentException("The prompt template has no " + token + " placeholder");
            }
        }
        this.system = template
                .replace(STATUSES, statuses(catalog))
                .replace(CONFIDENCE_LEVELS, String.join(", ", catalog.confidenceLevels()))
                .replace(FIELDS, fields(catalog));
    }

    public String system() {
        return system;
    }

    public String prompt(String documentText) {
        return "Fill in the form for the report between the markers.\n\n<report>\n" + documentText + "\n</report>";
    }

    private static String statuses(Catalog catalog) {
        return catalog.statuses().entrySet().stream()
                .map((Map.Entry<String, String> e) -> "  " + e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining("\n"));
    }

    private static String fields(Catalog catalog) {
        StringBuilder text = new StringBuilder();
        for (Section section : catalog.sections()) {
            text.append("\n## ").append(section.label()).append('\n');
            for (FieldDefinition field : catalog.fieldsIn(section.id())) {
                describe(field, text);
            }
        }
        return text.toString().strip();
    }

    private static void describe(FieldDefinition field, StringBuilder text) {
        text.append("- ").append(field.id()).append(" (").append(field.label()).append("): ").append(valueHint(field.type())).append('\n');
        if (!field.options().isEmpty()) {
            String options = field.options().stream().map((Option o) -> o.code() + " = " + o.label()).collect(Collectors.joining("; "));
            text.append("  Allowed codes: ").append(options).append('\n');
        }
        Validation rules = field.validation();
        if (rules.pattern() != null) {
            text.append("  Must match the pattern ").append(rules.pattern()).append('\n');
        }
        if (rules.min() != null) {
            text.append("  Minimum ").append(plain(rules.min())).append('\n');
        }
        if (rules.max() != null) {
            text.append("  Maximum ").append(plain(rules.max())).append('\n');
        }
        field.guidanceText().ifPresent(g -> text.append("  ").append(g).append('\n'));
    }

    private static String plain(double bound) {
        return bound == Math.rint(bound) ? String.valueOf((long) bound) : String.valueOf(bound);
    }

    private static String valueHint(FieldType type) {
        return switch (type) {
            case STRING -> "text";
            case INTEGER -> "whole number";
            case NUMBER -> "number";
            case DATE -> "date as YYYY-MM-DD";
            case CODED -> "one allowed code";
            case CODED_MULTI -> "list of allowed codes";
            case STRING_LIST -> "list of texts";
        };
    }
}
