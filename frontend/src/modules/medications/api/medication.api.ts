import { apiClient } from "@/services/api-client";
import { DosageForm, Medication, PagedResponse } from "../types/medication.types";

export const medicationApi = {
  search: (query: string, options?: { dosageForm?: DosageForm; includeInactive?: boolean; size?: number }) =>
    apiClient.get("/medications", {
      params: {
        query,
        dosageForm: options?.dosageForm,
        includeInactive: options?.includeInactive,
        page: 0,
        size: options?.size ?? 10,
        sortBy: "genericName",
        sortDir: "asc",
      },
    }),
};

export type MedicationSearchPage = PagedResponse<Medication>;
