import type { Status } from "../api/types";
import { humanize } from "../lib/text";

export function StatusChip({ status, description }: { status: Status; description?: string }) {
  return (
    <span className={`chip chip--${status}`} title={description}>
      {humanize(status)}
    </span>
  );
}
