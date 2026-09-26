"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { diagnosisApi } from "../api/diagnosis.api";
import {
  CreateDiagnosisPayload,
  Diagnosis,
  UpdateDiagnosisPayload,
} from "../types/diagnosis.types";

export function useDiagnoses(encounterUuid?: string, enabled = true) {
  return useQuery({
    queryKey: ["encounter-diagnoses", encounterUuid],
    enabled: Boolean(encounterUuid) && enabled,
    queryFn: async () =>
      (await diagnosisApi.getDiagnoses(encounterUuid!)).data as Diagnosis[],
  });
}

function useInvalidateDiagnoses(encounterUuid?: string) {
  const queryClient = useQueryClient();
  return () =>
    queryClient.invalidateQueries({ queryKey: ["encounter-diagnoses", encounterUuid] });
}

export function useCreateDiagnosis(encounterUuid?: string) {
  const invalidate = useInvalidateDiagnoses(encounterUuid);
  return useMutation({
    mutationFn: async (payload: CreateDiagnosisPayload) =>
      (await diagnosisApi.createDiagnosis(encounterUuid!, payload)).data as Diagnosis,
    onSuccess: invalidate,
  });
}

export function useUpdateDiagnosis(encounterUuid?: string) {
  const invalidate = useInvalidateDiagnoses(encounterUuid);
  return useMutation({
    mutationFn: async ({
      diagnosisUuid,
      payload,
    }: {
      diagnosisUuid: string;
      payload: UpdateDiagnosisPayload;
    }) =>
      (await diagnosisApi.updateDiagnosis(encounterUuid!, diagnosisUuid, payload))
        .data as Diagnosis,
    onSuccess: invalidate,
  });
}

export function useDeactivateDiagnosis(encounterUuid?: string) {
  const invalidate = useInvalidateDiagnoses(encounterUuid);
  return useMutation({
    mutationFn: async (diagnosisUuid: string) => {
      await diagnosisApi.deactivateDiagnosis(encounterUuid!, diagnosisUuid);
    },
    onSuccess: invalidate,
  });
}
