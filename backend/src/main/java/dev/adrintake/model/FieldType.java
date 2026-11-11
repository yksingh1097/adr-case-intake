package dev.adrintake.model;

import java.util.Arrays;
import java.util.Optional;

public enum FieldType {
    STRING("string"),
    INTEGER("integer"),
    NUMBER("number"),
    DATE("date"),
    CODED("coded"),
    CODED_MULTI("coded_multi"),
    STRING_LIST("string_list");

    private final String jsonName;

    FieldType(String jsonName) {
        this.jsonName = jsonName;
    }

    public String jsonName() {
        return jsonName;
    }

    public boolean hasOptions() {
        return this == CODED || this == CODED_MULTI;
    }

    public boolean isNumeric() {
        return this == INTEGER || this == NUMBER;
    }


    public static Optional<FieldType> fromJsonName(String name) {
        return Arrays.stream(values()).filter(t -> t.jsonName.equals(name)).findFirst();
    }

    public static String allJsonNames() {
        return String.join(", ", Arrays.stream(values()).map(FieldType::jsonName).toList());
    }
}
