export type LabResultStatus = "PRELIMINARY" | "FINAL" | "CORRECTED" | "CANCELLED";

export type AbnormalFlag =
  | "LOW"
  | "HIGH"
  | "CRITICAL_LOW"
  | "CRITICAL_HIGH"
  | "ABNORMAL"
  | "POSITIVE"
  | "NEGATIVE"
  | "NORMAL";

export interface LabResultValue {
  id?: number;
  uuid: string;
  labTestUuid: string;
  labTestCode?: string | null;
  labTestName?: string | null;
  valueNumeric?: number | null;
  valueText?: string | null;
  valueCode?: string | null;
  unit?: string | null;
  referenceLow?: number | null;
  referenceHigh?: number | null;
  referenceText?: string | null;
  abnormalFlag?: AbnormalFlag | null;
  notes?: string | null;
  version?: number;
}

export interface LabResult {
  id?: number;
  uuid: string;
  labOrderUuid: string;
  orderNumber?: string | null;
  labOrderItemUuid: string;
  labTestCode?: string | null;
  labTestName?: string | null;
  specimenUuid: string;
  specimenType?: string | null;
  specimenStatus?: string | null;
  collectedAt?: string | null;
  receivedAt?: string | null;
  status: LabResultStatus;
  resultedAt?: string | null;
  verifiedAt?: string | null;
  verifiedBy?: number | null;
  comments?: string | null;
  correctionReason?: string | null;
  values: LabResultValue[];
  createdAt?: string;
  updatedAt?: string;
  version?: number;
}

export interface LabResultValuePayload {
  labTestUuid: string;
  valueNumeric?: number | null;
  valueText?: string | null;
  valueCode?: string | null;
  unit?: string | null;
  referenceLow?: number | null;
  referenceHigh?: number | null;
  referenceText?: string | null;
  abnormalFlag?: AbnormalFlag | null;
  notes?: string | null;
}

export interface CreateLabResultPayload {
  labOrderItemUuid: string;
  specimenUuid: string;
  comments?: string;
  values: LabResultValuePayload[];
}

export interface UpdateLabResultPayload {
  comments?: string;
  values: LabResultValuePayload[];
  version?: number;
}

export interface CorrectLabResultPayload {
  correctionReason: string;
  comments?: string;
  values: LabResultValuePayload[];
  version?: number;
}
