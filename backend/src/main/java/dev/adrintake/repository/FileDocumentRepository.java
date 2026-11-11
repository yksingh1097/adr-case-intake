package dev.adrintake.repository;

import dev.adrintake.model.DocumentDetail;
import dev.adrintake.model.DocumentSummary;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Repository
public class FileDocumentRepository implements DocumentRepository {

    public static final String FLAG_UNREADABLE = "unreadable_output";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final Pattern ID = Pattern.compile("[A-Za-z0-9_-]{1,64}");
    private static final Pattern SUBJECT = Pattern.compile("^(?:subject|re)\\s*:\\s*(.+)$", Pattern.CASE_INSENSITIVE);
    private static final int TITLE_SCAN_LINES = 15;

    private final Path documentsDir;
    private final Path outDir;

    public FileDocumentRepository(@Value("${app.documents-dir:}") String documentsDir, @Value("${app.out-dir:}") String outDir) {
        this.documentsDir = resolve(documentsDir, "documents"); 
        this.outDir = resolve(outDir, "out");
    }

    private static Path resolve(String configured, String folder) {
        if (!configured.isBlank()) {
            return Path.of(configured);
        }
        for (String candidate : List.of(folder, "../" + folder)) {
            Path path = Path.of(candidate);
            if (Files.isDirectory(path)) {
                return path;
            }
        }
        return Path.of(folder);
    }

    @Override
    public List<DocumentSummary> findAll() {
        if (!Files.isDirectory(outDir)) {
            return List.of();
        }
        List<DocumentSummary> summaries = new ArrayList<>();
        try (Stream<Path> files = Files.list(outDir)) {
            for (Path file : files.filter(f -> f.getFileName().toString().endsWith(".json")).sorted().toList()) {
                String name = file.getFileName().toString();
                String id = name.substring(0, name.length() - ".json".length());
                if (!ID.matcher(id).matches()) {
                    continue;
                }
                String title = titleOf(id, readText(id));
                try {
                    JsonNode extraction = objectMapper.readTree(file.toFile());
                    summaries.add(new DocumentSummary(id, title, extraction.path("cases").size(), flagsOf(extraction)));
                } catch (IOException e) {
                    summaries.add(new DocumentSummary(id, title, 0, List.of(FLAG_UNREADABLE)));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return summaries;
    }

    @Override
    public Optional<DocumentDetail> findById(String id) {
        if (!ID.matcher(id).matches()) {
            return Optional.empty();
        }
        Path file = outDir.resolve(id + ".json");
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try {
            JsonNode extraction = objectMapper.readTree(file.toFile());
            String text = readText(id);
            return Optional.of(new DocumentDetail(id, titleOf(id, text), text, extraction));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + file, e);
        }
    }

    private String readText(String id) {
        for (Path candidate : List.of(documentsDir.resolve(id + ".txt"), documentsDir.resolve("stretch").resolve(id + ".txt"))) {
            if (Files.isRegularFile(candidate)) {
                try {
                    return Files.readString(candidate);
                } catch (IOException e) {
                    throw new UncheckedIOException("Cannot read " + candidate, e);
                }
            }
        }
        return null;
    }

    private static String titleOf(String id, String text) {
        if (text == null) {
            return id;
        }
        String firstLine = null;
        String[] lines = text.split("\\R", TITLE_SCAN_LINES + 1);
        for (int i = 0; i < Math.min(lines.length, TITLE_SCAN_LINES); i++) {
            String line = lines[i].strip();
            Matcher subject = SUBJECT.matcher(line);
            if (subject.matches()) {
                return subject.group(1).strip();
            }
            if (firstLine == null && !line.isEmpty()) {
                firstLine = line;
            }
        }
        return firstLine != null ? firstLine : id;
    }

    private static List<String> flagsOf(JsonNode extraction) {
        List<String> flags = new ArrayList<>();
        extraction.path("flags").forEach(f -> flags.add(f.asText()));
        return flags;
    }
}