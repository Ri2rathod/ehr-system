import { DoseUnit, DosageForm, Route } from "@/modules/medications/types/medication.types";

export type { DoseUnit, DosageForm, Route };

export type PrescriptionStatus = "DRAFT" | "ACTIVE" | "COMPLETED" | "CANCELLED" | "VOID";

export type PrescriptionItemStatus = "ACTIVE" | "COMPLETED" | "CANCELLED" | "DISCONTINUED";

export type MedicationFrequency =
  | "ONCE_DAILY"
  | "TWICE_DAILY"
  | "THREE_TIMES_DAILY"
  | "FOUR_TIMES_DAILY"
  | "EVERY_MORNING"
  | "EVERY_EVENING"
  | "AT_BEDTIME"
  | "WEEKLY"
  | "AS_NEEDED"
  | "CUSTOM";

export type FrequencyUnit = "HOURS" | "DAYS";

export type QuantityUnit =
  | "TABLETS"
  | "CAPSULES"
  | "ML"
  | "G"
  | "PUFFS"
  | "DROPS"
  | "UNITS"
  | "OTHER";

export type DurationUnit = "DAYS" | "WEEKS" | "MONTHS";

export interface Prescription {
  id?: number;
  uuid: string;
  encounterUuid: string;
  patientUuid: string;
  doctorUuid?: string | null;
  prescriptionNumber: string;
  status: PrescriptionStatus;
  prescribedAt?: string | null;
  notes?: string | null;
  createdAt?: string;
  updatedAt?: string;
  version?: number;
}

export interface PrescriptionItem {
  id?: number;
  uuid: string;
  prescriptionUuid: string;
  medicationUuid: string;
  medicationCode?: string | null;
  medicationGenericName: string;
  medicationBrandName?: string | null;
  medicationStrength?: number | null;
  medicationStrengthUnit?: DoseUnit | null;
  medicationDosageForm?: DosageForm | null;
  medicationRoute?: Route | null;
  dose: number;
  doseUnit: DoseUnit;
  route: Route;
  frequency: MedicationFrequency;
  frequencyValue?: number | null;
  frequencyUnit?: FrequencyUnit | null;
  duration?: number | null;
  durationUnit?: DurationUnit | null;
  quantity?: number | null;
  quantityUnit?: QuantityUnit | null;
  refills?: number | null;
  instructions?: string | null;
  startDate?: string | null;
  endDate?: string | null;
  status: PrescriptionItemStatus;
  createdAt?: string;
  updatedAt?: string;
  version?: number;
}

export interface CreatePrescriptionPayload {
  notes?: string;
}

export interface UpdatePrescriptionPayload {
  notes?: string;
  version?: number;
}

export interface CreatePrescriptionItemPayload {
  medicationUuid: string;
  dose: number;
  doseUnit: DoseUnit;
  route: Route;
  frequency: MedicationFrequency;
  frequencyValue?: number;
  frequencyUnit?: FrequencyUnit;
  duration?: number;
  durationUnit?: DurationUnit;
  quantity?: number;
  quantityUnit?: QuantityUnit;
  refills?: number;
  instructions?: string;
  startDate?: string;
  endDate?: string;
}

export interface UpdatePrescriptionItemPayload {
  dose?: number;
  doseUnit?: DoseUnit;
  route?: Route;
  frequency?: MedicationFrequency;
  frequencyValue?: number;
  frequencyUnit?: FrequencyUnit;
  duration?: number;
  durationUnit?: DurationUnit;
  quantity?: number;
  quantityUnit?: QuantityUnit;
  refills?: number;
  instructions?: string;
  startDate?: string;
  endDate?: string;
  version?: number;
}
