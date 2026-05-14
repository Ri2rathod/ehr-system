export type PatientStatus = "ACTIVE" | "INACTIVE" | "ARCHIVED" | "BLOCKED" | "DECEASED";

export type Gender = "MALE" | "FEMALE" | "OTHER" | "UNKNOWN";

export type BloodGroup =
  | "A_POSITIVE"
  | "A_NEGATIVE"
  | "B_POSITIVE"
  | "B_NEGATIVE"
  | "AB_POSITIVE"
  | "AB_NEGATIVE"
  | "O_POSITIVE"
  | "O_NEGATIVE"
  | "UNKNOWN";

export interface Patient {
  id: string;
  patientUuid?: string;
  uuid?: string;
  mrn: string;
  firstName: string;
  lastName: string;
  email?: string;
  phone?: string;
  dateOfBirth?: string;
  gender: Gender;
  bloodGroup?: BloodGroup;
  status: PatientStatus;
  tenantName?: string;
  registeredAt?: string;
  isDeceased?: boolean;
  deceasedAt?: string;
}

export interface PatientListParams {
  q?: string;
  status?: string;
  gender?: string;
  bloodGroup?: string;
  tenant?: string;
  includeDeceased?: string;
  page: number;
  size: number;
  sortBy?: string;
  sortDir?: "asc" | "desc";
}

export interface PatientListResult {
  items: Patient[];
  page: number;
  size: number;
  totalPages: number;
  totalElements: number;
}
