"use client";

import { useQuery } from "@tanstack/react-query";
import { medicationApi, MedicationSearchPage } from "../api/medication.api";

/**
 * Server-side medication search for the prescribing autocomplete.
 * Debounce is handled by the caller (input value), not here.
 */
export function useMedicationSearch(query: string, options?: { includeInactive?: boolean }) {
  const trimmed = query.trim();
  return useQuery({
    queryKey: ["medication-search", trimmed, options?.includeInactive ?? false],
    enabled: trimmed.length > 0,
    queryFn: async () =>
      (await medicationApi.search(trimmed, { includeInactive: options?.includeInactive }))
        .data as MedicationSearchPage,
  });
}
