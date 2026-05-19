"use client";

import { useQuery } from "@tanstack/react-query";
import { doctorApi } from "../api/doctor.api";

export function useDoctor(uuid: string) {
  return useQuery({
    queryKey: ["doctor", uuid],
    queryFn: async () => {
      const res = await doctorApi.getByUuid(uuid);
      return res.data;
    },
    enabled: !!uuid,
  });
}
