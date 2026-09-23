import { AppointmentStatus, VisitType } from "../types/appointment.types";

export const visitTypeLabel: Record<VisitType, string> = { CONSULTATION: "Consultation", FOLLOW_UP: "Follow-up", EMERGENCY: "Emergency", TELEMEDICINE: "Telemedicine", PROCEDURE: "Procedure", VACCINATION: "Vaccination" };

export function formatAppointmentDate(value?: string) {
  if (!value) return "—";
  const date = new Date(`${value.slice(0, 10)}T00:00:00`);
  return Number.isNaN(date.getTime()) ? "—" : new Intl.DateTimeFormat("en-GB", { day: "2-digit", month: "short", year: "numeric" }).format(date);
}

export function formatAppointmentTime(value?: string) {
  if (!value) return "—";
  const time = value.includes("T") ? value.slice(11, 16) : value.slice(0, 5);
  const [hours, minutes] = time.split(":").map(Number);
  if (Number.isNaN(hours) || Number.isNaN(minutes)) return "—";
  return new Intl.DateTimeFormat("en-GB", { hour: "2-digit", minute: "2-digit", hour12: false }).format(new Date(2000, 0, 1, hours, minutes));
}

export function formatAppointmentTime12(value?: string) {
  if (!value) return "—";
  const time = value.includes("T") ? value.slice(11, 16) : value.slice(0, 5);
  const [hours, minutes] = time.split(":").map(Number);
  if (Number.isNaN(hours) || Number.isNaN(minutes)) return "—";
  const period = hours >= 12 ? "PM" : "AM";
  const hour12 = hours % 12 || 12;
  return `${String(hour12).padStart(2, "0")}:${String(minutes).padStart(2, "0")} ${period}`;
}

export function formatLongDate(value?: string) {
  if (!value) return "—";
  const date = new Date(`${value.slice(0, 10)}T00:00:00`);
  if (Number.isNaN(date.getTime())) return "—";
  return new Intl.DateTimeFormat("en-US", {
    weekday: "long",
    month: "long",
    day: "numeric",
    year: "numeric",
  }).format(date);
}

export function formatCompactDate(value?: string) {
  if (!value) return "Select date";
  const date = new Date(`${value.slice(0, 10)}T00:00:00`);
  if (Number.isNaN(date.getTime())) return "Select date";
  return new Intl.DateTimeFormat("en-US", {
    weekday: "long",
    month: "short",
    day: "numeric",
  }).format(date);
}

export function statusLabel(status: AppointmentStatus) { return status.replaceAll("_", " "); }
export function toLocalDate(value: Date) { return `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, "0")}-${String(value.getDate()).padStart(2, "0")}`; }

export function shiftLocalDate(value: string | undefined, days: number) {
  const base = value ? new Date(`${value.slice(0, 10)}T00:00:00`) : new Date();
  if (Number.isNaN(base.getTime())) return toLocalDate(new Date());
  base.setDate(base.getDate() + days);
  return toLocalDate(base);
}
