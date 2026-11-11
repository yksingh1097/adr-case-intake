package dev.adrintake.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class Catalog {

    private final String version;
    private final Map<String, String> statuses;
    private final List<String> confidenceLevels;
    private final List<Section> sections;
    private final List<FieldDefinition> fields;
    private final Map<String, FieldDefinition> fieldsById;
    private final JsonNode raw;

    public Catalog(String version,
                   Map<String, String> statuses,
                   List<String> confidenceLevels,
                   List<Section> sections,
                   List<FieldDefinition> fields,
                   JsonNode raw) {
        this.version = version;
        this.statuses = Collections.unmodifiableMap(new LinkedHashMap<>(statuses));
        this.confidenceLevels = List.copyOf(confidenceLevels);
        this.sections = List.copyOf(sections);
        this.fields = List.copyOf(fields);
        this.raw = raw.deepCopy();
        Map<String, FieldDefinition> index = new LinkedHashMap<>();
        fields.forEach(f -> index.put(f.id(), f));
        this.fieldsById = Collections.unmodifiableMap(index);
    }

    public String version() {
        return version;
    }

    public Map<String, String> statuses() {
        return statuses;
    }

    public List<String> confidenceLevels() {
        return confidenceLevels;
    }

    public List<Section> sections() {
        return sections;
    }


    public List<FieldDefinition> fields() {
        return fields;
    }

    public Optional<FieldDefinition> find(String fieldId) {
        return Optional.ofNullable(fieldsById.get(fieldId));
    }

    public FieldDefinition require(String fieldId) {
        return find(fieldId).orElseThrow(() -> new IllegalArgumentException("Field not in catalog: " + fieldId));
    }

    public List<FieldDefinition> fieldsIn(String sectionId) {
        return fields.stream().filter(f -> f.section().equals(sectionId)).toList();
    }

    public JsonNode rawJson() {
        return raw.deepCopy();
    }
}
