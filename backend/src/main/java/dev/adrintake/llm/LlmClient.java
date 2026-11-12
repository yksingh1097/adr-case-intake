package dev.adrintake.llm;

public interface LlmClient {

    LlmResponse complete(LlmRequest request);
}
