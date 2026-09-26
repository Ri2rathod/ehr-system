import { visitTypeLabel } from "@/modules/appointments/utils/appointment-format";
import { VisitType } from "@/modules/appointments/types/appointment.types";
import { EncounterStatus } from "../types/encounter.types";

export const encounterStatusLabel: Record<EncounterStatus, string> = {
  DRAFT: "Draft",
  IN_PROGRESS: "In progress",
  COMPLETED: "Completed",
  CANCELLED: "Cancelled",
};

export const encounterTypeLabel: Record<VisitType, string> = {
  ...visitTypeLabel,
  WALK_IN: "Walk-in",
};

export function formatEncounterDateTime(value?: string) {
  if (!value) return "—";
  const date = new Date(value.includes("T") ? value : `${value}T00:00:00`);
  if (Number.isNaN(date.getTime())) return "—";
  return new Intl.DateTimeFormat("en-GB", {
    day: "2-digit",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  }).format(date);
}

export function ageFrom(dob?: string) {
  if (!dob) return null;
  const date = new Date(dob.slice(0, 10));
  if (Number.isNaN(date.getTime())) return null;
  const now = new Date();
  let age = now.getFullYear() - date.getFullYear();
  const m = now.getMonth() - date.getMonth();
  if (m < 0 || (m === 0 && now.getDate() < date.getDate())) age -= 1;
  return age;
}
