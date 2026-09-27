export type ResultType = "NUMERIC" | "TEXT" | "QUALITATIVE" | "CODED";

export type SpecimenType =
  | "BLOOD"
  | "SERUM"
  | "PLASMA"
  | "URINE"
  | "STOOL"
  | "SWAB"
  | "SALIVA"
  | "TISSUE"
  | "OTHER";

export interface LabTest {
  id?: number;
  uuid: string;
  code: string;
  codeSystem?: string | null;
  name: string;
  shortName?: string | null;
  description?: string | null;
  category?: string | null;
  specimenType?: SpecimenType | null;
  resultType: ResultType;
  unit?: string | null;
  defaultReferenceLow?: number | null;
  defaultReferenceHigh?: number | null;
  defaultReferenceText?: string | null;
  isActive: boolean;
  createdAt?: string;
  updatedAt?: string;
  version?: number;
}

export interface LabTestSummary {
  uuid: string;
  code: string;
  name: string;
  shortName?: string | null;
  category?: string | null;
  specimenType?: SpecimenType | null;
  resultType: ResultType;
  unit?: string | null;
  defaultReferenceLow?: number | null;
  defaultReferenceHigh?: number | null;
  defaultReferenceText?: string | null;
  isActive: boolean;
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

export type LabTestSearchPage = PagedResponse<LabTestSummary>;
