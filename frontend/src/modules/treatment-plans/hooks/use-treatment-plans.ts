"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { treatmentPlanApi } from "../api/treatment-plan.api";
import {
  CreateTreatmentItemPayload,
  CreateTreatmentPlanPayload,
  TreatmentPlan,
  TreatmentPlanItem,
  UpdateTreatmentItemPayload,
  UpdateTreatmentPlanPayload,
} from "../types/treatment-plan.types";

export type TreatmentPlanAction = "activate" | "complete" | "cancel";
export type TreatmentItemAction = "start" | "complete" | "cancel";

export function useTreatmentPlans(encounterUuid?: string) {
  return useQuery({
    queryKey: ["encounter-treatment-plans", encounterUuid],
    enabled: Boolean(encounterUuid),
    queryFn: async () =>
      (await treatmentPlanApi.listPlans(encounterUuid!)).data as TreatmentPlan[],
  });
}

export function useTreatmentPlanItems(encounterUuid?: string, planUuid?: string) {
  return useQuery({
    queryKey: ["treatment-plan-items", planUuid],
    enabled: Boolean(encounterUuid) && Boolean(planUuid),
    queryFn: async () =>
      (await treatmentPlanApi.listItems(encounterUuid!, planUuid!))
        .data as TreatmentPlanItem[],
  });
}

function useInvalidatePlans(encounterUuid?: string) {
  const queryClient = useQueryClient();
  return () =>
    queryClient.invalidateQueries({
      queryKey: ["encounter-treatment-plans", encounterUuid],
    });
}

function useInvalidateItems() {
  const queryClient = useQueryClient();
  return () => queryClient.invalidateQueries({ queryKey: ["treatment-plan-items"] });
}

export function useCreateTreatmentPlan(encounterUuid?: string) {
  const invalidate = useInvalidatePlans(encounterUuid);
  return useMutation({
    mutationFn: async (payload: CreateTreatmentPlanPayload) =>
      (await treatmentPlanApi.createPlan(encounterUuid!, payload))
        .data as TreatmentPlan,
    onSuccess: invalidate,
  });
}

export function useUpdateTreatmentPlan(encounterUuid?: string) {
  const invalidate = useInvalidatePlans(encounterUuid);
  return useMutation({
    mutationFn: async ({
      planUuid,
      payload,
    }: {
      planUuid: string;
      payload: UpdateTreatmentPlanPayload;
    }) =>
      (await treatmentPlanApi.updatePlan(encounterUuid!, planUuid, payload))
        .data as TreatmentPlan,
    onSuccess: invalidate,
  });
}

export function useTreatmentPlanAction(encounterUuid?: string) {
  const invalidate = useInvalidatePlans(encounterUuid);
  return useMutation({
    mutationFn: async ({
      planUuid,
      action,
    }: {
      planUuid: string;
      action: TreatmentPlanAction;
    }) => {
      if (action === "activate")
        return (await treatmentPlanApi.activatePlan(encounterUuid!, planUuid))
          .data as TreatmentPlan;
      if (action === "complete")
        return (await treatmentPlanApi.completePlan(encounterUuid!, planUuid))
          .data as TreatmentPlan;
      return (await treatmentPlanApi.cancelPlan(encounterUuid!, planUuid))
        .data as TreatmentPlan;
    },
    onSuccess: invalidate,
  });
}

export function useDeleteTreatmentPlan(encounterUuid?: string) {
  const invalidate = useInvalidatePlans(encounterUuid);
  return useMutation({
    mutationFn: async (planUuid: string) => {
      await treatmentPlanApi.deletePlan(encounterUuid!, planUuid);
    },
    onSuccess: invalidate,
  });
}

export function useCreateTreatmentItem(encounterUuid?: string) {
  const invalidate = useInvalidateItems();
  return useMutation({
    mutationFn: async ({
      planUuid,
      payload,
    }: {
      planUuid: string;
      payload: CreateTreatmentItemPayload;
    }) =>
      (
        await treatmentPlanApi.createItem(encounterUuid!, planUuid, payload)
      ).data as TreatmentPlanItem,
    onSuccess: invalidate,
  });
}

export function useUpdateTreatmentItem(encounterUuid?: string) {
  const invalidate = useInvalidateItems();
  return useMutation({
    mutationFn: async ({
      planUuid,
      itemUuid,
      payload,
    }: {
      planUuid: string;
      itemUuid: string;
      payload: UpdateTreatmentItemPayload;
    }) =>
      (
        await treatmentPlanApi.updateItem(encounterUuid!, planUuid, itemUuid, payload)
      ).data as TreatmentPlanItem,
    onSuccess: invalidate,
  });
}

export function useTreatmentItemAction(encounterUuid?: string) {
  const invalidate = useInvalidateItems();
  return useMutation({
    mutationFn: async ({
      planUuid,
      itemUuid,
      action,
    }: {
      planUuid: string;
      itemUuid: string;
      action: TreatmentItemAction;
    }) => {
      if (action === "start")
        return (
          await treatmentPlanApi.startItem(encounterUuid!, planUuid, itemUuid)
        ).data as TreatmentPlanItem;
      if (action === "complete")
        return (
          await treatmentPlanApi.completeItem(encounterUuid!, planUuid, itemUuid)
        ).data as TreatmentPlanItem;
      return (
        await treatmentPlanApi.cancelItem(encounterUuid!, planUuid, itemUuid)
      ).data as TreatmentPlanItem;
    },
    onSuccess: invalidate,
  });
}

export function useDeleteTreatmentItem(encounterUuid?: string) {
  const invalidate = useInvalidateItems();
  return useMutation({
    mutationFn: async ({ planUuid, itemUuid }: { planUuid: string; itemUuid: string }) => {
      await treatmentPlanApi.deleteItem(encounterUuid!, planUuid, itemUuid);
    },
    onSuccess: invalidate,
  });
}
