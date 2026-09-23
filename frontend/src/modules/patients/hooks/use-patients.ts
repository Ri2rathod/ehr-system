"use client";

import { useQuery } from "@tanstack/react-query";
import { patientApi } from "../api/patient.api";
import { Patient, PatientListParams, PatientListResult } from "../types/patient.types";

export function normalizeListResponse(raw: any, fallback: { page: number; size: number }): PatientListResult {
  if (Array.isArray(raw)) {
    return {
      items: raw as Patient[],
      page: fallback.page,
      size: fallback.size,
      totalElements: raw.length,
      totalPages: 1,
    };
  }

  const items = (raw?.content || raw?.items || raw?.data || []) as Patient[];
  return {
    items,
    page: raw?.number ?? raw?.page ?? fallback.page,
    size: raw?.size ?? fallback.size,
    totalElements: raw?.totalElements ?? raw?.total ?? items.length,
    totalPages: raw?.totalPages ?? 1,
  };
}

export function usePatients(params: PatientListParams) {
  return useQuery({
    queryKey: ["patients", params],
    queryFn: async () => {
      const res = await patientApi.getAll(params);
      return normalizeListResponse(res.data, { page: params.page, size: params.size });
    },
  });
}
