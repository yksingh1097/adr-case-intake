package dev.adrintake.model;

import java.util.Arrays;
import java.util.Optional;

public enum Status {
    EXTRACTED("extracted"),
    UNKNOWN("unknown"),
    CONFLICT("conflict");

    private final String jsonName;

    Status(String jsonName) {
        this.jsonName = jsonName;
    }

    public String jsonName() {
        return jsonName;
    }

    public static Optional<Status> fromJsonName(String name) {
        return Arrays.stream(values()).filter(s -> s.jsonName.equals(name)).findFirst();
    }
}
