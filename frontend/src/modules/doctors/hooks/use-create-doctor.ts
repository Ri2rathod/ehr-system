"use client";

import { useMutation } from "@tanstack/react-query";
import { doctorApi } from "../api/doctor.api";

export function useCreateDoctor() {
  return useMutation({
    mutationFn: async (payload: any) => {
      const res = await doctorApi.create(payload);
      return res.data;
    },
  });
}
