package dev.adrintake.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/** What the output file says about one catalog field of one case. */
public record FieldResult(Status status, JsonNode value, String confidence, List<Provenance> provenance, String note) {

    public FieldResult {
        provenance = List.copyOf(provenance);
    }

    public static FieldResult extracted(JsonNode value, String confidence, List<Provenance> provenance, String note) {
        return new FieldResult(Status.EXTRACTED, value, confidence, provenance, note);
    }

    public static FieldResult unknown(String note) {
        return new FieldResult(Status.UNKNOWN, null, null, List.of(), note);
    }

    public static FieldResult conflict(String note) {
        return new FieldResult(Status.CONFLICT, null, null, List.of(), note);
    }
}
