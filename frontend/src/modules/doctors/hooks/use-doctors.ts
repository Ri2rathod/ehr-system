"use client";

import { useQuery } from "@tanstack/react-query";
import { doctorApi } from "../api/doctor.api";
import { Doctor, DoctorListParams, DoctorListResult } from "../types/doctor.types";

function normalize(raw: any, fallback: { page: number; size: number }): DoctorListResult {
  if (Array.isArray(raw)) {
    return {
      items: raw as Doctor[],
      page: fallback.page,
      size: fallback.size,
      totalElements: raw.length,
      totalPages: 1,
    };
  }

  const items = (raw?.content || raw?.items || raw?.data || []) as Doctor[];
  return {
    items,
    page: raw?.number ?? raw?.page ?? fallback.page,
    size: raw?.size ?? fallback.size,
    totalElements: raw?.totalElements ?? raw?.total ?? items.length,
    totalPages: raw?.totalPages ?? 1,
  };
}

export function useDoctors(params: DoctorListParams) {
  return useQuery({
    queryKey: ["doctors", params],
    queryFn: async () => {
      const res = await doctorApi.getAll(params);
      return normalize(res.data, { page: params.page, size: params.size });
    },
  });
}
