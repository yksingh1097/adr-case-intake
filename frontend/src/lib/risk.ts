import type { FieldDefinition, FieldResult } from "../api/types";

/** How closely a reviewer should look at a field. Drives the colour of the rail beside it. */
export type Risk = "conflict" | "unknown" | "low" | "mid" | "ok";

export interface RiskSummary {
  conflict: number;
  unknown: number;
  uncertain: number;
}

/** Position of a level in the catalog's list; 0 is the most confident. A level the catalog does not list counts as the least confident. */
export function confidenceRank(level: string | undefined, levels: string[]): number {
  const index = level === undefined ? -1 : levels.indexOf(level);
  return index === -1 ? levels.length - 1 : index;
}

export function riskOf(result: FieldResult, levels: string[]): Risk {
  if (result.status === "conflict") {
    return "conflict";
  }
  if (result.status === "unknown") {
    return "unknown";
  }
  const rank = confidenceRank(result.confidence, levels);
  if (rank === 0) {
    return "ok";
  }
  return rank >= levels.length - 1 ? "low" : "mid";
}

export const needsAttention = (result: FieldResult, levels: string[]): boolean => riskOf(result, levels) !== "ok";

export function summarize(fields: FieldDefinition[], results: Record<string, FieldResult>, levels: string[]): RiskSummary {
  const summary: RiskSummary = { conflict: 0, unknown: 0, uncertain: 0 };
  for (const field of fields) {
    const result = results[field.id];
    if (!result) {
      summary.unknown += 1;
      continue;
    }
    const risk = riskOf(result, levels);
    if (risk === "conflict") {
      summary.conflict += 1;
    } else if (risk === "unknown") {
      summary.unknown += 1;
    } else if (risk !== "ok") {
      summary.uncertain += 1;
    }
  }
  return summary;
}
