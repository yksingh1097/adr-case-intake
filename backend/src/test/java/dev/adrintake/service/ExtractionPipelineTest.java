package dev.adrintake.service;

import dev.adrintake.exception.ExtractionException;
import dev.adrintake.llm.LlmClient;
import dev.adrintake.llm.LlmRequest;
import dev.adrintake.llm.LlmResponse;
import dev.adrintake.model.ExtractionResult;
import dev.adrintake.model.Status;
import dev.adrintake.testsupport.TestCatalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExtractionPipelineTest {

    private static final String TEMPLATE = "{{statuses}} {{confidence_levels}} {{fields}}";
    private static final String TEXT = "Patient MK, aged 54, took the drug.";
    private static final String ANSWER = """
            {"flags": [], "cases": [{"label": "MK", "fields": {
              "age": {"status": "extracted", "value": 54, "confidence": "high", "quotes": ["aged 54"]}}}]}""";

    private final List<LlmRequest> seen = new ArrayList<>();

    private ExtractionPipeline pipelineAnswering(String text) {
        LlmClient llm = request -> {
            seen.add(request);
            return new LlmResponse(text, "model-x");
        };
        return new ExtractionPipeline(TestCatalog.catalog(), llm, TEMPLATE, "prov");
    }

    @Test
    void sendsOneRequestNamedAfterTheDocumentAndBuildsTheResult() {
        ExtractionResult result = pipelineAnswering(ANSWER).process("D01", TEXT);

        assertEquals(1, seen.size());
        assertEquals("D01", seen.get(0).exchangeId());
        assertTrue(seen.get(0).prompt().contains(TEXT));
        assertEquals("D01", result.documentId());
        assertEquals("t1", result.catalogVersion());
        assertEquals("prov/model-x", result.model());
        assertEquals(Status.EXTRACTED, result.cases().get(0).fields().get("age").status());
    }

    @Test
    void answerThatIsNotJsonIsAnErrorNotAnEmptyResult() {
        ExtractionException e = assertThrows(ExtractionException.class, () -> pipelineAnswering("not json").process("D01", TEXT));

        assertTrue(e.getMessage().startsWith("D01"));
    }

    @Test
    void unsupportedDocumentHasTheFlagNoCasesAndCallsNoModel() {
        ExtractionResult result = pipelineAnswering(ANSWER).unsupported("S03");

        assertEquals(List.of(ExtractionPipeline.FLAG_UNSUPPORTED_FORMAT), result.flags());
        assertTrue(result.cases().isEmpty());
        assertTrue(seen.isEmpty());
    }

    @Test
    void writtenFileFollowsTheOutputShape() throws IOException {
        ExtractionResult result = pipelineAnswering(ANSWER).process("D01", TEXT);
        Path dir = Files.createTempDirectory("out");

        Path file = ResultWriter.write(dir, result);

        JsonNode json = new ObjectMapper().readTree(Files.readString(file));
        assertEquals("D01.json", file.getFileName().toString());
        assertEquals("D01", json.path("document_id").asText());
        JsonNode age = json.path("cases").get(0).path("fields").path("age");
        assertEquals("extracted", age.path("status").asText());
        assertEquals(54, age.path("value").asInt());
        assertEquals(12, age.path("provenance").get(0).path("start").asInt());
        JsonNode sex = json.path("cases").get(0).path("fields").path("sex");
        assertEquals("unknown", sex.path("status").asText());
        assertFalse(sex.has("value"));
        assertFalse(Files.readString(file).contains("\r"));
    }
}
