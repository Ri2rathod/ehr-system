export const permissions = {
  // Patient Permissions
  PATIENT_VIEW: "patient.view",
  PATIENT_CREATE: "patient.create",
  PATIENT_EDIT: "patient.edit",
  PATIENT_DELETE: "patient.delete",

  // Appointment Permissions
  APPOINTMENT_VIEW: "appointment.view",
  APPOINTMENT_CREATE: "appointment.create",
  
  // Clinical Permissions
  ENCOUNTER_VIEW: "encounter.view",
  ENCOUNTER_CREATE: "encounter.create",
  
  // Administrative Permissions
  BILLING_VIEW: "billing.view",
  BILLING_MANAGE: "billing.manage",
  DOCTOR_MANAGE: "doctor.manage",
} as const;

export type Permission = typeof permissions[keyof typeof permissions];
