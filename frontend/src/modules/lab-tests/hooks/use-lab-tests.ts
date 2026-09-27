"use client";

import { useQuery } from "@tanstack/react-query";
import { labTestApi } from "../api/lab-test.api";
import { LabTest, LabTestSearchPage } from "../types/lab-test.types";

/**
 * Debounce belongs to the caller (see prescription-item-dialog for the
 * 250 ms setTimeout pattern feeding this hook).
 */
export function useLabTestSearch(
  query: string,
  options?: { active?: boolean; size?: number },
) {
  const trimmed = query.trim();
  return useQuery({
    queryKey: ["lab-test-search", trimmed, options?.active ?? null, options?.size ?? 10],
    enabled: trimmed.length > 0,
    queryFn: async () => (await labTestApi.search(trimmed, options)).data as LabTestSearchPage,
  });
}

export function useLabTest(labTestUuid?: string) {
  return useQuery({
    queryKey: ["lab-test", labTestUuid],
    enabled: Boolean(labTestUuid),
    queryFn: async () => (await labTestApi.get(labTestUuid!)).data as LabTest,
  });
}
