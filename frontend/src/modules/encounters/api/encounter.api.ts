import { apiClient } from "@/services/api-client";
import {
  CreateEncounterPayload,
  EncounterFilters,
  RecordVitalsPayload,
  UpdateEncounterPayload,
} from "../types/encounter.types";

export const encounterApi = {
  getEncounters: (params: EncounterFilters) => apiClient.get("/encounters", { params }),
  getEncounter: (uuid: string) => apiClient.get(`/encounters/${uuid}`),
  getEncounterByNumber: (encounterNumber: string) =>
    apiClient.get(`/encounters/number/${encounterNumber}`),
  createEncounter: (payload: CreateEncounterPayload) => apiClient.post("/encounters", payload),
  updateEncounter: (uuid: string, payload: UpdateEncounterPayload) =>
    apiClient.put(`/encounters/${uuid}`, payload),
  startEncounter: (uuid: string) => apiClient.post(`/encounters/${uuid}/start`),
  completeEncounter: (uuid: string, payload?: UpdateEncounterPayload) =>
    apiClient.post(`/encounters/${uuid}/complete`, payload ?? {}),
  cancelEncounter: (uuid: string, reason: string) =>
    apiClient.post(`/encounters/${uuid}/cancel`, { reason }),
  getVitals: (uuid: string) => apiClient.get(`/encounters/${uuid}/vitals`),
  recordVitals: (uuid: string, payload: RecordVitalsPayload) =>
    apiClient.post(`/encounters/${uuid}/vitals`, payload),
  updateVitals: (uuid: string, vitalsUuid: string, payload: RecordVitalsPayload) =>
    apiClient.put(`/encounters/${uuid}/vitals/${vitalsUuid}`, payload),
};
