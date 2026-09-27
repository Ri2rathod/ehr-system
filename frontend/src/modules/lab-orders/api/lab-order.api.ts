import { apiClient } from "@/services/api-client";
import {
  CreateLabOrderItemPayload,
  CreateLabOrderPayload,
  CreateSpecimenPayload,
  LabOrderAction,
  RejectSpecimenPayload,
  SpecimenAction,
  UpdateLabOrderItemPayload,
  UpdateLabOrderPayload,
} from "../types/lab-order.types";

export const labOrderApi = {
  list: (encounterUuid: string) =>
    apiClient.get(`/encounters/${encounterUuid}/lab-orders`),
  create: (encounterUuid: string, payload: CreateLabOrderPayload) =>
    apiClient.post(`/encounters/${encounterUuid}/lab-orders`, payload),
  get: (encounterUuid: string, labOrderUuid: string) =>
    apiClient.get(`/encounters/${encounterUuid}/lab-orders/${labOrderUuid}`),
  update: (encounterUuid: string, labOrderUuid: string, payload: UpdateLabOrderPayload) =>
    apiClient.put(`/encounters/${encounterUuid}/lab-orders/${labOrderUuid}`, payload),
  action: (
    encounterUuid: string,
    labOrderUuid: string,
    action: LabOrderAction,
  ) => apiClient.post(`/encounters/${encounterUuid}/lab-orders/${labOrderUuid}/${action}`, {}),
  delete: (encounterUuid: string, labOrderUuid: string) =>
    apiClient.delete(`/encounters/${encounterUuid}/lab-orders/${labOrderUuid}`),

  addItem: (
    encounterUuid: string,
    labOrderUuid: string,
    payload: CreateLabOrderItemPayload,
  ) => apiClient.post(`/encounters/${encounterUuid}/lab-orders/${labOrderUuid}/items`, payload),
  updateItem: (
    encounterUuid: string,
    labOrderUuid: string,
    itemUuid: string,
    payload: UpdateLabOrderItemPayload,
  ) =>
    apiClient.put(
      `/encounters/${encounterUuid}/lab-orders/${labOrderUuid}/items/${itemUuid}`,
      payload,
    ),
  deleteItem: (encounterUuid: string, labOrderUuid: string, itemUuid: string) =>
    apiClient.delete(
      `/encounters/${encounterUuid}/lab-orders/${labOrderUuid}/items/${itemUuid}`,
    ),

  listSpecimens: (labOrderUuid: string) =>
    apiClient.get(`/lab-orders/${labOrderUuid}/specimens`),
  createSpecimen: (labOrderUuid: string, payload: CreateSpecimenPayload) =>
    apiClient.post(`/lab-orders/${labOrderUuid}/specimens`, payload),
  specimenAction: (labOrderUuid: string, specimenUuid: string, action: SpecimenAction) =>
    apiClient.post(`/lab-orders/${labOrderUuid}/specimens/${specimenUuid}/${action}`, {}),
  rejectSpecimen: (
    labOrderUuid: string,
    specimenUuid: string,
    payload: RejectSpecimenPayload,
  ) =>
    apiClient.post(`/lab-orders/${labOrderUuid}/specimens/${specimenUuid}/reject`, payload),
};
