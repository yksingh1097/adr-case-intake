import { useMemo, useState } from "react";

import { fetchDocument } from "../api/client";
import type { Catalog } from "../api/types";
import { useAsync } from "../lib/useAsync";
import { CaseTabs } from "./CaseTabs";
import { DocumentPane } from "./DocumentPane";
import { FieldsPane } from "./FieldsPane";
import { EmptyState, ErrorState, Loading } from "./States";

/** The review screen for one document. Mount it with key={documentId} so the selection starts fresh for each document. */
export function Review({ catalog, documentId }: { catalog: Catalog; documentId: string }) {
  const [loaded, reload] = useAsync((signal) => fetchDocument(documentId, signal), [documentId]);
  const [caseIndex, setCaseIndex] = useState(0);
  const [selectedFieldId, setSelectedFieldId] = useState<string | null>(null);

  const cases = loaded.state === "ready" ? loaded.data.extraction.cases : [];
  const activeCase = cases[caseIndex];
  const highlight = useMemo(
    () => (activeCase && selectedFieldId ? (activeCase.fields[selectedFieldId]?.provenance ?? []) : []),
    [activeCase, selectedFieldId],
  );

  if (loaded.state === "loading") {
    return <Loading label="Loading the document…" />;
  }
  if (loaded.state === "error") {
    return <ErrorState title="Could not load this document" message={loaded.message} onRetry={reload} />;
  }

  const { data: document } = loaded;
  return (
    <div className="review">
      <DocumentPane document={document} highlight={highlight} />
      <section className="review__fields" aria-label="Extracted fields">
        {cases.length > 1 && (
          <CaseTabs
            cases={cases}
            active={caseIndex}
            onChange={(index) => {
              setCaseIndex(index);
              setSelectedFieldId(null);
            }}
          />
        )}
        {activeCase ? (
          <>
            {cases.length === 1 && <h2 className="case-title">{activeCase.label}</h2>}
            <FieldsPane key={activeCase.case_index} catalog={catalog} item={activeCase} selectedFieldId={selectedFieldId} onSelect={setSelectedFieldId} />
          </>
        ) : (
          <EmptyState title="No case in this document">
            <p>A case needs an identifiable patient, a reporter, a suspect drug and an adverse event. This document does not have all four, so there is nothing to review.</p>
          </EmptyState>
        )}
      </section>
    </div>
  );
}
