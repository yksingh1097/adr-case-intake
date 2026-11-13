import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

import { catalog, documentDetail } from "../test/fixtures";
import { FieldsPane } from "./FieldsPane";

const item = documentDetail.extraction.cases[0]!;

function show(selected: string | null = null, onSelect: (id: string) => void = () => {}) {
  render(<FieldsPane catalog={catalog} item={item} selectedFieldId={selected} onSelect={onSelect} />);
}

describe("FieldsPane", () => {
  it("groups the fields by the catalog's sections, in catalog order", () => {
    show();

    const headings = screen.getAllByRole("heading", { level: 3 }).map((h) => h.textContent);
    expect(headings).toEqual(["Who", "What happened"]);
    const labels = screen.getAllByRole("button", { pressed: false }).map((b) => b.textContent ?? "");
    expect(labels.findIndex((l) => l.startsWith("Shoe size"))).toBeLessThan(labels.findIndex((l) => l.startsWith("Nickname")));
  });

  it("renders each catalog type with its own control", () => {
    show();

    expect(screen.getByLabelText("Shoe size")).toHaveAttribute("type", "number");
    expect(screen.getByLabelText("Seen on")).toHaveAttribute("type", "date");
    expect(screen.getByLabelText("Seen on")).toHaveValue("2026-03-04");
    const symptoms = screen.getByRole("group", { name: "Symptoms" });
    // a coded_multi field shows every option and marks the chosen ones
    expect(within(symptoms).getAllByRole("checkbox")).toHaveLength(3);
    expect(within(symptoms).getByRole("checkbox", { name: "Itching" })).toBeChecked();
    expect(within(symptoms).getByRole("checkbox", { name: "None" })).not.toBeChecked();
    expect(screen.getByText("None", { selector: "p" })).toBeInTheDocument();
  });

  it("gives unknown and conflict their own presentation instead of a control", () => {
    show();

    expect(screen.getByText("Not stated in the document")).toBeInTheDocument();
    expect(screen.getByText("The document states different values")).toBeInTheDocument();
    expect(screen.getByText("calm and tense")).toBeInTheDocument();
    expect(screen.queryByLabelText("Nickname")).not.toBeInTheDocument();
    expect(screen.queryByRole("radiogroup", { name: "Mood" })).not.toBeInTheDocument();
  });

  it("shows confidence by name next to the status", () => {
    show();

    expect(screen.getByText("Medium")).toBeInTheDocument();
    expect(screen.getByText("Low")).toBeInTheDocument();
  });

  it("summarises what needs attention and can show only those fields", async () => {
    show();

    expect(screen.getByText("1 in conflict")).toBeInTheDocument();
    expect(screen.getByText("2 less certain")).toBeInTheDocument();
    expect(screen.getByText("1 unknown")).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "Needs attention (4)" }));

    expect(screen.queryByLabelText("Shoe size")).not.toBeInTheDocument();
    expect(screen.getByText("Mood")).toBeInTheDocument();
    expect(screen.getByText("Nickname")).toBeInTheDocument();
  });

  it("reports the clicked field and shows its guidance only when selected", async () => {
    const chosen: string[] = [];
    show(null, (id) => chosen.push(id));

    await userEvent.click(screen.getByText("Symptoms"));

    expect(chosen).toEqual(["symptoms"]);
    expect(screen.queryByText("Pick every symptom named.")).not.toBeInTheDocument();
  });

  it("shows the guidance of the selected field", () => {
    show("symptoms");

    expect(screen.getByText("Pick every symptom named.")).toBeInTheDocument();
  });

  it("picks up a field added to the catalog without any code change", () => {
    const wider = { ...catalog, fields: [...catalog.fields, { id: "brand_new", label: "Brand new", section: "who", type: "string" as const }] };
    render(<FieldsPane catalog={wider} item={item} selectedFieldId={null} onSelect={() => {}} />);

    // the output file does not have it yet, so it reads as unknown rather than disappearing
    expect(screen.getByText("Brand new")).toBeInTheDocument();
    expect(screen.getByText("This field is not in the output file.")).toBeInTheDocument();
  });
});
