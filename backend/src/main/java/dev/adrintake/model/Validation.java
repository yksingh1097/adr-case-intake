package dev.adrintake.model;

import java.util.Optional;
import java.util.regex.Pattern;

public record Validation(String pattern, Double min, Double max) {

    public static final Validation NONE = new Validation(null, null, null);

    public static Pattern compile(String pattern) {
        return Pattern.compile(pattern, Pattern.UNIX_LINES);
    }

    public Optional<Pattern> compiledPattern() {
        return Optional.ofNullable(pattern).map(Validation::compile);
    }
}