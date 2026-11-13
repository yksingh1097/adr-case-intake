import type { Catalog, DocumentDetail, DocumentSummary } from "../api/types";

// Invented field ids on purpose: the UI must render whatever the catalog says.
export const catalog: Catalog = {
  catalog_version: "test-1",
  statuses: { extracted: "Found.", unknown: "Not stated.", conflict: "Two values." },
  confidence_levels: ["high", "medium", "low"],
  sections: [
    { id: "who", label: "Who" },
    { id: "what", label: "What happened" },
  ],
  fields: [
    { id: "shoe_size", label: "Shoe size", section: "who", type: "integer" },
    { id: "mood", label: "Mood", section: "who", type: "coded", options: [{ code: "calm", label: "Calm" }, { code: "tense", label: "Tense" }] },
    { id: "symptoms", label: "Symptoms", section: "what", type: "coded_multi", options: [{ code: "itch", label: "Itching" }, { code: "swelling", label: "Swelling" }, { code: "none", label: "None" }], guidance: "Pick every symptom named." },
    { id: "seen_on", label: "Seen on", section: "what", type: "date" },
    { id: "nickname", label: "Nickname", section: "what", type: "string" },
    { id: "pets", label: "Pets", section: "what", type: "string_list" },
  ],
};

export const TEXT = "Patient wears size 42 shoes.\nSeen on 2026-03-04 with itching and swelling.";

export const documentDetail: DocumentDetail = {
  id: "X1",
  title: "A report about shoes",
  text: TEXT,
  extraction: {
    document_id: "X1",
    catalog_version: "test-1",
    model: "test/model",
    flags: [],
    cases: [
      {
        case_index: 0,
        label: "Patient A",
        fields: {
          shoe_size: { status: "extracted", value: 42, confidence: "high", provenance: [{ quote: "size 42", start: 14, end: 21 }] },
          mood: { status: "conflict", note: "calm and tense" },
          symptoms: {
            status: "extracted",
            value: ["itch", "swelling"],
            confidence: "medium",
            provenance: [{ quote: "itching and swelling", start: 53, end: 73 }],
          },
          seen_on: { status: "extracted", value: "2026-03-04", confidence: "low", provenance: [{ quote: "2026-03-04", start: 37, end: 47 }] },
          nickname: { status: "unknown" },
          pets: { status: "extracted", value: [], confidence: "high", provenance: [{ quote: "Patient", start: 0, end: 7 }] },
        },
      },
      { case_index: 1, label: "Patient B", fields: { shoe_size: { status: "unknown" } } },
    ],
  },
};

export const summaries: DocumentSummary[] = [
  { id: "X1", title: "A report about shoes", caseCount: 2, flags: ["multiple_patients"] },
  { id: "X2", title: "A complaint", caseCount: 0, flags: ["no_adverse_event"] },
];

export const emptyDocument: DocumentDetail = {
  id: "X2",
  title: "A complaint",
  text: "Nobody took it.",
  extraction: { document_id: "X2", catalog_version: "test-1", flags: ["no_adverse_event"], cases: [] },
};
