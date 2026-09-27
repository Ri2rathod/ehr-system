export type LabOrderPriority = "ROUTINE" | "URGENT" | "STAT";

export type LabOrderStatus =
  | "DRAFT"
  | "ORDERED"
  | "IN_PROGRESS"
  | "COMPLETED"
  | "CANCELLED";

export type LabOrderItemStatus =
  | "ORDERED"
  | "IN_PROGRESS"
  | "COMPLETED"
  | "CANCELLED";

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

export type SpecimenStatus =
  | "PENDING_COLLECTION"
  | "COLLECTED"
  | "RECEIVED"
  | "REJECTED"
  | "CANCELLED";

export interface LabOrderItem {
  id?: number;
  uuid: string;
  labOrderUuid: string;
  labTestUuid: string;
  labTestCode: string;
  labTestName: string;
  instructions?: string | null;
  status: LabOrderItemStatus;
  createdAt?: string;
  updatedAt?: string;
  version?: number;
}

export interface LabOrderSummary {
  id?: number;
  uuid: string;
  encounterUuid: string;
  orderNumber: string;
  priority: LabOrderPriority;
  status: LabOrderStatus;
  instructions?: string | null;
  orderedAt?: string | null;
  itemCount: number;
  specimenCount: number;
  specimenStatus?: string | null;
  resultCount: number;
  finalizedResultCount: number;
  createdAt?: string;
  updatedAt?: string;
  version?: number;
}

export interface LabOrder extends LabOrderSummary {
  items: LabOrderItem[];
}

export interface Specimen {
  id?: number;
  uuid: string;
  labOrderUuid: string;
  orderNumber: string;
  specimenType: SpecimenType;
  specimenIdentifier?: string | null;
  status: SpecimenStatus;
  collectedAt?: string | null;
  collectedBy?: number | null;
  receivedAt?: string | null;
  receivedBy?: number | null;
  rejectionReason?: string | null;
  notes?: string | null;
  createdAt?: string;
  updatedAt?: string;
  version?: number;
}

export interface CreateLabOrderPayload {
  priority?: LabOrderPriority;
  instructions?: string;
  items?: Array<{ labTestUuid: string; instructions?: string }>;
}

export interface UpdateLabOrderPayload {
  priority?: LabOrderPriority;
  instructions?: string;
  version?: number;
}

export interface CreateLabOrderItemPayload {
  labTestUuid: string;
  instructions?: string;
}

export interface UpdateLabOrderItemPayload {
  labTestUuid?: string;
  instructions?: string;
  version?: number;
}

export interface CreateSpecimenPayload {
  specimenType: SpecimenType;
  specimenIdentifier?: string;
  notes?: string;
}

export interface RejectSpecimenPayload {
  rejectionReason: string;
  notes?: string;
}

export type LabOrderAction = "order" | "start" | "complete" | "cancel";
export type SpecimenAction = "collect" | "receive" | "cancel";
