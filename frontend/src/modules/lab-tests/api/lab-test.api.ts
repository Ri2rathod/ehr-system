import { apiClient } from "@/services/api-client";
import { LabTestSearchPage, ResultType, SpecimenType } from "../types/lab-test.types";

export const labTestApi = {
  search: (query: string, options?: { active?: boolean; size?: number }) =>
    apiClient.get("/lab-tests", {
      params: {
        query,
        active: options?.active,
        page: 0,
        size: options?.size ?? 10,
        sortBy: "name",
        sortDir: "asc",
      },
    }),
  get: (labTestUuid: string) => apiClient.get(`/lab-tests/${labTestUuid}`),
  create: (payload: {
    code: string;
    name: string;
    resultType: ResultType;
    codeSystem?: string;
    shortName?: string;
    description?: string;
    category?: string;
    specimenType?: SpecimenType;
    unit?: string;
    defaultReferenceLow?: number;
    defaultReferenceHigh?: number;
    defaultReferenceText?: string;
  }) => apiClient.post("/lab-tests", payload),
};

export type { LabTestSearchPage };
