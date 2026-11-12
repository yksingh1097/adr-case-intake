package dev.adrintake.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named = "smoke", matches = "true")
@EnabledIfEnvironmentVariable(named = "GEMINI_API_KEY", matches = ".+")
class GeminiSmokeTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void realModelAnswersInTheRequestedShape() throws IOException {
        String model = System.getProperty("model", "gemini-3.5-flash-lite");
//        GeminiClient client = new GeminiClient("https://generativelanguage.googleapis.com/v1beta", model,
//                () -> Objects.requireNonNull(System.getenv("GEMINI_API_KEY"), "GEMINI_API_KEY is not set in this run configuration"), Duration.ofSeconds(60));

        LlmClient client = new RetryingLlmClient(
                new GeminiClient("https://generativelanguage.googleapis.com/v1beta", model,
                        () -> System.getenv("GEMINI_API_KEY"), Duration.ofSeconds(60)),
                4, Duration.ofSeconds(15), Duration.ofSeconds(12));

        JsonNode schema = JSON.readTree("""
                {"type":"object","properties":{"answer":{"type":"string"}},"required":["answer"]}""");

        LlmResponse response = client.complete(new LlmRequest("Answer in JSON only.", "Put the word ok in the answer field.", schema));

        System.out.println("model reported by Gemini: " + response.model());
        System.out.println("raw answer: " + response.text());
        assertEquals("ok", JSON.readTree(response.text()).path("answer").asText().toLowerCase());
    }
}
