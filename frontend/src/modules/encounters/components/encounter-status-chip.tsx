import { EncounterStatus } from "../types/encounter.types";
import { encounterStatusLabel } from "../utils/encounter-format";

const styles: Record<EncounterStatus, string> = {
  DRAFT: "bg-surface-container-high text-on-surface-variant",
  IN_PROGRESS: "bg-primary text-on-primary",
  COMPLETED: "bg-emerald-100 text-emerald-900",
  CANCELLED: "bg-error-container text-on-error-container",
};

export function EncounterStatusChip({ status }: { status: EncounterStatus }) {
  return (
    <span
      className={`inline-flex items-center gap-1 rounded-full px-2 py-1 text-[10px] font-bold tracking-wide ${styles[status]}`}
    >
      <span aria-hidden="true">●</span>
      {encounterStatusLabel[status]}
    </span>
  );
}
