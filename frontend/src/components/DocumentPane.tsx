import { useEffect, useMemo, useRef } from "react";

import type { DocumentDetail, Provenance } from "../api/types";
import { buildSegments } from "../lib/segments";
import { humanize } from "../lib/text";
import { EmptyState } from "./States";

interface Props {
  document: DocumentDetail;
  highlight: Provenance[];
}

export function DocumentPane({ document, highlight }: Props) {
  const firstMark = useRef<HTMLElement | null>(null);
  const segments = useMemo(() => (document.text === null ? [] : buildSegments(document.text, highlight)), [document.text, highlight]);

  useEffect(() => {
    const reduceMotion = window.matchMedia?.("(prefers-reduced-motion: reduce)").matches ?? false;
    firstMark.current?.scrollIntoView?.({ block: "center", behavior: reduceMotion ? "auto" : "smooth" });
  }, [segments]);

  const { extraction } = document;
  let markedSoFar = false;
  return (
    <article className="document" aria-label="Document text">
      <header className="document__head">
        <h2>{document.title}</h2>
        <p className="document__meta">
          {document.id}
          {extraction.model && <> · {extraction.model}</>}
        </p>
        {extraction.flags.length > 0 && (
          <ul className="flags">
            {extraction.flags.map((flag) => (
              <li key={flag} className="chip chip--flag">
                {humanize(flag)}
              </li>
            ))}
          </ul>
        )}
      </header>
      {document.text === null ? (
        <EmptyState title="No text version of this document">The values below cannot be checked against a text.</EmptyState>
      ) : (
        <pre className="document__text">
          {segments.map((segment, index) => {
            if (!segment.marked) {
              return segment.text;
            }
            const isFirst = !markedSoFar;
            markedSoFar = true;
            return (
              <mark key={index} ref={isFirst ? firstMark : undefined}>
                {segment.text}
              </mark>
            );
          })}
        </pre>
      )}
    </article>
  );
}
