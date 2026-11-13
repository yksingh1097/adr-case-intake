// Shapes of what the backend serves. The catalog is the only place that names fields, so field ids are plain strings here.

export type FieldType = "string" | "integer" | "number" | "date" | "coded" | "coded_multi" | "string_list";

export type Status = "extracted" | "unknown" | "conflict";

export interface FieldOption {
  code: string;
  label: string;
}

export interface FieldValidation {
  pattern?: string;
  min?: number;
  max?: number;
}

export interface FieldDefinition {
  id: string;
  label: string;
  section: string;
  type: FieldType;
  options?: FieldOption[];
  validation?: FieldValidation;
  guidance?: string;
}

export interface Section {
  id: string;
  label: string;
}

export interface Catalog {
  catalog_version: string;
  statuses: Record<string, string>;
  /** Ordered from the most to the least confident. */
  confidence_levels: string[];
  sections: Section[];
  fields: FieldDefinition[];
}

export interface DocumentSummary {
  id: string;
  title: string;
  caseCount: number;
  flags: string[];
}

export interface Provenance {
  quote: string;
  /** Code point offsets into the document text. */
  start: number;
  end: number;
}

export type FieldValue = string | number | string[] | null;

export interface FieldResult {
  status: Status;
  value?: FieldValue;
  confidence?: string;
  provenance?: Provenance[];
  note?: string;
}

export interface CaseResult {
  case_index: number;
  label: string;
  fields: Record<string, FieldResult>;
}

export interface Extraction {
  document_id: string;
  catalog_version: string;
  model?: string;
  flags: string[];
  cases: CaseResult[];
}

export interface DocumentDetail {
  id: string;
  title: string;
  /** Null when there is no text version of the document. */
  text: string | null;
  extraction: Extraction;
}
