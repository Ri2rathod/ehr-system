// ─── Enums ────────────────────────────────────────────────────────────────────

export type DayOfWeek =
  | "MONDAY"
  | "TUESDAY"
  | "WEDNESDAY"
  | "THURSDAY"
  | "FRIDAY"
  | "SATURDAY"
  | "SUNDAY";

export type ScheduleType = "WORK" | "BREAK";

export type ExceptionType =
  | "VACATION"
  | "HOLIDAY"
  | "EMERGENCY_LEAVE"
  | "PERSONAL_LEAVE"
  | "SICK_LEAVE"
  | "TRAINING"
  | "OTHER";

// ─── Availability ─────────────────────────────────────────────────────────────

export interface AvailabilityResponse {
  uuid: string;
  doctorUuid: string;
  dayOfWeek: DayOfWeek;
  scheduleType: ScheduleType;
  startTime: string; // HH:mm
  endTime: string;   // HH:mm
  isActive: boolean;
  effectiveFrom?: string; // YYYY-MM-DD
  effectiveUntil?: string; // YYYY-MM-DD
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateAvailabilityRequest {
  dayOfWeek: DayOfWeek;
  scheduleType: ScheduleType;
  startTime: string; // HH:mm
  endTime: string;   // HH:mm
  isActive?: boolean;
  effectiveFrom?: string;
  effectiveUntil?: string;
}

export type UpdateAvailabilityRequest = Partial<CreateAvailabilityRequest>;

// ─── Exceptions ───────────────────────────────────────────────────────────────

export interface AvailabilityExceptionResponse {
  uuid: string;
  doctorUuid: string;
  exceptionDate: string; // YYYY-MM-DD
  exceptionType: ExceptionType;
  isFullDay: boolean;
  startTime?: string; // HH:mm
  endTime?: string;   // HH:mm
  reason?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateExceptionRequest {
  exceptionDate: string;
  exceptionType: ExceptionType;
  isFullDay?: boolean;
  startTime?: string;
  endTime?: string;
  reason?: string;
}

export type UpdateExceptionRequest = Partial<CreateExceptionRequest>;

// ─── Available Slots ─────────────────────────────────────────────────────────

export interface AppointmentSlot {
  startTime: string; // HH:mm
  endTime: string;   // HH:mm
  available: boolean;
}

export interface AvailableSlotsResponse {
  doctorUuid: string;
  date: string; // YYYY-MM-DD
  slotDurationMinutes: number;
  slots: AppointmentSlot[];
}

// ─── Grouped (UI helper) ─────────────────────────────────────────────────────

export const DAY_ORDER: DayOfWeek[] = [
  "MONDAY",
  "TUESDAY",
  "WEDNESDAY",
  "THURSDAY",
  "FRIDAY",
  "SATURDAY",
  "SUNDAY",
];

export const DAY_LABELS: Record<DayOfWeek, string> = {
  MONDAY: "Monday",
  TUESDAY: "Tuesday",
  WEDNESDAY: "Wednesday",
  THURSDAY: "Thursday",
  FRIDAY: "Friday",
  SATURDAY: "Saturday",
  SUNDAY: "Sunday",
};
