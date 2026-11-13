import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, vi } from "vitest";

import App from "./App";
import { catalog, documentDetail, emptyDocument, summaries } from "./test/fixtures";

type Routes = Record<string, unknown>;

function serve(routes: Routes) {
  vi.stubGlobal(
    "fetch",
    vi.fn(async (path: string) => {
      if (!(path in routes)) {
        return new Response("", { status: 500 });
      }
      return new Response(JSON.stringify(routes[path]), { status: 200 });
    }),
  );
}

const normal: Routes = {
  "/api/catalog": catalog,
  "/api/documents": summaries,
  "/api/documents/X1": documentDetail,
  "/api/documents/X2": emptyDocument,
};

afterEach(() => vi.unstubAllGlobals());

describe("App", () => {
  it("shows a loading state first, then the first document", async () => {
    serve(normal);
    render(<App />);

    expect(screen.getAllByRole("status").length).toBeGreaterThan(0);
    expect(await screen.findByRole("heading", { name: "A report about shoes" })).toBeInTheDocument();
    expect(screen.getByRole("tab", { name: "Patient A" })).toHaveAttribute("aria-selected", "true");
  });

  it("highlights the quote in the document when a field is selected", async () => {
    serve(normal);
    render(<App />);
    await screen.findByRole("heading", { name: "A report about shoes" });
    expect(document.querySelector("mark")).toBeNull();

    await userEvent.click(screen.getByText("Shoe size"));

    expect(document.querySelector("mark")?.textContent).toBe("size 42");
  });

  it("switches between cases and clears the highlight", async () => {
    serve(normal);
    render(<App />);
    await screen.findByRole("heading", { name: "A report about shoes" });
    await userEvent.click(screen.getByText("Shoe size"));

    await userEvent.click(screen.getByRole("tab", { name: "Patient B" }));

    expect(document.querySelector("mark")).toBeNull();
    // patient B has one field in the output; the rest of the catalog reads as unknown
    expect(screen.getAllByText("Not stated in the document")).toHaveLength(catalog.fields.length);
  });

  it("shows an intentional empty state for a document with no cases", async () => {
    serve(normal);
    render(<App />);
    await screen.findByRole("heading", { name: "A report about shoes" });

    await userEvent.click(screen.getByRole("button", { name: /A complaint/ }));

    expect(await screen.findByRole("heading", { name: "No case in this document" })).toBeInTheDocument();
    expect(within(screen.getByLabelText("Document text")).getByText("No adverse event")).toBeInTheDocument();
  });

  it("explains a failure and retries", async () => {
    serve({ ...normal, "/api/catalog": undefined });
    vi.stubGlobal("fetch", vi.fn().mockRejectedValueOnce(new TypeError("network down")).mockImplementation(async (path: string) => new Response(JSON.stringify(normal[path]))));
    render(<App />);

    expect(await screen.findAllByRole("alert")).not.toHaveLength(0);
    expect(screen.getAllByText(/could not be reached/i).length).toBeGreaterThan(0);

    await userEvent.click(screen.getAllByRole("button", { name: "Try again" })[0]!);

    await waitFor(() => expect(screen.getByRole("heading", { name: "A report about shoes" })).toBeInTheDocument());
  });

  it("says so when there are no documents", async () => {
    serve({ ...normal, "/api/documents": [] });
    render(<App />);

    expect(await screen.findByRole("heading", { name: "No documents yet" })).toBeInTheDocument();
  });
});
