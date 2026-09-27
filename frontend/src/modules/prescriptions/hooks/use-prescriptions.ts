"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { prescriptionApi } from "../api/prescription.api";
import {
  CreatePrescriptionItemPayload,
  CreatePrescriptionPayload,
  Prescription,
  PrescriptionItem,
  UpdatePrescriptionItemPayload,
  UpdatePrescriptionPayload,
} from "../types/prescription.types";

export type PrescriptionAction = "activate" | "complete" | "cancel" | "void";
export type PrescriptionItemAction = "complete" | "cancel" | "discontinue";

export function usePrescriptions(encounterUuid?: string) {
  return useQuery({
    queryKey: ["encounter-prescriptions", encounterUuid],
    enabled: Boolean(encounterUuid),
    queryFn: async () =>
      (await prescriptionApi.list(encounterUuid!)).data as Prescription[],
  });
}

export function usePrescriptionItems(encounterUuid?: string, prescriptionUuid?: string) {
  return useQuery({
    queryKey: ["prescription-items", prescriptionUuid],
    enabled: Boolean(encounterUuid) && Boolean(prescriptionUuid),
    queryFn: async () =>
      (await prescriptionApi.listItems(encounterUuid!, prescriptionUuid!))
        .data as PrescriptionItem[],
  });
}

function useInvalidatePrescriptions(encounterUuid?: string) {
  const queryClient = useQueryClient();
  return () =>
    queryClient.invalidateQueries({
      queryKey: ["encounter-prescriptions", encounterUuid],
    });
}

function useInvalidateItems() {
  const queryClient = useQueryClient();
  return () => queryClient.invalidateQueries({ queryKey: ["prescription-items"] });
}

export function useCreatePrescription(encounterUuid?: string) {
  const invalidate = useInvalidatePrescriptions(encounterUuid);
  return useMutation({
    mutationFn: async (payload: CreatePrescriptionPayload) =>
      (await prescriptionApi.create(encounterUuid!, payload)).data as Prescription,
    onSuccess: invalidate,
  });
}

export function useUpdatePrescription(encounterUuid?: string) {
  const invalidate = useInvalidatePrescriptions(encounterUuid);
  return useMutation({
    mutationFn: async ({
      prescriptionUuid,
      payload,
    }: {
      prescriptionUuid: string;
      payload: UpdatePrescriptionPayload;
    }) =>
      (
        await prescriptionApi.update(encounterUuid!, prescriptionUuid, payload)
      ).data as Prescription,
    onSuccess: invalidate,
  });
}

export function usePrescriptionAction(encounterUuid?: string) {
  const invalidate = useInvalidatePrescriptions(encounterUuid);
  return useMutation({
    mutationFn: async ({
      prescriptionUuid,
      action,
    }: {
      prescriptionUuid: string;
      action: PrescriptionAction;
    }) => {
      if (action === "activate")
        return (
          await prescriptionApi.activate(encounterUuid!, prescriptionUuid)
        ).data as Prescription;
      if (action === "complete")
        return (
          await prescriptionApi.complete(encounterUuid!, prescriptionUuid)
        ).data as Prescription;
      if (action === "cancel")
        return (
          await prescriptionApi.cancel(encounterUuid!, prescriptionUuid)
        ).data as Prescription;
      return (await prescriptionApi.void(encounterUuid!, prescriptionUuid))
        .data as Prescription;
    },
    onSuccess: invalidate,
  });
}

export function useDeletePrescription(encounterUuid?: string) {
  const invalidate = useInvalidatePrescriptions(encounterUuid);
  return useMutation({
    mutationFn: async (prescriptionUuid: string) => {
      await prescriptionApi.delete(encounterUuid!, prescriptionUuid);
    },
    onSuccess: invalidate,
  });
}

export function useCreatePrescriptionItem(encounterUuid?: string) {
  const invalidate = useInvalidateItems();
  const invalidatePrescriptions = useInvalidatePrescriptions(encounterUuid);
  return useMutation({
    mutationFn: async ({
      prescriptionUuid,
      payload,
    }: {
      prescriptionUuid: string;
      payload: CreatePrescriptionItemPayload;
    }) =>
      (
        await prescriptionApi.createItem(encounterUuid!, prescriptionUuid, payload)
      ).data as PrescriptionItem,
    onSuccess: () => {
      invalidate();
      invalidatePrescriptions();
    },
  });
}

export function useUpdatePrescriptionItem(encounterUuid?: string) {
  const invalidate = useInvalidateItems();
  return useMutation({
    mutationFn: async ({
      prescriptionUuid,
      itemUuid,
      payload,
    }: {
      prescriptionUuid: string;
      itemUuid: string;
      payload: UpdatePrescriptionItemPayload;
    }) =>
      (
        await prescriptionApi.updateItem(
          encounterUuid!,
          prescriptionUuid,
          itemUuid,
          payload,
        )
      ).data as PrescriptionItem,
    onSuccess: invalidate,
  });
}

export function usePrescriptionItemAction(encounterUuid?: string) {
  const invalidate = useInvalidateItems();
  return useMutation({
    mutationFn: async ({
      prescriptionUuid,
      itemUuid,
      action,
    }: {
      prescriptionUuid: string;
      itemUuid: string;
      action: PrescriptionItemAction;
    }) => {
      if (action === "complete")
        return (
          await prescriptionApi.completeItem(encounterUuid!, prescriptionUuid, itemUuid)
        ).data as PrescriptionItem;
      if (action === "cancel")
        return (
          await prescriptionApi.cancelItem(encounterUuid!, prescriptionUuid, itemUuid)
        ).data as PrescriptionItem;
      return (
        await prescriptionApi.discontinueItem(encounterUuid!, prescriptionUuid, itemUuid)
      ).data as PrescriptionItem;
    },
    onSuccess: invalidate,
  });
}

export function useDeletePrescriptionItem(encounterUuid?: string) {
  const invalidate = useInvalidateItems();
  return useMutation({
    mutationFn: async ({
      prescriptionUuid,
      itemUuid,
    }: {
      prescriptionUuid: string;
      itemUuid: string;
    }) => {
      await prescriptionApi.deleteItem(encounterUuid!, prescriptionUuid, itemUuid);
    },
    onSuccess: invalidate,
  });
}
