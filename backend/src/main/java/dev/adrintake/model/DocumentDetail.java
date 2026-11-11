package dev.adrintake.model;

import com.fasterxml.jackson.databind.JsonNode;

public record DocumentDetail(String id, String title, String text, JsonNode extraction) {
}