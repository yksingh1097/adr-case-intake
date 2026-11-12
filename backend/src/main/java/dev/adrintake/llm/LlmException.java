package dev.adrintake.llm;

public final class LlmException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int status;
    private final boolean retryable;

    public LlmException(String message, int status, boolean retryable) {
        super(message);
        this.status = status;
        this.retryable = retryable;
    }

    public LlmException(String message, LlmException cause) {
        super(message, cause);
        this.status = cause.status;
        this.retryable = false;
    }

    public int status() {
        return status;
    }

    public boolean retryable() {
        return retryable;
    }
}
