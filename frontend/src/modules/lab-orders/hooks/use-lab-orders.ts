"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { labOrderApi } from "../api/lab-order.api";
import {
  CreateLabOrderItemPayload,
  CreateLabOrderPayload,
  CreateSpecimenPayload,
  LabOrder,
  LabOrderAction,
  LabOrderSummary,
  RejectSpecimenPayload,
  Specimen,
  SpecimenAction,
  UpdateLabOrderItemPayload,
  UpdateLabOrderPayload,
} from "../types/lab-order.types";

export function useLabOrders(encounterUuid?: string) {
  return useQuery({
    queryKey: ["encounter-lab-orders", encounterUuid],
    enabled: Boolean(encounterUuid),
    queryFn: async () =>
      (await labOrderApi.list(encounterUuid!)).data as LabOrderSummary[],
  });
}

export function useLabOrder(encounterUuid?: string, labOrderUuid?: string) {
  return useQuery({
    queryKey: ["lab-order", encounterUuid, labOrderUuid],
    enabled: Boolean(encounterUuid && labOrderUuid),
    queryFn: async () =>
      (await labOrderApi.get(encounterUuid!, labOrderUuid!)).data as LabOrder,
  });
}

export function useSpecimens(labOrderUuid?: string) {
  return useQuery({
    queryKey: ["lab-order-specimens", labOrderUuid],
    enabled: Boolean(labOrderUuid),
    queryFn: async () =>
      (await labOrderApi.listSpecimens(labOrderUuid!)).data as Specimen[],
  });
}

function useInvalidateOrderScope() {
  const queryClient = useQueryClient();
  return () => {
    queryClient.invalidateQueries({ queryKey: ["encounter-lab-orders"] });
    queryClient.invalidateQueries({ queryKey: ["lab-order"] });
  };
}

function useInvalidateSpecimenScope() {
  const invalidateOrder = useInvalidateOrderScope();
  const queryClient = useQueryClient();
  return () => {
    queryClient.invalidateQueries({ queryKey: ["lab-order-specimens"] });
    invalidateOrder();
  };
}

export function useCreateLabOrder(encounterUuid?: string) {
  const invalidate = useInvalidateOrderScope();
  return useMutation({
    mutationFn: async (payload: CreateLabOrderPayload) =>
      (await labOrderApi.create(encounterUuid!, payload)).data as LabOrder,
    onSuccess: invalidate,
  });
}

export function useUpdateLabOrder(encounterUuid?: string, labOrderUuid?: string) {
  const invalidate = useInvalidateOrderScope();
  return useMutation({
    mutationFn: async (payload: UpdateLabOrderPayload) =>
      (await labOrderApi.update(encounterUuid!, labOrderUuid!, payload)).data as LabOrder,
    onSuccess: invalidate,
  });
}

export function useLabOrderAction(encounterUuid?: string, labOrderUuid?: string) {
  const invalidate = useInvalidateOrderScope();
  return useMutation({
    mutationFn: async (action: LabOrderAction) =>
      (await labOrderApi.action(encounterUuid!, labOrderUuid!, action)).data as LabOrder,
    onSuccess: invalidate,
  });
}

export function useDeleteLabOrder(encounterUuid?: string, labOrderUuid?: string) {
  const invalidate = useInvalidateOrderScope();
  return useMutation({
    mutationFn: async () => {
      await labOrderApi.delete(encounterUuid!, labOrderUuid!);
    },
    onSuccess: invalidate,
  });
}

export function useCreateLabOrderItem(encounterUuid?: string, labOrderUuid?: string) {
  const invalidate = useInvalidateOrderScope();
  return useMutation({
    mutationFn: async (payload: CreateLabOrderItemPayload) =>
      (await labOrderApi.addItem(encounterUuid!, labOrderUuid!, payload)).data as LabOrder,
    onSuccess: invalidate,
  });
}

export function useUpdateLabOrderItem(
  encounterUuid?: string,
  labOrderUuid?: string,
) {
  const invalidate = useInvalidateOrderScope();
  return useMutation({
    mutationFn: async (input: { itemUuid: string; payload: UpdateLabOrderItemPayload }) =>
      (
        await labOrderApi.updateItem(
          encounterUuid!,
          labOrderUuid!,
          input.itemUuid,
          input.payload,
        )
      ).data as LabOrder,
    onSuccess: invalidate,
  });
}

export function useDeleteLabOrderItem(
  encounterUuid?: string,
  labOrderUuid?: string,
) {
  const invalidate = useInvalidateOrderScope();
  return useMutation({
    mutationFn: async (itemUuid: string) => {
      await labOrderApi.deleteItem(encounterUuid!, labOrderUuid!, itemUuid);
    },
    onSuccess: invalidate,
  });
}

export function useCreateSpecimen(labOrderUuid?: string) {
  const invalidate = useInvalidateSpecimenScope();
  return useMutation({
    mutationFn: async (payload: CreateSpecimenPayload) =>
      (await labOrderApi.createSpecimen(labOrderUuid!, payload)).data as Specimen,
    onSuccess: invalidate,
  });
}

export function useSpecimenAction(labOrderUuid?: string) {
  const invalidate = useInvalidateSpecimenScope();
  return useMutation({
    mutationFn: async (input: { specimenUuid: string; action: SpecimenAction }) =>
      (
        await labOrderApi.specimenAction(labOrderUuid!, input.specimenUuid, input.action)
      ).data as Specimen,
    onSuccess: invalidate,
  });
}

export function useRejectSpecimen(labOrderUuid?: string) {
  const invalidate = useInvalidateSpecimenScope();
  return useMutation({
    mutationFn: async (input: { specimenUuid: string; payload: RejectSpecimenPayload }) =>
      (
        await labOrderApi.rejectSpecimen(labOrderUuid!, input.specimenUuid, input.payload)
      ).data as Specimen,
    onSuccess: invalidate,
  });
}
