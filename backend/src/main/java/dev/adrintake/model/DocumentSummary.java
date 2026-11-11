package dev.adrintake.model;

import java.util.List;

public record DocumentSummary(String id, String title, int caseCount, List<String> flags) {
}