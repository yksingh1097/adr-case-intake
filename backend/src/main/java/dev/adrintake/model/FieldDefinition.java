package dev.adrintake.model;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;


public record FieldDefinition(
        String id,
        String label,
        String section,
        FieldType type,
        List<Option> options,
        Validation validation,
        String guidance) {

    public FieldDefinition {
        options = List.copyOf(options);
        validation = validation == null ? Validation.NONE : validation;
    }

    public Optional<String> guidanceText() {
        return Optional.ofNullable(guidance);
    }

    public Set<String> optionCodes() {
        Set<String> codes = new LinkedHashSet<>();
        options.forEach(o -> codes.add(o.code()));
        return codes;
    }
}
