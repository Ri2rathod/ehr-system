export type DiagnosisType = "PRIMARY" | "SECONDARY";

export type DiagnosisStatus = "ACTIVE" | "RESOLVED" | "INACTIVE";

export interface Diagnosis {
  id?: number;
  uuid: string;
  encounterUuid: string;
  code?: string | null;
  codeSystem?: string | null;
  name: string;
  diagnosisType: DiagnosisType;
  clinicalStatus: DiagnosisStatus;
  onsetDate?: string | null;
  resolvedDate?: string | null;
  notes?: string | null;
  createdAt?: string;
  updatedAt?: string;
  version?: number;
}

export interface CreateDiagnosisPayload {
  name: string;
  code?: string;
  codeSystem?: string;
  diagnosisType?: DiagnosisType;
  clinicalStatus?: DiagnosisStatus;
  onsetDate?: string;
  notes?: string;
  replacePrimary?: boolean;
}

export interface UpdateDiagnosisPayload {
  name?: string;
  clinicalStatus?: DiagnosisStatus;
  onsetDate?: string;
  resolvedDate?: string;
  notes?: string;
  version?: number;
}
