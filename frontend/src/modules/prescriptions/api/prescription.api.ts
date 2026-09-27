import { apiClient } from "@/services/api-client";
import {
  CreatePrescriptionItemPayload,
  CreatePrescriptionPayload,
  UpdatePrescriptionItemPayload,
  UpdatePrescriptionPayload,
} from "../types/prescription.types";

export const prescriptionApi = {
  list: (encounterUuid: string) =>
    apiClient.get(`/encounters/${encounterUuid}/prescriptions`),
  get: (encounterUuid: string, prescriptionUuid: string) =>
    apiClient.get(`/encounters/${encounterUuid}/prescriptions/${prescriptionUuid}`),
  create: (encounterUuid: string, payload: CreatePrescriptionPayload) =>
    apiClient.post(`/encounters/${encounterUuid}/prescriptions`, payload),
  update: (
    encounterUuid: string,
    prescriptionUuid: string,
    payload: UpdatePrescriptionPayload,
  ) =>
    apiClient.put(
      `/encounters/${encounterUuid}/prescriptions/${prescriptionUuid}`,
      payload,
    ),
  activate: (encounterUuid: string, prescriptionUuid: string) =>
    apiClient.post(
      `/encounters/${encounterUuid}/prescriptions/${prescriptionUuid}/activate`,
    ),
  complete: (encounterUuid: string, prescriptionUuid: string) =>
    apiClient.post(
      `/encounters/${encounterUuid}/prescriptions/${prescriptionUuid}/complete`,
    ),
  cancel: (encounterUuid: string, prescriptionUuid: string) =>
    apiClient.post(
      `/encounters/${encounterUuid}/prescriptions/${prescriptionUuid}/cancel`,
    ),
  void: (encounterUuid: string, prescriptionUuid: string) =>
    apiClient.post(
      `/encounters/${encounterUuid}/prescriptions/${prescriptionUuid}/void`,
    ),
  delete: (encounterUuid: string, prescriptionUuid: string) =>
    apiClient.delete(`/encounters/${encounterUuid}/prescriptions/${prescriptionUuid}`),

  listItems: (encounterUuid: string, prescriptionUuid: string) =>
    apiClient.get(
      `/encounters/${encounterUuid}/prescriptions/${prescriptionUuid}/items`,
    ),
  createItem: (
    encounterUuid: string,
    prescriptionUuid: string,
    payload: CreatePrescriptionItemPayload,
  ) =>
    apiClient.post(
      `/encounters/${encounterUuid}/prescriptions/${prescriptionUuid}/items`,
      payload,
    ),
  updateItem: (
    encounterUuid: string,
    prescriptionUuid: string,
    itemUuid: string,
    payload: UpdatePrescriptionItemPayload,
  ) =>
    apiClient.put(
      `/encounters/${encounterUuid}/prescriptions/${prescriptionUuid}/items/${itemUuid}`,
      payload,
    ),
  completeItem: (encounterUuid: string, prescriptionUuid: string, itemUuid: string) =>
    apiClient.post(
      `/encounters/${encounterUuid}/prescriptions/${prescriptionUuid}/items/${itemUuid}/complete`,
    ),
  cancelItem: (encounterUuid: string, prescriptionUuid: string, itemUuid: string) =>
    apiClient.post(
      `/encounters/${encounterUuid}/prescriptions/${prescriptionUuid}/items/${itemUuid}/cancel`,
    ),
  discontinueItem: (
    encounterUuid: string,
    prescriptionUuid: string,
    itemUuid: string,
  ) =>
    apiClient.post(
      `/encounters/${encounterUuid}/prescriptions/${prescriptionUuid}/items/${itemUuid}/discontinue`,
    ),
  deleteItem: (encounterUuid: string, prescriptionUuid: string, itemUuid: string) =>
    apiClient.delete(
      `/encounters/${encounterUuid}/prescriptions/${prescriptionUuid}/items/${itemUuid}`,
    ),
};
