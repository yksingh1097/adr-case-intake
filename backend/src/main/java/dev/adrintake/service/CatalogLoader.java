package dev.adrintake.service;

import dev.adrintake.exception.CatalogException;
import dev.adrintake.model.Catalog;
import dev.adrintake.model.FieldDefinition;
import dev.adrintake.model.FieldType;
import dev.adrintake.model.Option;
import dev.adrintake.model.Section;
import dev.adrintake.model.Validation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.PatternSyntaxException;

public final class CatalogLoader {

    private static final ObjectMapper mapper = new ObjectMapper().enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);

    private static final Set<String> CATALOG_KEYS = Set.of("catalog_version", "description", "statuses", "confidence_levels", "sections", "fields");
    private static final Set<String> SECTION_KEYS = Set.of("id", "label");
    private static final Set<String> FIELD_KEYS = Set.of("id", "label", "section", "type", "options", "validation", "guidance");
    private static final Set<String> OPTION_KEYS = Set.of("code", "label");
    private static final Set<String> VALIDATION_KEYS = Set.of("pattern", "min", "max");

    private CatalogLoader() {
    }

    public static Catalog load(Path file) {
        try {
            return parse(Files.readString(file));
        } catch (IOException e) {
            throw new CatalogException("Cannot read the catalog file " + file + ": " + e.getMessage());
        }
    }

    public static Catalog parse(String json) {
        JsonNode root;
        try {
            root = mapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw new CatalogException("The catalog is not valid JSON:" + e.getOriginalMessage());
        }
        return parse(root);
    }

    public static Catalog parse(JsonNode root) {
        if (root == null || !root.isObject()) {
            throw new CatalogException("The catalog must be a JSON object");
        }
        List<String> problems = new ArrayList<>();
        rejectUnknownKeys(root, CATALOG_KEYS, "catalog", problems);

        String version = requiredText(root, "catalog_version", "catalog", problems);
        Map<String, String> statuses = parseStatuses(root.get("statuses"), problems);
        List<String> confidenceLevels = parseConfidenceLevels(root.get("confidence_levels"), problems);
        List<Section> sections = parseSections(root.get("sections"), problems);
        Set<String> sectionIds = new HashSet<>();
        sections.forEach(s -> sectionIds.add(s.id()));
        List<FieldDefinition> fields = parseFields(root.get("fields"), sectionIds, problems);

        if (!problems.isEmpty()) {
            throw new CatalogException(problems);
        }
        return new Catalog(version, statuses, confidenceLevels, sections, fields, root);
    }

    private static Map<String, String> parseStatuses(JsonNode node, List<String> problems) {
        Map<String, String> statuses = new LinkedHashMap<>();
        if (node == null || !node.isObject() || node.isEmpty()) {
            problems.add("statuses: must be an object with at least one status");
            return statuses;
        }
        node.fields().forEachRemaining(e -> {
            if (!e.getValue().isTextual() || e.getValue().asText().isBlank()) {
                problems.add("statuses." + e.getKey() + ": must be a non-empty text");
            } else {
                statuses.put(e.getKey(), e.getValue().asText());
            }
        });
        return statuses;
    }

    private static List<String> parseConfidenceLevels(JsonNode node, List<String> problems) {
        List<String> levels = new ArrayList<>();
        if (node == null || !node.isArray() || node.isEmpty()) {
            problems.add("confidence_levels: must be a non-empty array of texts");
            return levels;
        }
        for (JsonNode level : node) {
            if (!level.isTextual() || level.asText().isBlank()) {
                problems.add("confidence_levels: every entry must be a non-empty text");
            } else if (levels.contains(level.asText())) {
                problems.add("confidence_levels: duplicate level '" + level.asText() + "'");
            } else {
                levels.add(level.asText());
            }
        }
        return levels;
    }

    private static List<Section> parseSections(JsonNode node, List<String> problems) {
        List<Section> sections = new ArrayList<>();
        if (node == null || !node.isArray() || node.isEmpty()) {
            problems.add("sections: must be a non-empty array");
            return sections;
        }
        Set<String> seen = new HashSet<>();
        int index = 0;
        for (JsonNode s : node) {
            String where = "sections[" + index++ + "]";
            if (!s.isObject()) {
                problems.add(where + ": must be an object");
                continue;
            }
            rejectUnknownKeys(s, SECTION_KEYS, where, problems);
            String id = requiredText(s, "id", where, problems);
            String label = requiredText(s, "label", where, problems);
            if (id != null && !seen.add(id)) {
                problems.add(where + ": duplicate section id '" + id + "'");
            } else if (id != null && label != null) {
                sections.add(new Section(id, label));
            }
        }
        return sections;
    }

    private static List<FieldDefinition> parseFields(JsonNode node, Set<String> sectionIds, List<String> problems) {
        List<FieldDefinition> fields = new ArrayList<>();
        if (node == null || !node.isArray() || node.isEmpty()) {
            problems.add("fields: must be a non-empty array");
            return fields;
        }
        Set<String> seen = new HashSet<>();
        int index = 0;
        for (JsonNode f : node) {
            String where = "fields[" + index++ + "]";
            if (!f.isObject()) {
                problems.add(where + ": must be an object");
                continue;
            }
            String id = requiredText(f, "id", where, problems);
            if (id != null) {
                where = "field '" + id + "'";
                if (!seen.add(id)) {
                    problems.add(where + ": duplicate field id");
                }
            }
            FieldDefinition parsed = parseField(f, id, where, sectionIds, problems);
            if (parsed != null) {
                fields.add(parsed);
            }
        }
        return fields;
    }

    private static FieldDefinition parseField(JsonNode f, String id, String where, Set<String> sectionIds, List<String> problems) {
        int before = problems.size();
        rejectUnknownKeys(f, FIELD_KEYS, where, problems);
        String label = requiredText(f, "label", where, problems);
        String section = requiredText(f, "section", where, problems);
        if (section != null && !sectionIds.contains(section)) {
            problems.add(where + ": section '" + section + "' is not defined in sections");
        }
        FieldType type = parseType(f, where, problems);
        String guidance = optionalText(f, "guidance", where, problems);
        List<Option> options = parseOptions(f.get("options"), type, where, problems);
        Validation validation = parseValidation(f.get("validation"), type, where, problems);

        if (id == null || label == null || section == null || type == null || problems.size() > before) {
            return null;
        }
        return new FieldDefinition(id, label, section, type, options, validation, guidance);
    }

    private static FieldType parseType(JsonNode f, String where, List<String> problems) {
        String name = requiredText(f, "type", where, problems);
        if (name == null) {
            return null;
        }
        return FieldType.fromJsonName(name).orElseGet(() -> {
            problems.add(where + ": unknown type '" + name + "' (known types: " + FieldType.allJsonNames() + ")");
            return null;
        });
    }

    private static List<Option> parseOptions(JsonNode node, FieldType type, String where, List<String> problems) {
        List<Option> options = new ArrayList<>();
        if (type == null) {
            return options;
        }
        if (!type.hasOptions()) {
            if (node != null) {
                problems.add(where + ": options are only allowed on coded and coded_multi fields, not " + type.jsonName());
            }
            return options;
        }
        if (node == null || !node.isArray() || node.isEmpty()) {
            problems.add(where + ": a " + type.jsonName() + " field needs a non-empty options array");
            return options;
        }
        Set<String> codes = new HashSet<>();
        int index = 0;
        for (JsonNode o : node) {
            String optionWhere = where + " options[" + index++ + "]";
            if (!o.isObject()) {
                problems.add(optionWhere + ": must be an object");
                continue;
            }
            rejectUnknownKeys(o, OPTION_KEYS, optionWhere, problems);
            String code = requiredText(o, "code", optionWhere, problems);
            String label = requiredText(o, "label", optionWhere, problems);
            if (code != null && !codes.add(code)) {
                problems.add(optionWhere + ": duplicate option code '" + code + "'");
            } else if (code != null && label != null) {
                options.add(new Option(code, label));
            }
        }
        return options;
    }

    private static Validation parseValidation(JsonNode node, FieldType type, String where, List<String> problems) {
        if (node == null) {
            return Validation.NONE;
        }
        if (!node.isObject()) {
            problems.add(where + ": validation must be an object");
            return Validation.NONE;
        }
        String here = where + " validation";
        rejectUnknownKeys(node, VALIDATION_KEYS, here, problems);

        String pattern = null;
        if (node.has("pattern")) {
            if (!node.get("pattern").isTextual()) {
                problems.add(here + ": pattern must be a text");
            } else if (type != null && type != FieldType.STRING) {
                problems.add(here + ": pattern only applies to string fields, not " + type.jsonName());
            } else {
                pattern = node.get("pattern").asText();
                try {
                    Validation.compile(pattern);
                } catch (PatternSyntaxException e) {
                    problems.add(here + ": pattern is not a valid regular expression: " + e.getDescription());
                    pattern = null;
                }
            }
        }
        Double min = parseBound(node, "min", type, here, problems);
        Double max = parseBound(node, "max", type, here, problems);
        if (min != null && max != null && min > max) {
            problems.add(here + ": min " + min + " is greater than max " + max);
        }
        return new Validation(pattern, min, max);
    }

    private static Double parseBound(JsonNode node, String key, FieldType type, String here, List<String> problems) {
        if (!node.has(key)) {
            return null;
        }
        JsonNode bound = node.get(key);
        if (!bound.isNumber()) {
            problems.add(here + ": " + key + " must be a number");
            return null;
        }
        if (type != null && !type.isNumeric()) {
            problems.add(here + ": " + key + " only applies to integer and number fields, not " + type.jsonName());
            return null;
        }
        return bound.doubleValue();
    }

    private static void rejectUnknownKeys(JsonNode node, Set<String> allowed, String where, List<String> problems) {
        Iterator<String> names = node.fieldNames();
        while (names.hasNext()) {
            String name = names.next();
            if (!allowed.contains(name)) {
                problems.add(where + ": unknown key '" + name + "' (allowed: " + String.join(", ", new TreeSet<>(allowed)) + ")");
            }
        }
    }

    private static String requiredText(JsonNode node, String key, String where, List<String> problems) {
        JsonNode value = node.get(key);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            problems.add(where + ": '" + key + "' is required and must be a non-empty text");
            return null;
        }
        return value.asText();
    }

    private static String optionalText(JsonNode node, String key, String where, List<String> problems) {
        JsonNode value = node.get(key);
        if (value == null) {
            return null;
        }
        if (!value.isTextual() || value.asText().isBlank()) {
            problems.add(where + ": '" + key + "' must be a non-empty text when present");
            return null;
        }
        return value.asText();
    }

}