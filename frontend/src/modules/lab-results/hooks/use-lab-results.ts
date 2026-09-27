"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { labResultApi } from "../api/lab-result.api";
import {
  CorrectLabResultPayload,
  CreateLabResultPayload,
  LabResult,
  UpdateLabResultPayload,
} from "../types/lab-result.types";

export function useLabResults(labOrderUuid?: string) {
  return useQuery({
    queryKey: ["lab-order-results", labOrderUuid],
    enabled: Boolean(labOrderUuid),
    queryFn: async () => (await labResultApi.list(labOrderUuid!)).data as LabResult[],
  });
}

function useInvalidateResultScope() {
  const queryClient = useQueryClient();
  return () => {
    queryClient.invalidateQueries({ queryKey: ["lab-order-results"] });
    queryClient.invalidateQueries({ queryKey: ["encounter-lab-orders"] });
    queryClient.invalidateQueries({ queryKey: ["lab-order"] });
  };
}

export function useCreateLabResult(labOrderUuid?: string) {
  const invalidate = useInvalidateResultScope();
  return useMutation({
    mutationFn: async (payload: CreateLabResultPayload) =>
      (await labResultApi.create(labOrderUuid!, payload)).data as LabResult,
    onSuccess: invalidate,
  });
}

export function useUpdateLabResult(labOrderUuid?: string, resultUuid?: string) {
  const invalidate = useInvalidateResultScope();
  return useMutation({
    mutationFn: async (payload: UpdateLabResultPayload) =>
      (await labResultApi.update(labOrderUuid!, resultUuid!, payload)).data as LabResult,
    onSuccess: invalidate,
  });
}

export function useLabResultAction(labOrderUuid?: string) {
  const invalidate = useInvalidateResultScope();
  return useMutation({
    mutationFn: async (input: { resultUuid: string; action: "finalize" | "cancel" }) =>
      (await labResultApi[input.action](labOrderUuid!, input.resultUuid)).data as LabResult,
    onSuccess: invalidate,
  });
}

export function useCorrectLabResult(labOrderUuid?: string, resultUuid?: string) {
  const invalidate = useInvalidateResultScope();
  return useMutation({
    mutationFn: async (payload: CorrectLabResultPayload) =>
      (await labResultApi.correct(labOrderUuid!, resultUuid!, payload)).data as LabResult,
    onSuccess: invalidate,
  });
}
