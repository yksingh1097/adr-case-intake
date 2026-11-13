import { useState } from "react";

import type { Catalog, CaseResult, FieldDefinition, FieldResult } from "../api/types";
import { needsAttention, summarize } from "../lib/risk";
import { FieldRow } from "./FieldRow";

interface Props {
  catalog: Catalog;
  item: CaseResult;
  selectedFieldId: string | null;
  onSelect: (fieldId: string) => void;
}

type Filter = "all" | "attention";

const MISSING: FieldResult = { status: "unknown", note: "This field is not in the output file." };

export function FieldsPane({ catalog, item, selectedFieldId, onSelect }: Props) {
  const [filter, setFilter] = useState<Filter>("all");
  const levels = catalog.confidence_levels;
  const resultOf = (field: FieldDefinition): FieldResult => item.fields[field.id] ?? MISSING;

  const summary = summarize(catalog.fields, item.fields, levels);
  const attentionCount = summary.conflict + summary.unknown + summary.uncertain;
  const visible = (field: FieldDefinition) => filter === "all" || needsAttention(resultOf(field), levels);
  const groups = groupBySection(catalog).map((group) => ({ ...group, fields: group.fields.filter(visible) })).filter((g) => g.fields.length > 0);

  return (
    <div className="fields">
      <div className="fields__bar">
        <p className="summary">
          {attentionCount === 0 ? (
            "Every value was extracted with the highest confidence."
          ) : (
            <>
              {summary.conflict > 0 && <span className="summary__item summary__item--conflict">{summary.conflict} in conflict</span>}
              {summary.uncertain > 0 && <span className="summary__item summary__item--low">{summary.uncertain} less certain</span>}
              {summary.unknown > 0 && <span className="summary__item summary__item--unknown">{summary.unknown} unknown</span>}
            </>
          )}
        </p>
        <div className="segmented" role="group" aria-label="Which fields to show">
          <button type="button" aria-pressed={filter === "all"} onClick={() => setFilter("all")}>
            All fields
          </button>
          <button type="button" aria-pressed={filter === "attention"} onClick={() => setFilter("attention")}>
            Needs attention ({attentionCount})
          </button>
        </div>
      </div>

      {groups.length === 0 && <p className="fields__none">Nothing needs attention in this case.</p>}
      {groups.map((group) => (
        <section key={group.id} className="group" aria-labelledby={`group-${group.id}`}>
          <h3 id={`group-${group.id}`}>{group.label}</h3>
          <ul className="field-list">
            {group.fields.map((field) => (
              <FieldRow
                key={field.id}
                catalog={catalog}
                definition={field}
                result={resultOf(field)}
                selected={field.id === selectedFieldId}
                onSelect={() => onSelect(field.id)}
              />
            ))}
          </ul>
        </section>
      ))}
    </div>
  );
}

/** Sections in catalog order, then any field whose section the catalog does not list. */
function groupBySection(catalog: Catalog): { id: string; label: string; fields: FieldDefinition[] }[] {
  const known = new Set(catalog.sections.map((s) => s.id));
  const groups = catalog.sections.map((section) => ({
    id: section.id,
    label: section.label,
    fields: catalog.fields.filter((f) => f.section === section.id),
  }));
  const stray = catalog.fields.filter((f) => !known.has(f.section));
  return stray.length > 0 ? [...groups, { id: "other", label: "Other", fields: stray }] : groups;
}
