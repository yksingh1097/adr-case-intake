package dev.adrintake.service;

import dev.adrintake.exception.ExtractionException;
import dev.adrintake.model.CaseResult;
import dev.adrintake.model.ExtractionResult;
import dev.adrintake.model.FieldResult;
import dev.adrintake.model.Status;
import dev.adrintake.testsupport.TestCatalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ResultAssemblerTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String TEXT = "Patient MK, aged 54, took the drug on 2 March 2026. Rash followed.";

    private final ResultAssembler assembler = new ResultAssembler(TestCatalog.catalog());

    private static JsonNode json(String text) {
        try {
            return JSON.readTree(text);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private ExtractionResult assembleWith(String fieldsJson) {
        JsonNode answer = json("{\"flags\": [], \"cases\": [{\"label\": \"Patient MK\", \"fields\": " + fieldsJson + "}]}");
        return assembler.assemble("X1", TEXT, answer, "test/model");
    }

    private FieldResult field(ExtractionResult result, String id) {
        return result.cases().get(0).fields().get(id);
    }

    @Test
    void extractedValueGetsLocatedQuotes() {
        ExtractionResult result = assembleWith("""
                {"age": {"status": "extracted", "value": 54, "confidence": "high", "quotes": ["aged 54"]}}""");

        FieldResult age = field(result, "age");
        assertEquals(Status.EXTRACTED, age.status());
        assertEquals(54, age.value().asInt());
        assertEquals("high", age.confidence());
        assertEquals(12, age.provenance().get(0).start());
        assertEquals(19, age.provenance().get(0).end());
    }

    @Test
    void everyCatalogFieldAppearsEvenWhenTheModelSkippedIt() {
        ExtractionResult result = assembleWith("{}");

        CaseResult c = result.cases().get(0);
        assertEquals(TestCatalog.catalog().fields().size(), c.fields().size());
        assertEquals(Status.UNKNOWN, field(result, "sex").status());
        assertEquals(ResultAssembler.NOTE_NO_ANSWER, field(result, "sex").note());
    }

    @Test
    void valueThatBreaksACatalogRuleBecomesUnknownWithTheReason() {
        ExtractionResult result = assembleWith("""
                {"age": {"status": "extracted", "value": 200, "confidence": "high", "quotes": ["aged 54"]}}""");

        FieldResult age = field(result, "age");
        assertEquals(Status.UNKNOWN, age.status());
        assertNull(age.value());
        assertTrue(age.note().contains("above max"));
    }

    @Test
    void quoteThatIsNotInTheDocumentBecomesUnknown() {
        ExtractionResult result = assembleWith("""
                {"age": {"status": "extracted", "value": 54, "confidence": "high", "quotes": ["aged 55"]}}""");

        assertEquals(Status.UNKNOWN, field(result, "age").status());
        assertTrue(field(result, "age").note().contains("no quote"));
    }

    @Test
    void oneFoundQuoteIsEnoughAndTheMissingOneIsDropped() {
        ExtractionResult result = assembleWith("""
                {"age": {"status": "extracted", "value": 54, "confidence": "medium", "quotes": ["aged 55", "aged 54"]}}""");

        assertEquals(1, field(result, "age").provenance().size());
    }

    @Test
    void confidenceMustBeOneOfTheCatalogLevels() {
        ExtractionResult result = assembleWith("""
                {"age": {"status": "extracted", "value": 54, "confidence": "certain", "quotes": ["aged 54"]},
                 "sex": {"status": "extracted", "value": "m", "quotes": ["Patient"]}}""");

        assertEquals(Status.UNKNOWN, field(result, "age").status());
        assertEquals(Status.UNKNOWN, field(result, "sex").status());
    }

    @Test
    void invalidStatusBecomesUnknown() {
        ExtractionResult result = assembleWith("""
                {"age": {"status": "maybe", "value": 54, "confidence": "high", "quotes": ["aged 54"]}}""");

        assertEquals(Status.UNKNOWN, field(result, "age").status());
        assertTrue(field(result, "age").note().contains("maybe"));
    }

    @Test
    void conflictKeepsItsNoteAndHasNoValue() {
        ExtractionResult result = assembleWith("""
                {"age": {"status": "conflict", "note": "54 and 56", "quotes": []}}""");

        FieldResult age = field(result, "age");
        assertEquals(Status.CONFLICT, age.status());
        assertNull(age.value());
        assertEquals("54 and 56", age.note());
    }

    @Test
    void integerWrittenAsDecimalIsStoredAsWholeNumber() {
        ExtractionResult result = assembleWith("""
                {"age": {"status": "extracted", "value": 54.0, "confidence": "high", "quotes": ["aged 54"]}}""");

        assertTrue(field(result, "age").value().isIntegralNumber());
    }

    @Test
    void fieldTheModelInventedIsIgnored() {
        ExtractionResult result = assembleWith("""
                {"shoe_size": {"status": "extracted", "value": 9, "confidence": "high", "quotes": ["aged 54"]}}""");

        assertNull(result.cases().get(0).fields().get("shoe_size"));
    }

    @Test
    void newCatalogFieldMissingFromTheAnswerIsUnknown() {
        ResultAssembler wider = new ResultAssembler(TestCatalog.catalogWith("""
                {"id": "weight", "label": "Weight", "section": "s1", "type": "number", "validation": {"min": 0}}"""));
        JsonNode answer = json("""
                {"flags": [], "cases": [{"label": "A", "fields": {"age": {"status": "extracted", "value": 54, "confidence": "high", "quotes": ["aged 54"]}}}]}""");

        ExtractionResult result = wider.assemble("X1", TEXT, answer, "test/model");

        assertEquals(Status.EXTRACTED, result.cases().get(0).fields().get("age").status());
        assertEquals(Status.UNKNOWN, result.cases().get(0).fields().get("weight").status());
    }

    @Test
    void noCasesKeepsTheFlags() {
        ExtractionResult result = assembler.assemble("X1", TEXT, json("{\"flags\": [\"no_adverse_event\", \"no_adverse_event\"], \"cases\": []}"), "m");

        assertTrue(result.cases().isEmpty());
        assertEquals(1, result.flags().size());
        assertEquals("no_adverse_event", result.flags().get(0));
    }

    @Test
    void caseWithoutLabelGetsANumberedOne() {
        ExtractionResult result = assembler.assemble("X1", TEXT, json("{\"flags\": [], \"cases\": [{\"fields\": {}}, {\"label\": \" \", \"fields\": {}}]}"), "m");

        assertEquals("Case 1", result.cases().get(0).label());
        assertEquals("Case 2", result.cases().get(1).label());
        assertEquals(1, result.cases().get(1).caseIndex());
    }

    @Test
    void answerWithoutCasesListIsAnError() {
        assertThrows(ExtractionException.class, () -> assembler.assemble("X1", TEXT, json("{\"flags\": []}"), "m"));
    }
}
