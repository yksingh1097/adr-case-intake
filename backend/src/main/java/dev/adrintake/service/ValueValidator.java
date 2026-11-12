package dev.adrintake.service;

import dev.adrintake.model.FieldDefinition;
import dev.adrintake.model.FieldType;
import dev.adrintake.model.Validation;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

public final class ValueValidator {

    public static final String EXCLUSIVE_OPTION_CODE = "none";

    private static final Pattern ISO_DATE_SHAPE = Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2}");

    private ValueValidator() {
    }

    public static Optional<String> check(FieldDefinition field, JsonNode value) {
        if (value == null || value.isNull() || value.isMissingNode()) {
            return Optional.of("value is missing");
        }
        return switch (field.type()) {
            case STRING -> checkString(field, value);
            case INTEGER, NUMBER -> checkNumber(field, value);
            case DATE -> checkDate(value);
            case CODED -> checkCoded(field, value);
            case CODED_MULTI -> checkCodedMulti(field, value);
            case STRING_LIST -> checkStringList(value);
        };
    }

    private static Optional<String> checkString(FieldDefinition field, JsonNode value) {
        if (!value.isTextual()) {
            return Optional.of("string expected");
        }
        Optional<Pattern> pattern = field.validation().compiledPattern();

        if (pattern.isPresent() && !pattern.get().matcher(value.asText()).lookingAt()) {
            return Optional.of("does not match pattern " + field.validation().pattern());
        }
        return Optional.empty();
    }

    private static Optional<String> checkNumber(FieldDefinition field, JsonNode value) {
        if (!value.isNumber()) {
            return Optional.of("number expected");
        }
        double number = value.doubleValue();
        if (!Double.isFinite(number)) {
            return Optional.of("finite number expected");
        }
        if (field.type() == FieldType.INTEGER && number != Math.rint(number)) {
            return Optional.of("integer expected");
        }
        Validation rules = field.validation();
        if (rules.min() != null && number < rules.min()) {
            return Optional.of("below min " + rules.min());
        }
        if (rules.max() != null && number > rules.max()) {
            return Optional.of("above max " + rules.max());
        }
        return Optional.empty();
    }

    private static Optional<String> checkDate(JsonNode value) {
        if (!value.isTextual() || !ISO_DATE_SHAPE.matcher(value.asText()).matches()) {
            return Optional.of("ISO date YYYY-MM-DD expected");
        }

        try {
            LocalDate.parse(value.asText());
        } catch (DateTimeException e) {
            return Optional.of("not a real calendar date: " + value.asText());
        }
        return Optional.empty();
    }

    private static Optional<String> checkCoded(FieldDefinition field, JsonNode value) {
        if (!value.isTextual() || !field.optionCodes().contains(value.asText())) {
            return Optional.of("not an option code: " + value);
        }
        return Optional.empty();
    }

    private static Optional<String> checkCodedMulti(FieldDefinition field, JsonNode value) {
        if (!value.isArray() || value.isEmpty()) {
            return Optional.of("non-empty list of option codes expected");
        }
        Set<String> codes = field.optionCodes();
        for (JsonNode item : value) {
            if (!item.isTextual() || !codes.contains(item.asText())) {
                return Optional.of("not an option code: " + item);
            }
        }
        if (value.size() > 1 && containsCode(value, EXCLUSIVE_OPTION_CODE)) {
            return Optional.of("'" + EXCLUSIVE_OPTION_CODE + "' must stand alone");
        }
        return Optional.empty();
    }

    private static Optional<String> checkStringList(JsonNode value) {
        if (!value.isArray()) {
            return Optional.of("list of strings expected");
        }
        for (JsonNode item : value) {
            if (!item.isTextual()) {
                return Optional.of("list of strings expected");
            }
        }
        return Optional.empty();
    }

    private static boolean containsCode(JsonNode array, String code) {
        for (JsonNode item : array) {
            if (item.isTextual() && item.asText().equals(code)) {
                return true;
            }
        }
        return false;
    }
}
