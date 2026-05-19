"use client";

import { useMutation } from "@tanstack/react-query";
import { doctorApi } from "../api/doctor.api";

export function useDeleteDoctor() {
  return useMutation({
    mutationFn: async (uuid: string) => {
      const res = await doctorApi.deactivate(uuid);
      return res.data;
    },
  });
}
