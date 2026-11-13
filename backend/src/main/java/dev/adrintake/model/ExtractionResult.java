package dev.adrintake.model;

import java.util.List;

/** Everything written to out/&lt;documentId&gt;.json. {@code model} is null when no model was called. */
public record ExtractionResult(String documentId, String catalogVersion, String model, List<String> flags, List<CaseResult> cases) {

    public ExtractionResult {
        flags = List.copyOf(flags);
        cases = List.copyOf(cases);
    }
}
