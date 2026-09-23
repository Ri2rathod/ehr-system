export type AppointmentStatus =
  | "SCHEDULED"
  | "CONFIRMED"
  | "CHECKED_IN"
  | "IN_PROGRESS"
  | "COMPLETED"
  | "CANCELLED"
  | "NO_SHOW"
  | "RESCHEDULED";

export type VisitType = "CONSULTATION" | "FOLLOW_UP" | "EMERGENCY" | "TELEMEDICINE" | "PROCEDURE" | "VACCINATION";

export interface Appointment {
  id?: number;
  uuid: string;
  appointmentNumber: string;
  patientUuid: string;
  patientName?: string;
  patientMrn?: string;
  doctorUuid: string;
  doctorName?: string;
  doctorCode?: string;
  appointmentDate: string;
  startTime: string;
  endTime: string;
  visitType: VisitType;
  status: AppointmentStatus;
  reasonForVisit?: string;
  notes?: string;
  cancellationReason?: string;
  checkedInAt?: string;
  inProgressAt?: string;
  completedAt?: string;
  cancelledAt?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface AppointmentFilters {
  doctorUuid?: string;
  patientUuid?: string;
  status?: AppointmentStatus;
  visitType?: VisitType;
  dateFrom?: string;
  dateTo?: string;
  page: number;
  size: number;
  sortBy?: string;
  sortDir?: "asc" | "desc";
}

export interface AppointmentListResult {
  items: Appointment[];
  page: number;
  size: number;
  totalPages: number;
  totalElements: number;
}

export interface AppointmentPayload {
  patientUuid: string;
  doctorUuid: string;
  startTime: string;
  endTime: string;
  visitType: VisitType;
  reasonForVisit?: string;
  notes?: string;
}

export interface AvailableSlot {
  startTime: string;
  endTime: string;
  available: boolean;
}

export interface AvailableSlots {
  doctorUuid: string;
  date: string;
  slotDurationMinutes: number;
  slots: AvailableSlot[];
}

export interface AppointmentHistory {
  uuid: string;
  appointmentNumber: string;
  action: string;
  previousStartTime?: string;
  previousEndTime?: string;
  previousStatus?: string;
  newStartTime?: string;
  newEndTime?: string;
  newStatus?: string;
  reason?: string;
  performedBy?: number;
  createdAt: string;
}
