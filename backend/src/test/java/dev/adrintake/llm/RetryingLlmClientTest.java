package dev.adrintake.llm;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.*;

class RetryingLlmClientTest {

    private static final LlmRequest REQUEST = new LlmRequest("s", "p", null);
    private static final LlmResponse OK = new LlmResponse("{}", "m");
    private static final Duration BACKOFF = Duration.ofSeconds(10);

    private static final class Script implements LlmClient {
        private final Queue<Object> steps = new ArrayDeque<>();
        int calls;

        Script then(Object step) {
            steps.add(step);
            return this;
        }

        @Override
        public LlmResponse complete(LlmRequest request) {
            calls++;
            Object step = steps.remove();
            if (step instanceof LlmException e) {
                throw e;
            }
            return (LlmResponse) step;
        }
    }

    private final List<Duration> sleeps = new ArrayList<>();
    private long now = 0;

    private RetryingLlmClient over(Script script, int attempts, Duration gap) {
        return new RetryingLlmClient(script, attempts, BACKOFF, gap, d -> {
            sleeps.add(d);
            now += d.toNanos();
        }, () -> now);
    }

    private static LlmException transientFailure() {
        return new LlmException("busy", 503, true);
    }

    @Test
    void retriesWithDoublingBackoffUntilItWorks() {
        Script script = new Script().then(transientFailure()).then(transientFailure()).then(OK);

        LlmResponse response = over(script, 5, Duration.ZERO).complete(REQUEST);

        assertSame(OK, response);
        assertEquals(3, script.calls);
        assertEquals(List.of(Duration.ofSeconds(10), Duration.ofSeconds(20)), sleeps);
    }

    @Test
    void doesNotRetryAFailureThatCannotChange() {
        Script script = new Script().then(new LlmException("bad key", 401, false)).then(OK);

        assertThrows(LlmException.class, () -> over(script, 5, Duration.ZERO).complete(REQUEST));

        assertEquals(1, script.calls);
        assertTrue(sleeps.isEmpty());
    }

    @Test
    void givesUpAfterTheLastAttemptAndSaysSo() {
        Script script = new Script().then(transientFailure()).then(transientFailure()).then(transientFailure());

        LlmException e = assertThrows(LlmException.class, () -> over(script, 3, Duration.ZERO).complete(REQUEST));

        assertEquals(3, script.calls);
        assertTrue(e.getMessage().contains("gave up after 3 attempts"));
        assertEquals(503, e.status());
        assertFalse(e.retryable());
    }

    @Test
    void spacesCallsByTheMinimumGap() {
        Script script = new Script().then(OK).then(OK).then(OK);
        RetryingLlmClient client = over(script, 1, Duration.ofSeconds(12));

        client.complete(REQUEST);
        client.complete(REQUEST);
        client.complete(REQUEST);

        assertEquals(List.of(Duration.ofSeconds(12), Duration.ofSeconds(12)), sleeps);
    }

    @Test
    void doesNotWaitWhenEnoughTimeHasPassedAlready() {
        Script script = new Script().then(OK).then(OK);
        RetryingLlmClient client = over(script, 1, Duration.ofSeconds(12));

        client.complete(REQUEST);
        now += Duration.ofSeconds(30).toNanos();
        client.complete(REQUEST);

        assertTrue(sleeps.isEmpty());
    }

    @Test
    void zeroAttemptsIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> over(new Script(), 0, Duration.ZERO));
    }
}
