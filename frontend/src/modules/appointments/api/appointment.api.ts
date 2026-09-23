import { apiClient } from "@/services/api-client";
import { AppointmentFilters, AppointmentPayload, AppointmentStatus } from "../types/appointment.types";

export const appointmentApi = {
  getAppointments: (params: AppointmentFilters) => apiClient.get("/appointments", { params }),
  getAppointment: (uuid: string) => apiClient.get(`/appointments/${uuid}`),
  createAppointment: (payload: AppointmentPayload) => apiClient.post("/appointments", payload),
  updateStatus: (uuid: string, status: AppointmentStatus, cancellationReason?: string) =>
    apiClient.put(`/appointments/${uuid}/status`, { status, cancellationReason }),
  rescheduleAppointment: (uuid: string, payload: { startTime: string; endTime: string; reason?: string }) =>
    apiClient.put(`/appointments/${uuid}/reschedule`, payload),
  getAvailableSlots: (doctorUuid: string, date: string) =>
    apiClient.get(`/doctors/${doctorUuid}/available-slots`, { params: { date } }),
  getRescheduleHistory: (uuid: string) => apiClient.get(`/appointments/${uuid}/history`),
};
