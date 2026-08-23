import { apiClient } from "@/services/api-client";
import {
  CreateAvailabilityRequest,
  UpdateAvailabilityRequest,
  CreateExceptionRequest,
  UpdateExceptionRequest,
} from "../types/scheduling.types";

const base = (doctorUuid: string) => `/doctors/${doctorUuid}/availability`;

export const schedulingApi = {
  // ── Weekly availability ───────────────────────────────────────────────────
  getAvailability: (doctorUuid: string) =>
    apiClient.get(base(doctorUuid)),

  createAvailability: (doctorUuid: string, payload: CreateAvailabilityRequest) =>
    apiClient.post(base(doctorUuid), payload),

  createBulkAvailability: (doctorUuid: string, payload: CreateAvailabilityRequest[]) =>
    apiClient.post(`${base(doctorUuid)}/bulk`, payload),

  updateAvailability: (
    doctorUuid: string,
    availabilityUuid: string,
    payload: UpdateAvailabilityRequest
  ) => apiClient.put(`${base(doctorUuid)}/${availabilityUuid}`, payload),

  toggleAvailabilityStatus: (doctorUuid: string, availabilityUuid: string) =>
    apiClient.patch(`${base(doctorUuid)}/${availabilityUuid}/status`),

  deleteAvailability: (doctorUuid: string, availabilityUuid: string) =>
    apiClient.delete(`${base(doctorUuid)}/${availabilityUuid}`),

  // ── Exceptions ────────────────────────────────────────────────────────────
  getExceptions: (doctorUuid: string, from?: string, to?: string) =>
    apiClient.get(`${base(doctorUuid)}/exceptions`, {
      params: { ...(from ? { from } : {}), ...(to ? { to } : {}) },
    }),

  createException: (doctorUuid: string, payload: CreateExceptionRequest) =>
    apiClient.post(`${base(doctorUuid)}/exceptions`, payload),

  updateException: (
    doctorUuid: string,
    exceptionUuid: string,
    payload: UpdateExceptionRequest
  ) => apiClient.put(`${base(doctorUuid)}/exceptions/${exceptionUuid}`, payload),

  deleteException: (doctorUuid: string, exceptionUuid: string) =>
    apiClient.delete(`${base(doctorUuid)}/exceptions/${exceptionUuid}`),

  // ── Available appointment slots ───────────────────────────────────────────
  getAvailableSlots: (doctorUuid: string, date: string) =>
    apiClient.get(`/doctors/${doctorUuid}/available-slots`, {
      params: { date },
    }),
};
