import type { Catalog, FieldDefinition, FieldResult } from "../api/types";
import { riskOf } from "../lib/risk";
import { ConfidenceMeter } from "./ConfidenceMeter";
import { StatusChip } from "./StatusChip";
import { ValueControl } from "./ValueControl";

interface Props {
  catalog: Catalog;
  definition: FieldDefinition;
  result: FieldResult;
  selected: boolean;
  onSelect: () => void;
}

export function FieldRow({ catalog, definition, result, selected, onSelect }: Props) {
  const risk = riskOf(result, catalog.confidence_levels);
  const quotes = result.provenance ?? [];
  return (
    <li className={`field field--${risk}${selected ? " is-selected" : ""}`} onClick={onSelect}>
      <button type="button" className="field__head" aria-pressed={selected}>
        <span className="field__label">{definition.label}</span>
        <span className="field__marks">
          <StatusChip status={result.status} description={catalog.statuses[result.status]} />
          {result.status === "extracted" && <ConfidenceMeter level={result.confidence} levels={catalog.confidence_levels} />}
        </span>
      </button>
      <div className="field__body">
        <Value definition={definition} result={result} />
        {result.note && <p className="field__note">{result.note}</p>}
        {quotes.length > 0 && (
          <ul className="quotes" aria-label="Quotes from the document">
            {quotes.map((quote) => (
              <li key={`${quote.start}-${quote.end}`}>
                <q>{quote.quote}</q>
              </li>
            ))}
          </ul>
        )}
        {selected && definition.guidance && <p className="field__guidance">{definition.guidance}</p>}
      </div>
    </li>
  );
}

function Value({ definition, result }: { definition: FieldDefinition; result: FieldResult }) {
  if (result.status === "unknown") {
    return <p className="placeholder">Not stated in the document</p>;
  }
  if (result.status === "conflict") {
    return <p className="placeholder">The document states different values</p>;
  }
  return <ValueControl definition={definition} value={result.value} />;
}
