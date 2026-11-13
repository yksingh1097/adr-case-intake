import type { FieldResult } from "../api/types";
import { catalog } from "../test/fixtures";
import { confidenceRank, needsAttention, riskOf, summarize } from "./risk";

const levels = ["high", "medium", "low"];
const extracted = (confidence?: string): FieldResult => ({ status: "extracted", value: "x", confidence });

describe("riskOf", () => {
  it("treats the first catalog level as fine and the last as the riskiest", () => {
    expect(riskOf(extracted("high"), levels)).toBe("ok");
    expect(riskOf(extracted("medium"), levels)).toBe("mid");
    expect(riskOf(extracted("low"), levels)).toBe("low");
  });

  it("flags conflict and unknown", () => {
    expect(riskOf({ status: "conflict" }, levels)).toBe("conflict");
    expect(riskOf({ status: "unknown" }, levels)).toBe("unknown");
  });

  it("does not trust a missing or unlisted confidence", () => {
    expect(riskOf(extracted(undefined), levels)).toBe("low");
    expect(riskOf(extracted("certain"), levels)).toBe("low");
  });

  it("follows the catalog when it lists other levels", () => {
    expect(riskOf(extracted("strong"), ["strong", "weak"])).toBe("ok");
    expect(riskOf(extracted("weak"), ["strong", "weak"])).toBe("low");
  });
});

describe("summarize", () => {
  it("counts fields by what a reviewer should do about them", () => {
    const results = {
      shoe_size: extracted("high"),
      mood: { status: "conflict" } as FieldResult,
      symptoms: extracted("medium"),
      seen_on: extracted("low"),
      nickname: { status: "unknown" } as FieldResult,
    };

    // pets is missing from the results, which counts as unknown
    expect(summarize(catalog.fields, results, levels)).toEqual({ conflict: 1, unknown: 2, uncertain: 2 });
    expect(needsAttention(results.shoe_size, levels)).toBe(false);
    expect(confidenceRank("low", levels)).toBe(2);
  });
});
