package dev.adrintake.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class GeminiClientTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String MODEL = "test-model";
    private static final String OK = """
            {"candidates":[{"finishReason":"STOP","content":{"parts":[{"text":"{\\"answer\\":\\"ok\\"}"}]}}],
             "modelVersion":"test-model-001"}""";

    private HttpServer server;
    private final AtomicReference<String> path = new AtomicReference<>();
    private final AtomicReference<String> keyHeader = new AtomicReference<>();
    private final AtomicReference<String> body = new AtomicReference<>();
    private int status = 200;
    private String reply = OK;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            path.set(exchange.getRequestURI().toString());
            keyHeader.set(exchange.getRequestHeaders().getFirst("x-goog-api-key"));
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = reply.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private GeminiClient client() {
        return client(() -> "secret-key");
    }

    private GeminiClient client(java.util.function.Supplier<String> key) {
        return new GeminiClient("http://127.0.0.1:" + server.getAddress().getPort() + "/v1beta", MODEL, key, Duration.ofSeconds(5));
    }

    private static LlmRequest request() throws IOException {
        return new LlmRequest("be strict", "read this", JSON.readTree("{\"type\":\"object\"}"));
    }

    @Test
    void returnsTheAnswerTextAndTheModelTheProviderReports() throws IOException {
        LlmResponse response = client().complete(request());

        assertEquals("{\"answer\":\"ok\"}", response.text());
        assertEquals("test-model-001", response.model());
    }

    @Test
    void callsTheModelEndpointAndKeepsTheKeyOutOfTheUrl() throws IOException {
        client().complete(request());

        assertEquals("/v1beta/models/test-model:generateContent", path.get());
        assertEquals("secret-key", keyHeader.get());
    }

    @Test
    void sendsPromptSystemTextAndSchema() throws IOException {
        client().complete(request());

        JsonNode sent = JSON.readTree(body.get());
        assertEquals("be strict", sent.at("/systemInstruction/parts/0/text").asText());
        assertEquals("read this", sent.at("/contents/0/parts/0/text").asText());
        assertEquals("application/json", sent.at("/generationConfig/responseMimeType").asText());
        assertEquals("object", sent.at("/generationConfig/responseJsonSchema/type").asText());
    }

    @Test
    void ignoresThinkingParts() throws IOException {
        reply = """
                {"candidates":[{"finishReason":"STOP","content":{"parts":[
                  {"thought":true,"text":"let me think"},{"text":"{}"}]}}]}""";

        assertEquals("{}", client().complete(request()).text());
    }

    @Test
    void rateLimitIsRetryableAndKeepsGooglesMessage() throws IOException {
        status = 429;
        reply = "{\"error\":{\"code\":429,\"message\":\"Quota exceeded\"}}";

        LlmException e = assertThrows(LlmException.class, () -> client().complete(request()));

        assertTrue(e.retryable());
        assertEquals(429, e.status());
        assertTrue(e.getMessage().contains("Quota exceeded"));
    }

    @Test
    void badRequestIsNotRetryable() throws IOException {
        status = 400;
        reply = "{\"error\":{\"message\":\"Schema is too deep\"}}";

        LlmException e = assertThrows(LlmException.class, () -> client().complete(request()));

        assertFalse(e.retryable());
        assertTrue(e.getMessage().contains("Schema is too deep"));
    }

    @Test
    void errorPageThatIsNotJsonStillGivesAReadableMessage() throws IOException {
        status = 503;
        reply = "<html>try later</html>";

        LlmException e = assertThrows(LlmException.class, () -> client().complete(request()));

        assertTrue(e.retryable());
        assertTrue(e.getMessage().contains("try later"));
    }

    @Test
    void answerCutOffByTheTokenLimitIsAFailureNotAPartialResult() throws IOException {
        reply = "{\"candidates\":[{\"finishReason\":\"MAX_TOKENS\",\"content\":{\"parts\":[{\"text\":\"{\\\"a\"}]}}]}";

        LlmException e = assertThrows(LlmException.class, () -> client().complete(request()));

        assertTrue(e.getMessage().contains("MAX_TOKENS"));
        assertFalse(e.retryable());
    }

    @Test
    void blockedPromptHasNoCandidates() throws IOException {
        reply = "{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}";

        LlmException e = assertThrows(LlmException.class, () -> client().complete(request()));

        assertTrue(e.getMessage().contains("SAFETY"));
    }

    @Test
    void missingKeyFailsWithoutCallingTheServer() throws IOException {
        GeminiClient noKey = client(() -> {
            throw new LlmException("Environment variable X is not set", 0, false);
        });

        assertThrows(LlmException.class, () -> noKey.complete(request()));
        assertNull(path.get());
    }

    @Test
    void connectionFailureIsRetryable() throws IOException {
        server.stop(0);

        LlmException e = assertThrows(LlmException.class, () -> client().complete(request()));

        assertTrue(e.retryable());
        assertEquals(0, e.status());
    }
}
