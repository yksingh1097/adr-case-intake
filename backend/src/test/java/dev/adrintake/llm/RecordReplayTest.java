package dev.adrintake.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class RecordReplayTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static LlmRequest request(String id) throws IOException {
        return new LlmRequest("be brief", "say ok", JSON.readTree("{\"type\":\"object\"}"), id);
    }

    @Test
    void recordedAnswerComesBackUnchangedWithoutTheRealClient() throws IOException {
        Path dir = Files.createTempDirectory("fixtures");
        LlmClient real = r -> new LlmResponse("{\"a\":1}", "model-x");

        LlmResponse recorded = new RecordingLlmClient(real, dir).complete(request("D01"));
        LlmResponse replayed = new ReplayLlmClient(dir).complete(request("D01"));

        assertEquals(recorded, replayed);
        assertEquals("model-x", replayed.model());
    }

    @Test
    void fixtureHoldsRequestAndResponse() throws IOException {
        Path dir = Files.createTempDirectory("fixtures");

        new RecordingLlmClient(r -> new LlmResponse("text", "m"), dir).complete(request("D02"));

        var saved = JSON.readTree(Files.readString(dir.resolve("D02.json")));
        assertEquals("be brief", saved.path("request").path("system").asText());
        assertEquals("say ok", saved.path("request").path("prompt").asText());
        assertEquals("object", saved.path("request").path("schema").path("type").asText());
        assertEquals("text", saved.path("response").path("text").asText());
    }

    @Test
    void replayIgnoresDifferencesInTheRequest() throws IOException {
        Path dir = Files.createTempDirectory("fixtures");
        new RecordingLlmClient(r -> new LlmResponse("old answer", "m"), dir).complete(request("D01"));

        LlmRequest changed = new LlmRequest("another prompt", "another text", JSON.readTree("{}"), "D01");

        assertEquals("old answer", new ReplayLlmClient(dir).complete(changed).text());
    }

    @Test
    void missingRecordingSaysHowToMakeOne() throws IOException {
        Path dir = Files.createTempDirectory("fixtures");

        LlmException e = assertThrows(LlmException.class, () -> new ReplayLlmClient(dir).complete(request("D09")));

        assertTrue(e.getMessage().contains("app.llm.mode=record"));
        assertFalse(e.retryable());
    }

    @Test
    void failedRealCallLeavesNoFixture() throws IOException {
        Path dir = Files.createTempDirectory("fixtures");
        LlmClient failing = r -> {
            throw new LlmException("boom", 500, false);
        };

        assertThrows(LlmException.class, () -> new RecordingLlmClient(failing, dir).complete(request("D01")));

        assertFalse(Files.exists(dir.resolve("D01.json")));
    }

    @Test
    void idWithPathCharactersIsRejected() throws IOException {
        Path dir = Files.createTempDirectory("fixtures");

        assertThrows(IllegalArgumentException.class, () -> new ReplayLlmClient(dir).complete(request("../secret")));
        assertThrows(IllegalArgumentException.class, () -> new ReplayLlmClient(dir).complete(request(null)));
    }
}
