package dev.adrintake.llm;

import java.time.Duration;
import java.util.function.LongSupplier;

public final class RetryingLlmClient implements LlmClient {

    interface Sleeper {
        void sleep(Duration duration) throws InterruptedException;
    }

    private final LlmClient delegate;
    private final int maxAttempts;
    private final Duration firstBackoff;
    private final Duration minGap;
    private final Sleeper sleeper;
    private final LongSupplier nanoClock;

    private long nextCallAt;

    public RetryingLlmClient(LlmClient delegate, int maxAttempts, Duration firstBackoff, Duration minGap) {
        this(delegate, maxAttempts, firstBackoff, minGap, d -> Thread.sleep(d), System::nanoTime);
    }

    RetryingLlmClient(LlmClient delegate, int maxAttempts, Duration firstBackoff, Duration minGap,
            Sleeper sleeper, LongSupplier nanoClock) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be at least 1, got " + maxAttempts);
        }
        this.delegate = delegate;
        this.maxAttempts = maxAttempts;
        this.firstBackoff = firstBackoff;
        this.minGap = minGap;
        this.sleeper = sleeper;
        this.nanoClock = nanoClock;
        this.nextCallAt = nanoClock.getAsLong();
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        for (int attempt = 1;; attempt++) {
            waitForTurn();
            try {
                return delegate.complete(request);
            } catch (LlmException e) {
                if (!e.retryable()) {
                    throw e;
                }
                if (attempt == maxAttempts) {
                    throw new LlmException(e.getMessage() + " (gave up after " + attempt + " attempts)", e);
                }
                pause(firstBackoff.multipliedBy(1L << (attempt - 1)));
            }
        }
    }

    private synchronized void waitForTurn() {
        long now = nanoClock.getAsLong();
        long wait = nextCallAt - now;
        if (wait > 0) {
            pause(Duration.ofNanos(wait));
            now = nextCallAt;
        }
        nextCallAt = now + minGap.toNanos();
    }

    private void pause(Duration duration) {
        try {
            sleeper.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmException("Interrupted while waiting to call the model", 0, false);
        }
    }
}
