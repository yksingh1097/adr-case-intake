import type { DocumentSummary } from "../api/types";
import { humanize, pluralCases } from "../lib/text";

interface Props {
  documents: DocumentSummary[];
  activeId: string | undefined;
  onSelect: (id: string) => void;
}

export function DocumentList({ documents, activeId, onSelect }: Props) {
  return (
    <ul className="doc-list">
      {documents.map((doc) => (
        <li key={doc.id}>
          <button type="button" className={`doc${doc.id === activeId ? " is-active" : ""}`} aria-current={doc.id === activeId} onClick={() => onSelect(doc.id)}>
            <span className="doc__title">{doc.title}</span>
            <span className="doc__meta">
              {doc.id} · {pluralCases(doc.caseCount)}
            </span>
            {doc.flags.length > 0 && (
              <span className="doc__flags">
                {doc.flags.map((flag) => (
                  <span key={flag} className="chip chip--flag">
                    {humanize(flag)}
                  </span>
                ))}
              </span>
            )}
          </button>
        </li>
      ))}
    </ul>
  );
}
