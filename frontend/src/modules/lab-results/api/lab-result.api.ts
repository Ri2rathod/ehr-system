import { apiClient } from "@/services/api-client";
import {
  CorrectLabResultPayload,
  CreateLabResultPayload,
  UpdateLabResultPayload,
} from "../types/lab-result.types";

export const labResultApi = {
  list: (labOrderUuid: string) => apiClient.get(`/lab-orders/${labOrderUuid}/results`),
  create: (labOrderUuid: string, payload: CreateLabResultPayload) =>
    apiClient.post(`/lab-orders/${labOrderUuid}/results`, payload),
  update: (labOrderUuid: string, resultUuid: string, payload: UpdateLabResultPayload) =>
    apiClient.put(`/lab-orders/${labOrderUuid}/results/${resultUuid}`, payload),
  finalize: (labOrderUuid: string, resultUuid: string) =>
    apiClient.post(`/lab-orders/${labOrderUuid}/results/${resultUuid}/finalize`, {}),
  correct: (labOrderUuid: string, resultUuid: string, payload: CorrectLabResultPayload) =>
    apiClient.post(`/lab-orders/${labOrderUuid}/results/${resultUuid}/correct`, payload),
  cancel: (labOrderUuid: string, resultUuid: string) =>
    apiClient.post(`/lab-orders/${labOrderUuid}/results/${resultUuid}/cancel`, {}),
};
