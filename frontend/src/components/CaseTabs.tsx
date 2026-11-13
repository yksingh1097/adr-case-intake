import type { CaseResult } from "../api/types";

interface Props {
  cases: CaseResult[];
  active: number;
  onChange: (index: number) => void;
}

export function CaseTabs({ cases, active, onChange }: Props) {
  return (
    <div className="tabs" role="tablist" aria-label="Cases in this document">
      {cases.map((item, index) => (
        <button
          key={item.case_index}
          type="button"
          role="tab"
          aria-selected={index === active}
          className={`tab${index === active ? " is-active" : ""}`}
          onClick={() => onChange(index)}
        >
          {item.label}
        </button>
      ))}
    </div>
  );
}
