package dev.adrintake.service;

import dev.adrintake.model.Catalog;
import dev.adrintake.model.FieldDefinition;
import dev.adrintake.model.Option;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The JSON Schema the model must answer in, built from the catalog. Kept to plain keywords (type, enum, items,
 * properties, required) that the structured-output feature of the model API accepts.
 */
public final class SchemaBuilder {

    private static final ObjectMapper JSON = new ObjectMapper();

    private SchemaBuilder() {
    }

    public static ObjectNode build(Catalog catalog) {
        ObjectNode fieldProperties = JSON.createObjectNode();
        ArrayNode fieldIds = JSON.createArrayNode();
        for (FieldDefinition field : catalog.fields()) {
            fieldProperties.set(field.id(), fieldSchema(catalog, field));
            fieldIds.add(field.id());
        }

        ObjectNode fields = object(fieldProperties, fieldIds);
        ObjectNode caseProperties = JSON.createObjectNode();
        caseProperties.set("label", type("string"));
        caseProperties.set("fields", fields);

        ObjectNode cases = JSON.createObjectNode();
        cases.put("type", "array");
        cases.set("items", object(caseProperties, names("label", "fields")));

        ObjectNode flags = JSON.createObjectNode();
        flags.put("type", "array");
        flags.set("items", type("string"));

        ObjectNode rootProperties = JSON.createObjectNode();
        rootProperties.set("flags", flags);
        rootProperties.set("cases", cases);
        return object(rootProperties, names("flags", "cases"));
    }

    private static ObjectNode fieldSchema(Catalog catalog, FieldDefinition field) {
        ObjectNode properties = JSON.createObjectNode();
        properties.set("status", enumOf(catalog.statuses().keySet()));
        properties.set("value", valueSchema(field));
        properties.set("confidence", enumOf(catalog.confidenceLevels()));
        ObjectNode quotes = JSON.createObjectNode();
        quotes.put("type", "array");
        quotes.set("items", type("string"));
        properties.set("quotes", quotes);
        properties.set("note", type("string"));
        return object(properties, names("status", "quotes"));
    }

    private static ObjectNode valueSchema(FieldDefinition field) {
        return switch (field.type()) {
            case STRING, DATE -> type("string");
            case INTEGER -> type("integer");
            case NUMBER -> type("number");
            case CODED -> enumOf(codes(field));
            case CODED_MULTI -> arrayOf(enumOf(codes(field)));
            case STRING_LIST -> arrayOf(type("string"));
        };
    }

    private static Iterable<String> codes(FieldDefinition field) {
        return field.options().stream().map(Option::code).toList();
    }

    private static ObjectNode arrayOf(ObjectNode items) {
        ObjectNode array = type("array");
        array.set("items", items);
        return array;
    }

    private static ObjectNode enumOf(Iterable<String> values) {
        ObjectNode node = type("string");
        ArrayNode list = node.putArray("enum");
        values.forEach(list::add);
        return node;
    }

    private static ObjectNode object(ObjectNode properties, ArrayNode required) {
        ObjectNode node = type("object");
        node.set("properties", properties);
        node.set("required", required);
        return node;
    }

    private static ObjectNode type(String name) {
        ObjectNode node = JSON.createObjectNode();
        node.put("type", name);
        return node;
    }

    private static ArrayNode names(String... names) {
        ArrayNode array = JSON.createArrayNode();
        for (String name : names) {
            array.add(name);
        }
        return array;
    }
}
