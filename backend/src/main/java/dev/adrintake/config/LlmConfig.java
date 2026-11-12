package dev.adrintake.config;

import dev.adrintake.llm.GeminiClient;
import dev.adrintake.llm.LlmClient;
import dev.adrintake.llm.LlmException;
import dev.adrintake.llm.RetryingLlmClient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.function.Supplier;

@Configuration
public class LlmConfig {

    private static final long MILLIS_PER_MINUTE = 60_000;

    @Bean
    public LlmClient llmClient(
            @Value("${app.llm.provider}") String provider,
            @Value("${app.llm.base-url}") String baseUrl,
            @Value("${app.llm.model}") String model,
            @Value("${app.llm.api-key-env}") String apiKeyEnv,
            @Value("${app.llm.requests-per-minute}") int requestsPerMinute,
            @Value("${app.llm.max-attempts}") int maxAttempts,
            @Value("${app.llm.first-backoff-seconds}") int firstBackoffSeconds,
            @Value("${app.llm.timeout-seconds}") int timeoutSeconds) {
        LlmClient client = switch (provider) {
            case "gemini" -> new GeminiClient(baseUrl, model, keyFrom(apiKeyEnv), Duration.ofSeconds(timeoutSeconds));
            default -> throw new IllegalArgumentException("Unknown app.llm.provider '" + provider + "'. Supported: gemini");
        };
        return new RetryingLlmClient(client, maxAttempts, Duration.ofSeconds(firstBackoffSeconds),
                Duration.ofMillis(MILLIS_PER_MINUTE / requestsPerMinute));
    }

    static Supplier<String> keyFrom(String envName) {
        return () -> {
            String key = System.getenv(envName);
            if (key == null || key.isBlank()) {
                throw new LlmException("Environment variable " + envName + " is not set", 0, false);
            }
            return key;
        };
    }
}
