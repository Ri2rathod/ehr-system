import { z } from "zod";

export const doctorSchema = z.object({
  firstName: z.string().min(1, "First name is required"),
  lastName: z.string().min(1, "Last name is required"),
  displayName: z.string().optional(),
  gender: z.string().optional(),
  dateOfBirth: z.string().optional(),
  email: z.email("Invalid email").or(z.literal("")).optional(),
  phone: z.string().min(7, "Phone number is too short").optional(),
  address: z.string().optional(),
  specialization: z.string().min(1, "Specialization is required"),
  department: z.string().min(1, "Department is required"),
  licenseNumber: z.string().min(1, "License number is required"),
  experienceYears: z.number().min(0).optional(),
  qualification: z.string().optional(),
  consultationFee: z.number().min(0, "Consultation fee must be positive"),
  availableDays: z.array(z.string()).optional(),
  consultationHours: z.string().optional(),
  breakTimes: z.string().optional(),
  onlineAvailable: z.boolean().optional(),
  status: z.enum(["ACTIVE", "ON_LEAVE", "INACTIVE", "SUSPENDED"]),
  version: z.number().optional(),
});

export type DoctorFormValues = z.infer<typeof doctorSchema>;
