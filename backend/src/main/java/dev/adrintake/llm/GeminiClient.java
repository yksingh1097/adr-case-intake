package dev.adrintake.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;
import java.util.function.Supplier;

public final class GeminiClient implements LlmClient {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String KEY_HEADER = "x-goog-api-key";
    private static final String FINISHED = "STOP";
    private static final Set<Integer> RETRYABLE_STATUS = Set.of(429, 500, 502, 503, 504);
    private static final int ERROR_SNIPPET = 200;

    private final HttpClient http = HttpClient.newHttpClient();
    private final String baseUrl;
    private final String model;
    private final Supplier<String> apiKey;
    private final Duration timeout;

    public GeminiClient(String baseUrl, String model, Supplier<String> apiKey, Duration timeout) {
        this.baseUrl = baseUrl;
        this.model = model;
        this.apiKey = apiKey;
        this.timeout = timeout;
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        HttpRequest call = HttpRequest.newBuilder(URI.create(baseUrl + "/models/" + model + ":generateContent"))
                .timeout(timeout)
                .header(KEY_HEADER, apiKey.get())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body(request)))
                .build();
        HttpResponse<String> response;
        try {
            response = http.send(call, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new LlmException("Gemini request failed: " + e.getMessage(), 0, true);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmException("Gemini request was interrupted", 0, false);
        }
        return read(response);
    }

    private static String body(LlmRequest request) {
        ObjectNode root = JSON.createObjectNode();
        root.putObject("systemInstruction").putArray("parts").addObject().put("text", request.system());
        root.putArray("contents").addObject().put("role", "user").putArray("parts").addObject().put("text", request.prompt());
        ObjectNode config = root.putObject("generationConfig");
        config.put("temperature", 0);
        config.put("responseMimeType", "application/json");
        config.set("responseJsonSchema", request.schema());
        return root.toString();
    }

    private LlmResponse read(HttpResponse<String> response) {
        int status = response.statusCode();
        JsonNode json = parse(response.body());
        if (status != 200) {
            String message = json != null ? json.path("error").path("message").asText("") : "";
            if (message.isEmpty()) {
                String raw = response.body();
                message = raw.length() > ERROR_SNIPPET ? raw.substring(0, ERROR_SNIPPET) : raw;
            }
            throw new LlmException("Gemini answered " + status + ": " + message, status, RETRYABLE_STATUS.contains(status));
        }
        if (json == null) {
            throw new LlmException("Gemini answered 200 with a body that is not JSON", status, false);
        }
        JsonNode candidate = json.path("candidates").path(0);
        if (candidate.isMissingNode()) {
            String blocked = json.path("promptFeedback").path("blockReason").asText("unknown reason");
            throw new LlmException("Gemini gave no answer (" + blocked + ")", status, false);
        }
        String finish = candidate.path("finishReason").asText("");
        if (!FINISHED.equals(finish)) {
            throw new LlmException("Gemini stopped before finishing (finishReason " + finish + ")", status, false);
        }
        StringBuilder text = new StringBuilder();
        for (JsonNode part : candidate.path("content").path("parts")) {
            if (!part.path("thought").asBoolean(false)) {
                text.append(part.path("text").asText(""));
            }
        }
        if (text.isEmpty()) {
            throw new LlmException("Gemini finished but the answer has no text", status, false);
        }
        return new LlmResponse(text.toString(), json.path("modelVersion").asText(model));
    }

    private static JsonNode parse(String body) {
        try {
            return JSON.readTree(body);
        } catch (IOException e) {
            return null;
        }
    }
}
