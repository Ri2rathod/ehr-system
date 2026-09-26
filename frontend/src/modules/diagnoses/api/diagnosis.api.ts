import { apiClient } from "@/services/api-client";
import { CreateDiagnosisPayload, UpdateDiagnosisPayload } from "../types/diagnosis.types";

export const diagnosisApi = {
  getDiagnoses: (encounterUuid: string) =>
    apiClient.get(`/encounters/${encounterUuid}/diagnoses`),
  getDiagnosis: (encounterUuid: string, diagnosisUuid: string) =>
    apiClient.get(`/encounters/${encounterUuid}/diagnoses/${diagnosisUuid}`),
  createDiagnosis: (encounterUuid: string, payload: CreateDiagnosisPayload) =>
    apiClient.post(`/encounters/${encounterUuid}/diagnoses`, payload),
  updateDiagnosis: (encounterUuid: string, diagnosisUuid: string, payload: UpdateDiagnosisPayload) =>
    apiClient.put(`/encounters/${encounterUuid}/diagnoses/${diagnosisUuid}`, payload),
  deactivateDiagnosis: (encounterUuid: string, diagnosisUuid: string) =>
    apiClient.delete(`/encounters/${encounterUuid}/diagnoses/${diagnosisUuid}`),
};
