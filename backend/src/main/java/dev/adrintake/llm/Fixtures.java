package dev.adrintake.llm;

import java.nio.file.Path;
import java.util.regex.Pattern;

final class Fixtures {

    private static final Pattern ID = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    private Fixtures() {
    }

    static Path fileFor(Path dir, String exchangeId) {
        if (exchangeId == null || !ID.matcher(exchangeId).matches()) {
            throw new IllegalArgumentException("Fixture id must be 1-64 letters, digits, '_' or '-', got: " + exchangeId);
        }
        return dir.resolve(exchangeId + ".json");
    }
}
