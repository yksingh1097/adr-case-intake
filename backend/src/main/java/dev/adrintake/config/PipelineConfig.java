package dev.adrintake.config;

import dev.adrintake.llm.LlmClient;
import dev.adrintake.model.Catalog;
import dev.adrintake.service.ExtractionPipeline;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

@Configuration
public class PipelineConfig {

    private static final String PROMPT_TEMPLATE = "prompts/system.txt";

    @Bean
    public ExtractionPipeline extractionPipeline(Catalog catalog, LlmClient llm, @Value("${app.llm.provider}") String provider) {
        return new ExtractionPipeline(catalog, llm, promptTemplate(), provider);
    }

    private static String promptTemplate() {
        try (var in = new ClassPathResource(PROMPT_TEMPLATE).getInputStream()) {
            return StreamUtils.copyToString(in, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + PROMPT_TEMPLATE, e);
        }
    }
}
