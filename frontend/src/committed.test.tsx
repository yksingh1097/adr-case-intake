import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, vi } from "vitest";

import type { Catalog, DocumentDetail, Extraction } from "./api/types";
import { Review } from "./components/Review";

// The real catalog, outputs and documents from the repository root.
const catalogFiles = import.meta.glob<Catalog>("../../catalog/field_catalog.json", { eager: true, import: "default" });
const outputs = import.meta.glob<Extraction>("../../out/D*.json", { eager: true, import: "default" });
const documents = import.meta.glob<string>("../../documents/D*.txt", { eager: true, query: "?raw", import: "default" });

const catalog = Object.values(catalogFiles)[0]!;

function detailFor(extraction: Extraction): DocumentDetail {
  const id = extraction.document_id;
  const text = documents[`../../documents/${id}.txt`] ?? null;
  return { id, title: id, text, extraction };
}

afterEach(() => vi.unstubAllGlobals());

describe("committed outputs", () => {
  it("has outputs to check", () => {
    expect(Object.keys(outputs).length).toBeGreaterThan(0);
  });

  it.each(Object.entries(outputs))("%s renders every catalog field and highlights each quote exactly", async (_path, extraction) => {
    const detail = detailFor(extraction);
    vi.stubGlobal("fetch", vi.fn(async () => new Response(JSON.stringify(detail))));
    render(<Review catalog={catalog} documentId={detail.id} />);
    await waitFor(() => expect(screen.getByLabelText("Document text")).toBeInTheDocument());

    for (const [index, item] of extraction.cases.entries()) {
      if (extraction.cases.length > 1) {
        await userEvent.click(screen.getByRole("tab", { name: item.label }));
      }
      expect(screen.getAllByRole("button", { pressed: false }).length).toBeGreaterThanOrEqual(catalog.fields.length);
      for (const field of catalog.fields) {
        const labels = screen.getAllByText(field.label, { selector: ".field__label" });
        expect(labels, `${detail.id} case ${index} field ${field.id}`).toHaveLength(1);
        const quotes = item.fields[field.id]?.provenance ?? [];
        if (quotes.length === 0) {
          continue;
        }
        await userEvent.click(labels[0]!);
        const marked = Array.from(document.querySelectorAll("mark")).map((m) => m.textContent);
        for (const quote of quotes) {
          // the document text under the mark is the quote, so the reviewer sees exactly what the model cited
          expect(marked.some((text) => text !== null && text.includes(quote.quote)), `${detail.id} ${field.id}: ${quote.quote}`).toBe(true);
        }
      }
    }
  });
});
