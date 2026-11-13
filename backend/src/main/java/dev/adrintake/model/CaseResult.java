package dev.adrintake.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record CaseResult(int caseIndex, String label, Map<String, FieldResult> fields) {

    public CaseResult {
        fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
    }
}
