import { apiClient } from "@/services/api-client";
import {
  CreateDoctorRequest,
  DoctorListParams,
  UpdateDoctorRequest,
} from "../types/doctor.types";

export const doctorApi = {
  getAll: (params: DoctorListParams) => apiClient.get("/doctors", { params }),
  search: (params: { q: string; page?: number; size?: number }) =>
    apiClient.get("/doctors/search", { params }),
  getByUuid: (uuid: string) => apiClient.get(`/doctors/${uuid}`),
  create: (payload: CreateDoctorRequest) => apiClient.post("/doctors", payload),
  update: (uuid: string, payload: UpdateDoctorRequest) => apiClient.put(`/doctors/${uuid}`, payload),
  deactivate: (uuid: string) => apiClient.delete(`/doctors/${uuid}`),
};
