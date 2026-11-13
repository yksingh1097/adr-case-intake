import { useCallback, useEffect, useState } from "react";

export type Loadable<T> =
  | { state: "loading" }
  | { state: "error"; message: string }
  | { state: "ready"; data: T };

/** Runs a request when the inputs change and again on reload(). The previous request is cancelled. */
export function useAsync<T>(load: (signal: AbortSignal) => Promise<T>, inputs: unknown[]): [Loadable<T>, () => void] {
  const [result, setResult] = useState<Loadable<T>>({ state: "loading" });
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    setResult({ state: "loading" });
    load(controller.signal).then(
      (data) => setResult({ state: "ready", data }),
      (error: unknown) => {
        if (controller.signal.aborted) {
          return;
        }
        setResult({ state: "error", message: error instanceof Error ? error.message : "Something went wrong." });
      },
    );
    return () => controller.abort();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...inputs, attempt]);

  const reload = useCallback(() => setAttempt((n) => n + 1), []);
  return [result, reload];
}
