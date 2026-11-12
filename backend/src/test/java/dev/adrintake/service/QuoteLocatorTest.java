package dev.adrintake.service;

import dev.adrintake.model.Provenance;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

class QuoteLocatorTest {

    private static QuoteLocator locator;

    @BeforeAll
    static void loadD01() throws IOException {
        locator = new QuoteLocator(Files.readString(RepoPaths.root().resolve("documents/D01.txt")));
    }

    @Test
    void findsQuoteAtTheOffsetsFromTheExample() {
        Provenance p = locator.locate("54-year-old").orElseThrow();

        assertEquals(288, p.start());
        assertEquals(299, p.end());
    }

    @Test
    void ignoresCaseSpacesAndTrailingDot() {
        Provenance p = locator.locate("AMOXICILLIN   500 mg.").orElseThrow();

        assertEquals("amoxicillin 500 mg", p.quote());
        assertEquals(373, p.start());
        assertEquals(391, p.end());
    }

    @Test
    void returnsNothingWhenTheQuoteIsNotInTheDocument() {
        assertTrue(locator.locate("amoxicillin 900 mg").isEmpty());
    }

    @Test
    void returnsNothingForVeryShortQuotes() {
        assertTrue(locator.locate("54").isEmpty());
    }

    @Test
    void countsCodePointsNotJavaCharacters() {
        Provenance p = new QuoteLocator("\uD83D\uDE00 note: abc def").locate("abc def").orElseThrow();

        assertEquals(8, p.start());
    }
}