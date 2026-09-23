"use client";

import { useMutation } from "@tanstack/react-query";
import { doctorApi } from "../api/doctor.api";

export function useUpdateDoctor() {
  return useMutation({
    mutationFn: async ({ uuid, payload }: { uuid: string; payload: any }) => {
      const res = await doctorApi.update(uuid, payload);
      return res.data;
    },
  });
}
