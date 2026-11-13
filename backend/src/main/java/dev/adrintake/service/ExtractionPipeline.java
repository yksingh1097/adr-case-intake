package dev.adrintake.service;

import dev.adrintake.exception.ExtractionException;
import dev.adrintake.llm.LlmClient;
import dev.adrintake.llm.LlmRequest;
import dev.adrintake.llm.LlmResponse;
import dev.adrintake.model.Catalog;
import dev.adrintake.model.ExtractionResult;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * One model call per document: the longest document is about 12,700 characters, which fits a single request,
 * so there is no chunking to get wrong.
 */
public final class ExtractionPipeline {

    public static final String FLAG_UNSUPPORTED_FORMAT = "unsupported_format";

    private static final ObjectMapper JSON = new ObjectMapper();

    private final Catalog catalog;
    private final LlmClient llm;
    private final String provider;
    private final PromptBuilder prompts;
    private final JsonNode schema;
    private final ResultAssembler assembler;

    public ExtractionPipeline(Catalog catalog, LlmClient llm, String promptTemplate, String provider) {
        this.catalog = catalog;
        this.llm = llm;
        this.provider = provider;
        this.prompts = new PromptBuilder(catalog, promptTemplate);
        this.schema = SchemaBuilder.build(catalog);
        this.assembler = new ResultAssembler(catalog);
    }

    public ExtractionResult process(String documentId, String text) {
        LlmResponse response = llm.complete(new LlmRequest(prompts.system(), prompts.prompt(text), schema, documentId));
        JsonNode answer;
        try {
            answer = JSON.readTree(response.text());
        } catch (JsonProcessingException e) {
            throw new ExtractionException(documentId + ": the model answer is not valid JSON: " + e.getOriginalMessage());
        }
        return assembler.assemble(documentId, text, answer, provider + "/" + response.model());
    }

    /** For a document that has no text version to read. No model is called. */
    public ExtractionResult unsupported(String documentId) {
        return new ExtractionResult(documentId, catalog.version(), null, List.of(FLAG_UNSUPPORTED_FORMAT), List.of());
    }
}
