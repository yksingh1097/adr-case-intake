export interface Span {
  start: number;
  end: number;
}

export interface Segment {
  text: string;
  marked: boolean;
}

/**
 * Cuts the text into plain and marked pieces. Offsets count code points (as the checker does), while JavaScript
 * strings count UTF-16 units, so the text is split into code points first. Overlapping spans are merged.
 */
export function buildSegments(text: string, spans: Span[]): Segment[] {
  const chars = Array.from(text);
  const merged = mergeSpans(spans, chars.length);
  const segments: Segment[] = [];
  let position = 0;
  for (const span of merged) {
    if (span.start > position) {
      segments.push({ text: chars.slice(position, span.start).join(""), marked: false });
    }
    segments.push({ text: chars.slice(span.start, span.end).join(""), marked: true });
    position = span.end;
  }
  if (position < chars.length) {
    segments.push({ text: chars.slice(position).join(""), marked: false });
  }
  return segments;
}

function mergeSpans(spans: Span[], length: number): Span[] {
  const valid = spans
    .map((s) => ({ start: Math.max(0, s.start), end: Math.min(length, s.end) }))
    .filter((s) => s.start < s.end)
    .sort((a, b) => a.start - b.start);
  const merged: Span[] = [];
  for (const span of valid) {
    const last = merged[merged.length - 1];
    if (last && span.start <= last.end) {
      last.end = Math.max(last.end, span.end);
    } else {
      merged.push({ ...span });
    }
  }
  return merged;
}
