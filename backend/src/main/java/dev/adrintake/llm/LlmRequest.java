package dev.adrintake.llm;

import com.fasterxml.jackson.databind.JsonNode;

public record LlmRequest(String system, String prompt, JsonNode schema) {
}
