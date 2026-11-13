import { useState } from "react";

import { fetchCatalog, fetchDocuments } from "./api/client";
import { DocumentList } from "./components/DocumentList";
import { Review } from "./components/Review";
import { EmptyState, ErrorState, Loading } from "./components/States";
import { useAsync } from "./lib/useAsync";

export default function App() {
  const [catalog, reloadCatalog] = useAsync(fetchCatalog, []);
  const [documents, reloadDocuments] = useAsync(fetchDocuments, []);
  const [chosenId, setChosenId] = useState<string | null>(null);

  const activeId = chosenId ?? (documents.state === "ready" ? documents.data[0]?.id : undefined);

  return (
    <div className="app">
      <aside className="rail" aria-label="Documents">
        <h1 className="rail__title">Case review</h1>
        {documents.state === "loading" && <Loading label="Loading documents…" />}
        {documents.state === "error" && <ErrorState title="Could not load documents" message={documents.message} onRetry={reloadDocuments} />}
        {documents.state === "ready" && documents.data.length === 0 && (
          <EmptyState title="No documents yet">Run the pipeline or replay.sh to create outputs in the out folder.</EmptyState>
        )}
        {documents.state === "ready" && documents.data.length > 0 && <DocumentList documents={documents.data} activeId={activeId} onSelect={setChosenId} />}
      </aside>
      <main className="stage">
        {catalog.state === "loading" && <Loading label="Loading the field catalog…" />}
        {catalog.state === "error" && <ErrorState title="Could not load the field catalog" message={catalog.message} onRetry={reloadCatalog} />}
        {catalog.state === "ready" && activeId && <Review key={activeId} catalog={catalog.data} documentId={activeId} />}
        {catalog.state === "ready" && !activeId && documents.state === "ready" && <EmptyState title="Pick a document to review" />}
      </main>
    </div>
  );
}
