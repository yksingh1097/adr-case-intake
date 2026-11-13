package dev.adrintake.runner;

import dev.adrintake.config.RepoDirs;
import dev.adrintake.model.ExtractionResult;
import dev.adrintake.service.ExtractionPipeline;
import dev.adrintake.service.ResultWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * The extraction command. Runs only with --app.pipeline.enabled=true: reads documents/*.txt, writes out/&lt;id&gt;.json.
 * A document that fails does not stop the others; the run ends with an error if any failed.
 */
@Component
@ConditionalOnProperty(name = "app.pipeline.enabled", havingValue = "true")
public class PipelineRunner implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(PipelineRunner.class);
    private static final String TEXT_SUFFIX = ".txt";

    private final ExtractionPipeline pipeline;
    private final Path documentsDir;
    private final Path outDir;
    private final Set<String> only;

    public PipelineRunner(ExtractionPipeline pipeline,
            @Value("${app.documents-dir:}") String documentsDir,
            @Value("${app.out-dir:}") String outDir,
            @Value("${app.pipeline.only:}") String only) {
        this.pipeline = pipeline;
        this.documentsDir = RepoDirs.resolve(documentsDir, "documents");
        this.outDir = RepoDirs.resolve(outDir, "out");
        this.only = Set.of(only.isBlank() ? new String[0] : only.split("\\s*,\\s*"));
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        List<String> failures = new ArrayList<>();
        for (Path file : documentFiles()) {
            String name = file.getFileName().toString();
            String id = name.substring(0, name.length() - TEXT_SUFFIX.length());
            if (!only.isEmpty() && !only.contains(id)) {
                continue;
            }
            try {
                ExtractionResult result = pipeline.process(id, Files.readString(file));
                Path written = ResultWriter.write(outDir, result);
                LOG.info("{}: {} case(s), flags {} -> {}", id, result.cases().size(), result.flags(), written);
            } catch (RuntimeException e) {
                LOG.error("{}: failed: {}", id, e.getMessage());
                failures.add(id);
            }
        }
        if (!failures.isEmpty()) {
            throw new IllegalStateException("No output for: " + String.join(", ", failures));
        }
    }

    private List<Path> documentFiles() throws IOException {
        try (Stream<Path> files = Files.list(documentsDir)) {
            return files.filter(f -> f.getFileName().toString().endsWith(TEXT_SUFFIX)).sorted().toList();
        }
    }
}
