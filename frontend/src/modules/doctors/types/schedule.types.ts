export type SlotType = "WORK" | "BREAK" | "ON_CALL" | "CONSULTATION" | "SURGERY";

export interface ScheduleSlot {
  id: string;
  startTime: string;
  endTime: string;
  type: SlotType;
  effectiveFrom?: string;
  effectiveTo?: string;
  notes?: string;
}

export interface DaySchedule {
  day: "Monday" | "Tuesday" | "Wednesday" | "Thursday" | "Friday" | "Saturday" | "Sunday";
  isActive: boolean;
  slots: ScheduleSlot[];
}

export interface ScheduleException {
  id: string;
  date: string;
  reason: string;
  type: "LEAVE" | "EMERGENCY" | "TRAINING" | "CONFERENCE";
  status: "APPROVED" | "PENDING" | "REJECTED";
}

export interface AvailableSlot {
  id: string;
  time: string;
  duration: number;
  isBooked: boolean;
  patientName?: string;
  type: string;
}
