import { apiClient } from "@/services/api-client";
import {
  CreateTreatmentItemPayload,
  CreateTreatmentPlanPayload,
  UpdateTreatmentItemPayload,
  UpdateTreatmentPlanPayload,
} from "../types/treatment-plan.types";

export const treatmentPlanApi = {
  listPlans: (encounterUuid: string) =>
    apiClient.get(`/encounters/${encounterUuid}/treatment-plans`),
  getPlan: (encounterUuid: string, planUuid: string) =>
    apiClient.get(`/encounters/${encounterUuid}/treatment-plans/${planUuid}`),
  createPlan: (encounterUuid: string, payload: CreateTreatmentPlanPayload) =>
    apiClient.post(`/encounters/${encounterUuid}/treatment-plans`, payload),
  updatePlan: (encounterUuid: string, planUuid: string, payload: UpdateTreatmentPlanPayload) =>
    apiClient.put(`/encounters/${encounterUuid}/treatment-plans/${planUuid}`, payload),
  activatePlan: (encounterUuid: string, planUuid: string) =>
    apiClient.post(`/encounters/${encounterUuid}/treatment-plans/${planUuid}/activate`),
  completePlan: (encounterUuid: string, planUuid: string) =>
    apiClient.post(`/encounters/${encounterUuid}/treatment-plans/${planUuid}/complete`),
  cancelPlan: (encounterUuid: string, planUuid: string) =>
    apiClient.post(`/encounters/${encounterUuid}/treatment-plans/${planUuid}/cancel`),
  deletePlan: (encounterUuid: string, planUuid: string) =>
    apiClient.delete(`/encounters/${encounterUuid}/treatment-plans/${planUuid}`),

  listItems: (encounterUuid: string, planUuid: string) =>
    apiClient.get(`/encounters/${encounterUuid}/treatment-plans/${planUuid}/items`),
  createItem: (
    encounterUuid: string,
    planUuid: string,
    payload: CreateTreatmentItemPayload,
  ) =>
    apiClient.post(
      `/encounters/${encounterUuid}/treatment-plans/${planUuid}/items`,
      payload,
    ),
  updateItem: (
    encounterUuid: string,
    planUuid: string,
    itemUuid: string,
    payload: UpdateTreatmentItemPayload,
  ) =>
    apiClient.put(
      `/encounters/${encounterUuid}/treatment-plans/${planUuid}/items/${itemUuid}`,
      payload,
    ),
  startItem: (encounterUuid: string, planUuid: string, itemUuid: string) =>
    apiClient.post(
      `/encounters/${encounterUuid}/treatment-plans/${planUuid}/items/${itemUuid}/start`,
    ),
  completeItem: (encounterUuid: string, planUuid: string, itemUuid: string) =>
    apiClient.post(
      `/encounters/${encounterUuid}/treatment-plans/${planUuid}/items/${itemUuid}/complete`,
    ),
  cancelItem: (encounterUuid: string, planUuid: string, itemUuid: string) =>
    apiClient.post(
      `/encounters/${encounterUuid}/treatment-plans/${planUuid}/items/${itemUuid}/cancel`,
    ),
  deleteItem: (encounterUuid: string, planUuid: string, itemUuid: string) =>
    apiClient.delete(
      `/encounters/${encounterUuid}/treatment-plans/${planUuid}/items/${itemUuid}`,
    ),
};
