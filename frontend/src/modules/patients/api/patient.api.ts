import { apiClient } from "@/services/api-client";

export const patientApi = {
  getAll: (params?: any) => apiClient.get("/patients", { params }),
  getById: (id: string) => apiClient.get(`/patients/${id}`),
  create: (payload: any) => apiClient.post("/patients", payload),
  update: (id: string, payload: any) => apiClient.put(`/patients/${id}`, payload),
  delete: (id: string) => apiClient.delete(`/patients/${id}`),
};
