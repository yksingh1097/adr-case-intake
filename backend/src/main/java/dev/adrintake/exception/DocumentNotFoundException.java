package dev.adrintake.exception;

public final class DocumentNotFoundException extends RuntimeException {

    public DocumentNotFoundException(String id) {
        super("No document " + id);
    }
}