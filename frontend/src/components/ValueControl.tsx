import type { FieldDefinition, FieldOption, FieldValue } from "../api/types";

interface Props {
  definition: FieldDefinition;
  value: FieldValue | undefined;
}

/** Shows an extracted value with the control that fits the catalog type. Read-only: reviewing, not editing. */
export function ValueControl({ definition, value }: Props) {
  switch (definition.type) {
    case "string":
      return <input className="control" type="text" readOnly tabIndex={-1} value={asText(value)} aria-label={definition.label} />;
    case "integer":
    case "number":
      return <input className="control control--short" type="number" readOnly tabIndex={-1} value={asText(value)} aria-label={definition.label} />;
    case "date":
      return <input className="control control--short" type="date" readOnly tabIndex={-1} value={asText(value)} aria-label={definition.label} />;
    case "coded":
      return <OptionGroup role="radio" label={definition.label} options={definition.options ?? []} chosen={asList(value)} />;
    case "coded_multi":
      return <OptionGroup role="checkbox" label={definition.label} options={definition.options ?? []} chosen={asList(value)} />;
    case "string_list":
      return <TagList label={definition.label} items={asList(value)} />;
  }
}

function OptionGroup({ role, label, options, chosen }: { role: "radio" | "checkbox"; label: string; options: FieldOption[]; chosen: string[] }) {
  const known = new Set(options.map((o) => o.code));
  const unlisted = chosen.filter((code) => !known.has(code));
  return (
    <div className="options" role={role === "radio" ? "radiogroup" : "group"} aria-label={label} aria-readonly="true">
      {options.map((option) => {
        const isChosen = chosen.includes(option.code);
        return (
          <span key={option.code} role={role} aria-checked={isChosen} className={`option${isChosen ? " is-chosen" : ""}`}>
            {option.label}
          </span>
        );
      })}
      {unlisted.map((code) => (
        <span key={code} className="option is-chosen is-unlisted" title="Not an option in the catalog">
          {code}
        </span>
      ))}
    </div>
  );
}

function TagList({ label, items }: { label: string; items: string[] }) {
  if (items.length === 0) {
    return <p className="none">None</p>;
  }
  return (
    <ul className="tags" aria-label={label}>
      {items.map((item) => (
        <li key={item}>{item}</li>
      ))}
    </ul>
  );
}

const asText = (value: FieldValue | undefined): string => (value === undefined || value === null || Array.isArray(value) ? "" : String(value));

const asList = (value: FieldValue | undefined): string[] =>
  Array.isArray(value) ? value : value === undefined || value === null ? [] : [String(value)];
