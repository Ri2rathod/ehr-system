"use client";

import { useQuery } from "@tanstack/react-query";
import { patientApi } from "../api/patient.api";
import { normalizeListResponse } from "./use-patients";

export const PATIENT_SEARCH_MIN_LENGTH = 2;

export function usePatientSearch(query: string, size = 20) {
  const q = query.trim();

  return useQuery({
    queryKey: ["patients", "search", { q, size }],
    enabled: q.length >= PATIENT_SEARCH_MIN_LENGTH,
    retry: 0,
    staleTime: 30_000,
    queryFn: async () => {
      const res = await patientApi.search({ q, page: 0, size });
      return normalizeListResponse(res.data, { page: 0, size });
    },
  });
}
