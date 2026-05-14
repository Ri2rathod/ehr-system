import { z } from "zod";

export const patientSchema = z.object({
  firstName: z.string().min(1, "First name is required"),
  middleName: z.string().optional(),
  lastName: z.string().min(1, "Last name is required"),
  displayName: z.string().optional(),
  gender: z.enum(["MALE", "FEMALE", "OTHER", "TRANSGENDER", "UNKNOWN"]),
  dateOfBirth: z.string().min(1, "Date of birth is required"),
  bloodGroup: z
    .enum([
      "A_POSITIVE",
      "A_NEGATIVE",
      "B_POSITIVE",
      "B_NEGATIVE",
      "AB_POSITIVE",
      "AB_NEGATIVE",
      "O_POSITIVE",
      "O_NEGATIVE",
      "UNKNOWN",
    ])
    .optional(),
  maritalStatus: z
    .enum(["SINGLE", "MARRIED", "DIVORCED", "WIDOWED", "SEPARATED", "UNKNOWN"])
    .or(z.literal(""))
    .optional(),
  profilePhotoUrl: z.string().optional(),

  email: z.email("Invalid email").or(z.literal("")).optional(),
  countryCode: z.string().default("+91"),
  phoneNumber: z
    .string()
    .min(7, "Phone number is too short")
    .max(15, "Phone number is too long")
    .regex(/^[0-9]+$/, "Phone number must contain digits only"),

  emergencyContactName: z.string().optional(),
  emergencyRelationship: z.string().optional(),
  emergencyPhoneNumber: z.string().optional(),

  addressLine1: z.string().optional(),
  addressLine2: z.string().optional(),
  city: z.string().optional(),
  state: z.string().optional(),
  postalCode: z.string().optional(),
  country: z.string().optional(),

  insuranceProvider: z.string().optional(),
  policyNumber: z.string().optional(),

  allergies: z.string().optional(),
  chronicConditions: z.string().optional(),
  clinicalNotes: z.string().optional(),

  tenant: z.string().min(1, "Tenant is required"),
  status: z.enum(["ACTIVE", "INACTIVE", "ARCHIVED", "BLOCKED", "DECEASED"]).default("ACTIVE"),
  registrationDate: z.string().min(1, "Registration date is required"),
});

export type PatientFormValues = z.infer<typeof patientSchema>;
