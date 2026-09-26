import { VisitType } from "@/modules/appointments/types/appointment.types";

export type EncounterStatus = "DRAFT" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED";

export type EncounterType = VisitType;

export interface Encounter {
  id?: number;
  uuid: string;
  encounterNumber: string;
  patientUuid: string;
  patientName?: string;
  patientMrn?: string;
  patientGender?: string;
  patientDateOfBirth?: string;
  doctorUuid: string;
  doctorName?: string;
  doctorCode?: string;
  appointmentUuid: string;
  appointmentNumber?: string;
  encounterType: EncounterType;
  status: EncounterStatus;
  startedAt?: string;
  endedAt?: string;
  chiefComplaint?: string;
  historyOfPresentIllness?: string;
  clinicalNotes?: string;
  assessment?: string;
  treatmentPlan?: string;
  followUpNotes?: string;
  cancellationReason?: string;
  createdAt?: string;
  updatedAt?: string;
  version?: number;
}

export interface EncounterFilters {
  patientUuid?: string;
  doctorUuid?: string;
  appointmentUuid?: string;
  status?: EncounterStatus;
  encounterType?: EncounterType;
  dateFrom?: string;
  dateTo?: string;
  page: number;
  size: number;
  sortBy?: string;
  sortDir?: "asc" | "desc";
}

export interface EncounterListResult {
  items: Encounter[];
  page: number;
  size: number;
  totalPages: number;
  totalElements: number;
}

export interface CreateEncounterPayload {
  appointmentUuid: string;
  encounterType?: EncounterType;
  chiefComplaint?: string;
  historyOfPresentIllness?: string;
}

export interface UpdateEncounterPayload {
  chiefComplaint?: string;
  historyOfPresentIllness?: string;
  clinicalNotes?: string;
  assessment?: string;
  treatmentPlan?: string;
  followUpNotes?: string;
  version?: number;
}

export interface EncounterVitals {
  id?: number;
  uuid: string;
  encounterUuid: string;
  temperature?: number;
  heartRate?: number;
  respiratoryRate?: number;
  systolicBp?: number;
  diastolicBp?: number;
  oxygenSaturation?: number;
  weight?: number;
  height?: number;
  bmi?: number;
  recordedAt?: string;
  recordedBy?: number;
  createdAt?: string;
  updatedAt?: string;
  version?: number;
}

export interface RecordVitalsPayload {
  temperature?: number;
  heartRate?: number;
  respiratoryRate?: number;
  systolicBp?: number;
  diastolicBp?: number;
  oxygenSaturation?: number;
  weight?: number;
  height?: number;
  recordedAt?: string;
}
