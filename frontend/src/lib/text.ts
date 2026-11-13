/** "no_adverse_event" becomes "No adverse event". Works for any flag, status or confidence name. */
export function humanize(name: string): string {
  const spaced = name.replace(/[_-]+/g, " ").trim();
  return spaced.charAt(0).toUpperCase() + spaced.slice(1);
}

export const pluralCases = (count: number): string => (count === 0 ? "No cases" : count === 1 ? "1 case" : `${count} cases`);
