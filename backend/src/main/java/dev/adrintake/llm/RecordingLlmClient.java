package dev.adrintake.llm;

import dev.adrintake.util.JsonFiles;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.file.Path;

/** Passes every call to the real client and saves request and response to fixtures/&lt;id&gt;.json. The key never reaches this class. */
public final class RecordingLlmClient implements LlmClient {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final LlmClient delegate;
    private final Path dir;

    public RecordingLlmClient(LlmClient delegate, Path dir) {
        this.delegate = delegate;
        this.dir = dir;
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        Path file = Fixtures.fileFor(dir, request.exchangeId());
        LlmResponse response = delegate.complete(request);

        ObjectNode root = JSON.createObjectNode();
        root.put("exchange_id", request.exchangeId());
        ObjectNode sent = root.putObject("request");
        sent.put("system", request.system());
        sent.put("prompt", request.prompt());
        sent.set("schema", request.schema());
        ObjectNode received = root.putObject("response");
        received.put("text", response.text());
        received.put("model", response.model());
        JsonFiles.write(file, root);
        return response;
    }
}
