import { confidenceRank } from "../lib/risk";
import { humanize } from "../lib/text";

/** One bar per catalog level; the most confident level fills them all. Always paired with the level's name. */
export function ConfidenceMeter({ level, levels }: { level: string | undefined; levels: string[] }) {
  const rank = confidenceRank(level, levels);
  const filled = levels.length - rank;
  const tone = rank === 0 ? "high" : rank >= levels.length - 1 ? "low" : "mid";
  return (
    <span className={`confidence confidence--${tone}`} title="Confidence the model gave this value">
      <span className="confidence__bars" aria-hidden="true">
        {levels.map((name, index) => (
          <span key={name} className={index < filled ? "is-filled" : ""} />
        ))}
      </span>
      {level ? humanize(level) : "No confidence"}
    </span>
  );
}
