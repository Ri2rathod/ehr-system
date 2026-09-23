"use client";

import { useQuery } from "@tanstack/react-query";
import { doctorApi } from "../api/doctor.api";
import { normalize } from "./use-doctors";

export const DOCTOR_SEARCH_MIN_LENGTH = 2;

export function useDoctorSearch(query: string, size = 20) {
  const q = query.trim();

  return useQuery({
    queryKey: ["doctors", "search", { q, size }],
    enabled: q.length >= DOCTOR_SEARCH_MIN_LENGTH,
    retry: 0,
    staleTime: 30_000,
    queryFn: async () => {
      const res = await doctorApi.search({ q, page: 0, size });
      return normalize(res.data, { page: 0, size });
    },
  });
}
