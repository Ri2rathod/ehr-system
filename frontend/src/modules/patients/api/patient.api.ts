import { apiClient } from "@/services/api-client";
import { PatientListParams } from "../types/patient.types";

export const patientApi = {
  getAll: (params: PatientListParams) => apiClient.get("/patients", { params }),
  getById: (id: string) => apiClient.get(`/patients/${id}`),
  create: (payload: any) => apiClient.post("/patients", payload),
  checkDuplicates: (params: { firstName: string; lastName: string; dateOfBirth: string; phone?: string }) =>
    apiClient.get("/patients/duplicates", { params }),
  update: (id: string, payload: any) => apiClient.put(`/patients/${id}`, payload),
  delete: (id: string) => apiClient.delete(`/patients/${id}`),
};
