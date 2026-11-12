package dev.adrintake.service;

import dev.adrintake.model.Provenance;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class QuoteLocator {

    public static final int MIN_QUOTE_LENGTH = 3;

    private final String document;

    public QuoteLocator(String document) {
        this.document = document;
    }

    public Optional<Provenance> locate(String quote) {
        String cleaned = stripTrailingDots(quote.strip());
        if (cleaned.length() < MIN_QUOTE_LENGTH) {
            return Optional.empty();
        }
        Matcher match = toPattern(cleaned).matcher(document);
        if (!match.find()) {
            return Optional.empty();
        }
        int start = document.codePointCount(0, match.start());
        int end = document.codePointCount(0, match.end());
        return Optional.of(new Provenance(match.group(), start, end));
    }

    private static Pattern toPattern(String quote) {
        StringBuilder regex = new StringBuilder();
        for (String word : quote.split("\\s+")) {
            if (regex.length() > 0) {
                regex.append("\\s+");
            }
            word.codePoints().forEach(cp -> regex.append(charPattern(cp)));
        }
        return Pattern.compile(regex.toString(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS);
    }

    private static String charPattern(int cp) {
        if (cp == '-' || cp == '\u2013' || cp == '\u2014') {
            return "[-\u2013\u2014]";
        }
        if (cp == '\'' || cp == '\u2019') {
            return "['\u2019]";
        }
        return Pattern.quote(new String(Character.toChars(cp)));
    }

    private static String stripTrailingDots(String s) {
        int end = s.length();
        while (end > 0 && s.charAt(end - 1) == '.') {
            end--;
        }
        return s.substring(0, end);
    }
}