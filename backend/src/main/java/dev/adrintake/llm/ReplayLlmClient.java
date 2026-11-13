package dev.adrintake.llm;

import dev.adrintake.util.JsonFiles;

import com.fasterxml.jackson.databind.JsonNode;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Answers from the recorded response only. The request is not compared with the recorded one on purpose: after a
 * catalog change the prompt differs, and the recorded answer is still checked against the new catalog downstream.
 */
public final class ReplayLlmClient implements LlmClient {

    private final Path dir;

    public ReplayLlmClient(Path dir) {
        this.dir = dir;
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        Path file = Fixtures.fileFor(dir, request.exchangeId());
        if (!Files.isRegularFile(file)) {
            throw new LlmException("No recording " + file + ". Record it with --app.llm.mode=record (needs the API key)", 0, false);
        }
        JsonNode response = JsonFiles.read(file).path("response");
        if (!response.path("text").isTextual()) {
            throw new LlmException("Recording " + file + " has no response text", 0, false);
        }
        return new LlmResponse(response.get("text").asText(), response.path("model").asText(""));
    }
}
