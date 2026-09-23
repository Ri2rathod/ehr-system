"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { appointmentApi } from "../api/appointment.api";
import {
  Appointment,
  AppointmentFilters,
  AppointmentHistory,
  AppointmentListResult,
  AppointmentPayload,
  AppointmentStatus,
  AvailableSlots,
} from "../types/appointment.types";

function normalizeList(raw: unknown, fallback: Pick<AppointmentFilters, "page" | "size">): AppointmentListResult {
  const data = raw as { content?: Appointment[]; items?: Appointment[]; data?: Appointment[]; number?: number; page?: number; size?: number; totalPages?: number; totalElements?: number; total?: number };
  const items = Array.isArray(raw) ? raw as Appointment[] : data?.content || data?.items || data?.data || [];
  return { items, page: data?.number ?? data?.page ?? fallback.page, size: data?.size ?? fallback.size, totalPages: data?.totalPages ?? 1, totalElements: data?.totalElements ?? data?.total ?? items.length };
}

export function useAppointments(filters: AppointmentFilters) {
  return useQuery({ queryKey: ["appointments", filters], queryFn: async () => normalizeList((await appointmentApi.getAppointments(filters)).data, filters) });
}

export function useAppointment(uuid?: string) {
  return useQuery({ queryKey: ["appointment", uuid], enabled: Boolean(uuid), queryFn: async () => (await appointmentApi.getAppointment(uuid!)).data as Appointment });
}

export function useAvailableSlots(doctorUuid?: string, date?: string) {
  return useQuery({ queryKey: ["appointment-slots", doctorUuid, date], enabled: Boolean(doctorUuid && date), queryFn: async () => (await appointmentApi.getAvailableSlots(doctorUuid!, date!)).data as AvailableSlots });
}

export function useAppointmentHistory(uuid?: string) {
  return useQuery({ queryKey: ["appointment-history", uuid], enabled: Boolean(uuid), queryFn: async () => (await appointmentApi.getRescheduleHistory(uuid!)).data as AppointmentHistory[] });
}

function useInvalidateAppointments() {
  const queryClient = useQueryClient();
  return () => queryClient.invalidateQueries({ queryKey: ["appointments"] });
}

export function useCreateAppointment() {
  const invalidate = useInvalidateAppointments();
  return useMutation({ mutationFn: async (payload: AppointmentPayload) => (await appointmentApi.createAppointment(payload)).data as Appointment, onSuccess: invalidate });
}

export function useUpdateAppointmentStatus() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ uuid, status, cancellationReason }: { uuid: string; status: AppointmentStatus; cancellationReason?: string }) => (await appointmentApi.updateStatus(uuid, status, cancellationReason)).data as Appointment,
    onSuccess: (appointment) => { queryClient.setQueryData(["appointment", appointment.uuid], appointment); queryClient.invalidateQueries({ queryKey: ["appointments"] }); },
  });
}

export function useRescheduleAppointment() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ uuid, startTime, endTime, reason }: { uuid: string; startTime: string; endTime: string; reason?: string }) => (await appointmentApi.rescheduleAppointment(uuid, { startTime, endTime, reason })).data as Appointment,
    onSuccess: (appointment) => { queryClient.setQueryData(["appointment", appointment.uuid], appointment); queryClient.invalidateQueries({ queryKey: ["appointments"] }); queryClient.invalidateQueries({ queryKey: ["appointment-slots"] }); queryClient.invalidateQueries({ queryKey: ["appointment-history", appointment.uuid] }); },
  });
}
