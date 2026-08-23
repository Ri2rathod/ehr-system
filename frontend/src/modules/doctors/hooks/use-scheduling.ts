"use client";

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { schedulingApi } from "../api/doctor-scheduling.api";
import {
  CreateAvailabilityRequest,
  UpdateAvailabilityRequest,
  CreateExceptionRequest,
  UpdateExceptionRequest,
  AvailabilityResponse,
  AvailabilityExceptionResponse,
  AvailableSlotsResponse,
} from "../types/scheduling.types";

// ─── Query Keys ──────────────────────────────────────────────────────────────
export const schedulingKeys = {
  availability: (doctorUuid: string) => ["availability", doctorUuid] as const,
  exceptions: (doctorUuid: string, from?: string, to?: string) =>
    ["exceptions", doctorUuid, from, to] as const,
  slots: (doctorUuid: string, date: string) =>
    ["available-slots", doctorUuid, date] as const,
};

// ─── Weekly Availability ──────────────────────────────────────────────────────

export function useWeeklyAvailability(doctorUuid: string) {
  return useQuery({
    queryKey: schedulingKeys.availability(doctorUuid),
    queryFn: async () => {
      const res = await schedulingApi.getAvailability(doctorUuid);
      return res.data as AvailabilityResponse[];
    },
    enabled: !!doctorUuid,
  });
}

export function useCreateAvailability(doctorUuid: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateAvailabilityRequest) =>
      schedulingApi.createAvailability(doctorUuid, payload),
    onSuccess: () =>
      qc.invalidateQueries({ queryKey: schedulingKeys.availability(doctorUuid) }),
  });
}

export function useCreateBulkAvailability(doctorUuid: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateAvailabilityRequest[]) =>
      schedulingApi.createBulkAvailability(doctorUuid, payload),
    onSuccess: () =>
      qc.invalidateQueries({ queryKey: schedulingKeys.availability(doctorUuid) }),
  });
}

export function useUpdateAvailability(doctorUuid: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      availabilityUuid,
      payload,
    }: {
      availabilityUuid: string;
      payload: UpdateAvailabilityRequest;
    }) => schedulingApi.updateAvailability(doctorUuid, availabilityUuid, payload),
    onSuccess: () =>
      qc.invalidateQueries({ queryKey: schedulingKeys.availability(doctorUuid) }),
  });
}

export function useToggleAvailabilityStatus(doctorUuid: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (availabilityUuid: string) =>
      schedulingApi.toggleAvailabilityStatus(doctorUuid, availabilityUuid),
    onSuccess: () =>
      qc.invalidateQueries({ queryKey: schedulingKeys.availability(doctorUuid) }),
  });
}

export function useDeleteAvailability(doctorUuid: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (availabilityUuid: string) =>
      schedulingApi.deleteAvailability(doctorUuid, availabilityUuid),
    onSuccess: () =>
      qc.invalidateQueries({ queryKey: schedulingKeys.availability(doctorUuid) }),
  });
}

// ─── Exceptions ───────────────────────────────────────────────────────────────

export function useExceptions(doctorUuid: string, from?: string, to?: string) {
  return useQuery({
    queryKey: schedulingKeys.exceptions(doctorUuid, from, to),
    queryFn: async () => {
      const res = await schedulingApi.getExceptions(doctorUuid, from, to);
      return res.data as AvailabilityExceptionResponse[];
    },
    enabled: !!doctorUuid,
  });
}

export function useCreateException(doctorUuid: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateExceptionRequest) =>
      schedulingApi.createException(doctorUuid, payload),
    onSuccess: () =>
      qc.invalidateQueries({ queryKey: ["exceptions", doctorUuid] }),
  });
}

export function useUpdateException(doctorUuid: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      exceptionUuid,
      payload,
    }: {
      exceptionUuid: string;
      payload: UpdateExceptionRequest;
    }) => schedulingApi.updateException(doctorUuid, exceptionUuid, payload),
    onSuccess: () =>
      qc.invalidateQueries({ queryKey: ["exceptions", doctorUuid] }),
  });
}

export function useDeleteException(doctorUuid: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (exceptionUuid: string) =>
      schedulingApi.deleteException(doctorUuid, exceptionUuid),
    onSuccess: () =>
      qc.invalidateQueries({ queryKey: ["exceptions", doctorUuid] }),
  });
}

// ─── Available Slots ──────────────────────────────────────────────────────────

export function useAvailableSlots(doctorUuid: string, date: string) {
  return useQuery({
    queryKey: schedulingKeys.slots(doctorUuid, date),
    queryFn: async () => {
      const res = await schedulingApi.getAvailableSlots(doctorUuid, date);
      return res.data as AvailableSlotsResponse;
    },
    enabled: !!doctorUuid && !!date,
  });
}
