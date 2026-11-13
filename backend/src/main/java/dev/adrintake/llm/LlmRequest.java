package dev.adrintake.llm;

import com.fasterxml.jackson.databind.JsonNode;

/** {@code exchangeId} names the exchange in fixtures/ (the document id). Live clients ignore it. */
public record LlmRequest(String system, String prompt, JsonNode schema, String exchangeId) {

    public LlmRequest(String system, String prompt, JsonNode schema) {
        this(system, prompt, schema, null);
    }
}
