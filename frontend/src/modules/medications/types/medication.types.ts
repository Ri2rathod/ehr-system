export type DoseUnit =
  | "MG"
  | "MCG"
  | "G"
  | "ML"
  | "IU"
  | "MEQ"
  | "PERCENT"
  | "PUFF"
  | "DROP"
  | "UNIT"
  | "OTHER";

export type DosageForm =
  | "TABLET"
  | "CAPSULE"
  | "SYRUP"
  | "SOLUTION"
  | "CREAM"
  | "OINTMENT"
  | "INJECTION"
  | "DROPS"
  | "INHALER"
  | "OTHER";

export type Route =
  | "ORAL"
  | "TOPICAL"
  | "INTRAVENOUS"
  | "INTRAMUSCULAR"
  | "SUBCUTANEOUS"
  | "INHALATION"
  | "OPHTHALMIC"
  | "OTIC"
  | "NASAL"
  | "RECTAL"
  | "OTHER";

export interface Medication {
  id?: number;
  uuid: string;
  code?: string | null;
  codeSystem?: string | null;
  genericName: string;
  brandName?: string | null;
  strength?: number | null;
  strengthUnit?: DoseUnit | null;
  dosageForm?: DosageForm | null;
  route?: Route | null;
  isActive: boolean;
  createdAt?: string;
  updatedAt?: string;
  version?: number;
}

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
  first: boolean;
}
