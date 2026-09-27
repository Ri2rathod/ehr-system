export type TreatmentPlanStatus = "DRAFT" | "ACTIVE" | "COMPLETED" | "CANCELLED";

export type TreatmentItemStatus = "PLANNED" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED";

export type TreatmentType =
  | "LIFESTYLE"
  | "DIET"
  | "EXERCISE"
  | "PHYSIOTHERAPY"
  | "BEHAVIORAL"
  | "EDUCATION"
  | "MONITORING"
  | "FOLLOW_UP"
  | "PROCEDURE"
  | "OTHER";

export type TreatmentPriority = "LOW" | "MEDIUM" | "HIGH";

export type DurationUnit = "DAYS" | "WEEKS" | "MONTHS";

export interface TreatmentPlan {
  id?: number;
  uuid: string;
  encounterUuid: string;
  title: string;
  status: TreatmentPlanStatus;
  goals?: string | null;
  instructions?: string | null;
  followUpInstructions?: string | null;
  notes?: string | null;
  startDate?: string | null;
  endDate?: string | null;
  createdAt?: string;
  updatedAt?: string;
  version?: number;
}

export interface TreatmentPlanItem {
  id?: number;
  uuid: string;
  treatmentPlanUuid: string;
  diagnosisUuid?: string | null;
  diagnosisCode?: string | null;
  diagnosisName?: string | null;
  treatmentType: TreatmentType;
  name: string;
  description?: string | null;
  instructions?: string | null;
  frequency?: string | null;
  duration?: number | null;
  durationUnit?: DurationUnit | null;
  priority: TreatmentPriority;
  status: TreatmentItemStatus;
  startDate?: string | null;
  endDate?: string | null;
  createdAt?: string;
  updatedAt?: string;
  version?: number;
}

export interface CreateTreatmentPlanPayload {
  title: string;
  goals?: string;
  instructions?: string;
  followUpInstructions?: string;
  notes?: string;
  startDate?: string;
  endDate?: string;
}

export interface UpdateTreatmentPlanPayload {
  title?: string;
  goals?: string;
  instructions?: string;
  followUpInstructions?: string;
  notes?: string;
  startDate?: string;
  endDate?: string;
  version?: number;
}

export interface CreateTreatmentItemPayload {
  diagnosisUuid?: string;
  treatmentType: TreatmentType;
  name: string;
  description?: string;
  instructions?: string;
  frequency?: string;
  duration?: number;
  durationUnit?: DurationUnit;
  priority?: TreatmentPriority;
  startDate?: string;
  endDate?: string;
}

export interface UpdateTreatmentItemPayload {
  diagnosisUuid?: string;
  unlinkDiagnosis?: boolean;
  treatmentType?: TreatmentType;
  name?: string;
  description?: string;
  instructions?: string;
  frequency?: string;
  duration?: number;
  durationUnit?: DurationUnit;
  priority?: TreatmentPriority;
  startDate?: string;
  endDate?: string;
  version?: number;
}
