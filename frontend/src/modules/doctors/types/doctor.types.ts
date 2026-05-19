export type DoctorStatus = "ACTIVE" | "ON_LEAVE" | "INACTIVE" | "SUSPENDED";

export interface Doctor {
  id?: string;
  uuid: string;
  firstName: string;
  lastName: string;
  displayName?: string;
  gender?: string;
  dateOfBirth?: string;
  email?: string;
  phone?: string;
  address?: string;
  specialization?: string;
  department?: string;
  licenseNumber?: string;
  experienceYears?: number;
  qualification?: string;
  consultationFee?: number;
  availableDays?: string[];
  consultationHours?: string;
  breakTimes?: string;
  onlineAvailable?: boolean;
  appointmentsToday?: number;
  status: DoctorStatus;
  createdAt?: string;
  updatedAt?: string;
}

export interface DoctorListResult {
  items: Doctor[];
  page: number;
  size: number;
  totalPages: number;
  totalElements: number;
}

export interface DoctorListParams {
  q?: string;
  status?: string;
  department?: string;
  specialization?: string;
  page: number;
  size: number;
}

export interface CreateDoctorRequest {
  firstName: string;
  lastName: string;
  displayName?: string;
  gender?: string;
  dateOfBirth?: string;
  email?: string;
  phone?: string;
  address?: string;
  specialization: string;
  department: string;
  licenseNumber: string;
  experienceYears?: number;
  qualification?: string;
  consultationFee: number;
  availableDays?: string[];
  consultationHours?: string;
  breakTimes?: string;
  onlineAvailable?: boolean;
  status: DoctorStatus;
}

export type UpdateDoctorRequest = Partial<CreateDoctorRequest> & { version?: number };
