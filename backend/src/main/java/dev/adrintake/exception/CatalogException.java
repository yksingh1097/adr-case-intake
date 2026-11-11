package dev.adrintake.exception;

import java.util.List;

public final class CatalogException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient List<String> problems;

    public CatalogException(String problem) {
        this(List.of(problem));
    }

    public CatalogException(List<String> problems) {
        super(describe(problems));
        this.problems = List.copyOf(problems);
    }


    public List<String> problems() {
        return problems;
    }

    private static String describe(List<String> problems) {
        if (problems.size() == 1) {
            return problems.get(0);
        }
        return "Catalog has " + problems.size() + " problems:\n - " + String.join("\n - ", problems);
    }
}
