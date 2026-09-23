import { AppointmentStatus } from "../types/appointment.types";
import { statusLabel } from "../utils/appointment-format";

const styles: Record<AppointmentStatus, string> = {
  SCHEDULED: "bg-secondary-container text-on-secondary-container",
  CONFIRMED: "bg-primary-container text-on-primary-container",
  CHECKED_IN: "bg-tertiary-container text-on-tertiary-container",
  IN_PROGRESS: "bg-primary text-on-primary",
  COMPLETED: "bg-emerald-100 text-emerald-900",
  CANCELLED: "bg-error-container text-on-error-container",
  NO_SHOW: "bg-amber-100 text-amber-900",
  RESCHEDULED: "bg-surface-container-high text-on-surface-variant",
};

export function AppointmentStatusChip({ status }: { status: AppointmentStatus }) {
  return <span className={`inline-flex items-center gap-1 rounded-full px-2 py-1 text-[10px] font-bold tracking-wide ${styles[status]}`}><span aria-hidden="true">●</span>{statusLabel(status)}</span>;
}
