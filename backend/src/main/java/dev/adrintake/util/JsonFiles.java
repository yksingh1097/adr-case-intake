package dev.adrintake.util;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Reads and writes JSON files the same way everywhere: UTF-8, two-space indent, "\n" line ends on every OS. */
public final class JsonFiles {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String LINE_END = "\n";

    private JsonFiles() {
    }

    public static JsonNode read(Path file) {
        try {
            return MAPPER.readTree(Files.readString(file));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + file, e);
        }
    }

    public static void write(Path file, JsonNode json) {
        DefaultIndenter indenter = new DefaultIndenter("  ", LINE_END);
        DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
        printer.indentObjectsWith(indenter);
        printer.indentArraysWith(indenter);
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            Files.writeString(file, MAPPER.writer(printer).writeValueAsString(json) + LINE_END);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write " + file, e);
        }
    }
}
