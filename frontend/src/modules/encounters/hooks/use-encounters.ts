"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { encounterApi } from "../api/encounter.api";
import {
  CreateEncounterPayload,
  Encounter,
  EncounterFilters,
  EncounterListResult,
  EncounterVitals,
  RecordVitalsPayload,
  UpdateEncounterPayload,
} from "../types/encounter.types";

function normalizeList(
  raw: unknown,
  fallback: Pick<EncounterFilters, "page" | "size">,
): EncounterListResult {
  const data = raw as {
    content?: Encounter[];
    items?: Encounter[];
    data?: Encounter[];
    number?: number;
    page?: number;
    size?: number;
    totalPages?: number;
    totalElements?: number;
    total?: number;
  };
  const items = Array.isArray(raw) ? (raw as Encounter[]) : data?.content || data?.items || data?.data || [];
  return {
    items,
    page: data?.number ?? data?.page ?? fallback.page,
    size: data?.size ?? fallback.size,
    totalPages: data?.totalPages ?? 1,
    totalElements: data?.totalElements ?? data?.total ?? items.length,
  };
}

export function useEncounters(filters: EncounterFilters) {
  return useQuery({
    queryKey: ["encounters", filters],
    queryFn: async () => normalizeList((await encounterApi.getEncounters(filters)).data, filters),
  });
}

export function useEncounter(uuid?: string) {
  return useQuery({
    queryKey: ["encounter", uuid],
    enabled: Boolean(uuid),
    queryFn: async () => (await encounterApi.getEncounter(uuid!)).data as Encounter,
  });
}

export function useEncounterByAppointment(appointmentUuid?: string, enabled = true) {
  return useQuery({
    queryKey: ["encounters", "appointment", appointmentUuid],
    enabled: Boolean(appointmentUuid) && enabled,
    queryFn: async () => {
      const list = normalizeList(
        (await encounterApi.getEncounters({ appointmentUuid: appointmentUuid!, page: 0, size: 1 })).data,
        { page: 0, size: 1 },
      );
      return list.items[0];
    },
  });
}

export function useEncounterVitals(encounterUuid?: string) {
  return useQuery({
    queryKey: ["encounter-vitals", encounterUuid],
    enabled: Boolean(encounterUuid),
    queryFn: async () => (await encounterApi.getVitals(encounterUuid!)).data as EncounterVitals[],
  });
}

function cacheEncounter(
  queryClient: ReturnType<typeof useQueryClient>,
  encounter: Encounter,
) {
  queryClient.setQueryData(["encounter", encounter.uuid], encounter);
  queryClient.invalidateQueries({ queryKey: ["encounters"] });
  queryClient.invalidateQueries({ queryKey: ["appointments"] });
}

export function useCreateEncounter() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: CreateEncounterPayload) =>
      (await encounterApi.createEncounter(payload)).data as Encounter,
    onSuccess: (encounter) => cacheEncounter(queryClient, encounter),
  });
}

export function useStartVisit() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: CreateEncounterPayload) => {
      try {
        return (await encounterApi.createEncounter(payload)).data as Encounter;
      } catch (caught: unknown) {
        const response = (caught as { response?: { status?: number; data?: { message?: string } } })?.response;
        const message = String(response?.data?.message || "");
        const status = response?.status;
        if (message.toLowerCase().includes("already exists") && (status === 400 || status === 409)) {
          const list = normalizeList(
            (await encounterApi.getEncounters({ appointmentUuid: payload.appointmentUuid, page: 0, size: 1 })).data,
            { page: 0, size: 1 },
          );
          if (list.items[0]) return list.items[0];
        }
        throw caught;
      }
    },
    onSuccess: (encounter) => cacheEncounter(queryClient, encounter),
  });
}

export function useUpdateEncounter() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ uuid, payload }: { uuid: string; payload: UpdateEncounterPayload }) =>
      (await encounterApi.updateEncounter(uuid, payload)).data as Encounter,
    onSuccess: (encounter) => cacheEncounter(queryClient, encounter),
  });
}

export function useCompleteEncounter() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ uuid, payload }: { uuid: string; payload?: UpdateEncounterPayload }) =>
      (await encounterApi.completeEncounter(uuid, payload)).data as Encounter,
    onSuccess: (encounter) => cacheEncounter(queryClient, encounter),
  });
}

export function useCancelEncounter() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ uuid, reason }: { uuid: string; reason: string }) =>
      (await encounterApi.cancelEncounter(uuid, reason)).data as Encounter,
    onSuccess: (encounter) => cacheEncounter(queryClient, encounter),
  });
}

export function useRecordVitals() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ uuid, payload }: { uuid: string; payload: RecordVitalsPayload }) =>
      (await encounterApi.recordVitals(uuid, payload)).data as EncounterVitals,
    onSuccess: (_vitals, { uuid }) => {
      queryClient.invalidateQueries({ queryKey: ["encounter-vitals", uuid] });
    },
  });
}
