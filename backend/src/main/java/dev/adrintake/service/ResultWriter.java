package dev.adrintake.service;

import dev.adrintake.model.CaseResult;
import dev.adrintake.model.ExtractionResult;
import dev.adrintake.model.FieldResult;
import dev.adrintake.model.Provenance;
import dev.adrintake.util.JsonFiles;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.file.Path;
import java.util.Map;

/** Turns a result into the JSON of schema/extraction.schema.json and saves it as out/&lt;documentId&gt;.json. */
public final class ResultWriter {

    private static final ObjectMapper JSON = new ObjectMapper();

    private ResultWriter() {
    }

    public static Path write(Path outDir, ExtractionResult result) {
        Path file = outDir.resolve(result.documentId() + ".json");
        JsonFiles.write(file, toJson(result));
        return file;
    }

    public static ObjectNode toJson(ExtractionResult result) {
        ObjectNode root = JSON.createObjectNode();
        root.put("document_id", result.documentId());
        root.put("catalog_version", result.catalogVersion());
        if (result.model() != null) {
            root.put("model", result.model());
        }
        ArrayNode flags = root.putArray("flags");
        result.flags().forEach(flags::add);
        ArrayNode cases = root.putArray("cases");
        for (CaseResult c : result.cases()) {
            ObjectNode node = cases.addObject();
            node.put("case_index", c.caseIndex());
            node.put("label", c.label());
            ObjectNode fields = node.putObject("fields");
            for (Map.Entry<String, FieldResult> e : c.fields().entrySet()) {
                fields.set(e.getKey(), fieldJson(e.getValue()));
            }
        }
        return root;
    }

    private static ObjectNode fieldJson(FieldResult field) {
        ObjectNode node = JSON.createObjectNode();
        node.put("status", field.status().jsonName());
        if (field.value() != null) {
            node.set("value", field.value());
        }
        if (field.confidence() != null) {
            node.put("confidence", field.confidence());
        }
        if (!field.provenance().isEmpty()) {
            ArrayNode spans = node.putArray("provenance");
            for (Provenance p : field.provenance()) {
                ObjectNode span = spans.addObject();
                span.put("quote", p.quote());
                span.put("start", p.start());
                span.put("end", p.end());
            }
        }
        if (field.note() != null) {
            node.put("note", field.note());
        }
        return node;
    }
}
