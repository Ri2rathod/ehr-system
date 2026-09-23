"use client";

import { useMutation } from "@tanstack/react-query";
import { patientApi } from "../api/patient.api";

export function useCreatePatient() {
  return useMutation({
    mutationFn: async (payload: any) => {
      const res = await patientApi.create(payload);
      return res.data;
    },
  });
}

export function usePatientDuplicateCheck() {
  return useMutation({
    mutationFn: async (payload: {
      firstName: string;
      lastName: string;
      dateOfBirth: string;
      phone?: string;
    }) => {
      const res = await patientApi.checkDuplicates(payload);
      return res.data;
    },
  });
}
