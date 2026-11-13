import { buildSegments } from "./segments";

describe("buildSegments", () => {
  it("returns one plain piece when nothing is highlighted", () => {
    expect(buildSegments("hello", [])).toEqual([{ text: "hello", marked: false }]);
  });

  it("marks the span and keeps the text around it", () => {
    expect(buildSegments("hello big world", [{ start: 6, end: 9 }])).toEqual([
      { text: "hello ", marked: false },
      { text: "big", marked: true },
      { text: " world", marked: false },
    ]);
  });

  it("merges overlapping and touching spans", () => {
    const segments = buildSegments("abcdefgh", [{ start: 4, end: 6 }, { start: 1, end: 3 }, { start: 2, end: 5 }]);

    expect(segments.filter((s) => s.marked)).toEqual([{ text: "bcdef", marked: true }]);
  });

  it("counts code points, not UTF-16 units", () => {
    // the emoji is one code point but two JavaScript characters
    const segments = buildSegments("\u{1F600} note: abc", [{ start: 8, end: 11 }]);

    expect(segments.find((s) => s.marked)?.text).toBe("abc");
  });

  it("ignores empty spans and clamps spans that run past the end", () => {
    const segments = buildSegments("abc", [{ start: 2, end: 2 }, { start: 1, end: 99 }]);

    expect(segments).toEqual([
      { text: "a", marked: false },
      { text: "bc", marked: true },
    ]);
  });
});
