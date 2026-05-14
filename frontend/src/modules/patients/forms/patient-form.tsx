"use client";

import React, { useMemo } from "react";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter } from "next/navigation";
import { patientSchema, PatientFormValues } from "../schemas/patient.schema";
import { useCreatePatient, usePatientDuplicateCheck } from "../hooks/use-create-patient";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="rounded-lg border border-outline-variant bg-surface-container-lowest p-4">
      <h2 className="mb-3 text-sm font-bold text-on-surface">{title}</h2>
      <div className="grid grid-cols-1 gap-3 md:grid-cols-2">{children}</div>
    </section>
  );
}

function Field({
  label,
  error,
  children,
}: {
  label: string;
  error?: string;
  children: React.ReactNode;
}) {
  return (
    <label className="block">
      <span className="mb-1 block text-[11px] font-bold uppercase tracking-wide text-on-surface-variant">{label}</span>
      {children}
      {error ? <span className="mt-1 block text-[11px] text-error">{error}</span> : null}
    </label>
  );
}

const inputCls =
  "h-9 w-full rounded-md border border-outline-variant bg-surface px-3 text-sm outline-none focus:border-primary";

export function PatientForm() {
  const router = useRouter();
  const createPatient = useCreatePatient();
  const duplicateCheck = usePatientDuplicateCheck();
  const [showDuplicateDialog, setShowDuplicateDialog] = useState(false);
  const [showDraftSaved, setShowDraftSaved] = useState(false);

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<PatientFormValues>({
    resolver: zodResolver(patientSchema),
    defaultValues: {
      gender: "UNKNOWN",
      countryCode: "+91",
      status: "ACTIVE",
      registrationDate: new Date().toISOString().slice(0, 10),
      tenant: "default",
    },
  });

  const dob = watch("dateOfBirth");
  const ageText = useMemo(() => {
    if (!dob) return "-";
    const d = new Date(dob);
    if (Number.isNaN(d.getTime())) return "-";
    const now = new Date();
    let age = now.getFullYear() - d.getFullYear();
    const m = now.getMonth() - d.getMonth();
    if (m < 0 || (m === 0 && now.getDate() < d.getDate())) age -= 1;
    return `${age} years`;
  }, [dob]);

  const onSubmit = async (values: PatientFormValues) => {
    const dup = await duplicateCheck.mutateAsync({
      firstName: values.firstName,
      lastName: values.lastName,
      dateOfBirth: values.dateOfBirth,
      phone: `${values.countryCode}${values.phoneNumber}`,
    });

    const duplicateItems = Array.isArray(dup) ? dup : dup?.items || dup?.content || [];
    if (duplicateItems.length > 0) {
      setShowDuplicateDialog(true);
      return;
    }

    const payload = {
      ...values,
      maritalStatus: values.maritalStatus || undefined,
      phone: `${values.countryCode}${values.phoneNumber}`,
      emergencyPhone: values.emergencyPhoneNumber,
    };

    const created = await createPatient.mutateAsync(payload);
    const patientId = created?.id || created?.patientId;
    if (patientId) {
      router.push(`/patients/${patientId}`);
      return;
    }
    router.push("/patients");
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4 pb-24">
      {showDraftSaved ? (
        <Alert>
          <AlertTitle>Draft saved</AlertTitle>
          <AlertDescription>Patient registration draft was saved locally for this browser session.</AlertDescription>
        </Alert>
      ) : null}

      <Section title="Personal Information">
        <Field label="Gender" error={errors.gender?.message}>
          <select className={inputCls} {...register("gender")}>
            <option value="MALE">Male</option>
            <option value="FEMALE">Female</option>
            <option value="OTHER">Other</option>
            <option value="TRANSGENDER">Transgender</option>
            <option value="UNKNOWN">Unknown</option>
          </select>
        </Field>
        <Field label="First Name" error={errors.firstName?.message}>
          <input className={inputCls} {...register("firstName")} />
        </Field>
        <Field label="Last Name" error={errors.lastName?.message}>
          <input className={inputCls} {...register("lastName")} />
        </Field>
        <Field label="Middle Name" error={errors.middleName?.message}>
          <input className={inputCls} {...register("middleName")} />
        </Field>
        <Field label="Display Name" error={errors.displayName?.message}>
          <input className={inputCls} {...register("displayName")} />
        </Field>
        <Field label="Date of Birth" error={errors.dateOfBirth?.message}>
          <input type="date" className={inputCls} {...register("dateOfBirth")} />
          <span className="mt-1 block text-[11px] text-on-surface-variant">Age: {ageText}</span>
        </Field>
        <Field label="Blood Group" error={errors.bloodGroup?.message}>
          <select className={inputCls} {...register("bloodGroup")}>
            <option value="">Select</option>
            <option value="A_POSITIVE">A+</option>
            <option value="A_NEGATIVE">A-</option>
            <option value="B_POSITIVE">B+</option>
            <option value="B_NEGATIVE">B-</option>
            <option value="AB_POSITIVE">AB+</option>
            <option value="AB_NEGATIVE">AB-</option>
            <option value="O_POSITIVE">O+</option>
            <option value="O_NEGATIVE">O-</option>
            <option value="UNKNOWN">Unknown</option>
          </select>
        </Field>
        <Field label="Marital Status" error={errors.maritalStatus?.message}>
          <select className={inputCls} {...register("maritalStatus")}>
            <option value="">Select</option>
            <option value="SINGLE">Single</option>
            <option value="MARRIED">Married</option>
            <option value="DIVORCED">Divorced</option>
            <option value="WIDOWED">Widowed</option>
            <option value="SEPARATED">Separated</option>
            <option value="UNKNOWN">Unknown</option>
          </select>
        </Field>
        <Field label="Profile Photo URL" error={errors.profilePhotoUrl?.message}>
          <input className={inputCls} placeholder="Upload patient photo" {...register("profilePhotoUrl")} />
        </Field>
      </Section>

      <Section title="Contact Information">
        <Field label="Email" error={errors.email?.message}>
          <input className={inputCls} {...register("email")} />
        </Field>
        <div className="grid grid-cols-3 gap-2">
          <Field label="Country Code" error={errors.countryCode?.message}>
            <input className={inputCls} {...register("countryCode")} />
          </Field>
          <div className="col-span-2">
            <Field label="Phone Number" error={errors.phoneNumber?.message}>
              <input className={inputCls} {...register("phoneNumber")} />
            </Field>
          </div>
        </div>
      </Section>

      <Section title="Emergency Contact">
        <Field label="Emergency Contact Name" error={errors.emergencyContactName?.message}>
          <input className={inputCls} {...register("emergencyContactName")} />
        </Field>
        <Field label="Relationship" error={errors.emergencyRelationship?.message}>
          <select className={inputCls} {...register("emergencyRelationship")}>
            <option value="">Select</option>
            <option value="Parent">Parent</option>
            <option value="Spouse">Spouse</option>
            <option value="Sibling">Sibling</option>
            <option value="Child">Child</option>
            <option value="Friend">Friend</option>
            <option value="Guardian">Guardian</option>
            <option value="Other">Other</option>
          </select>
        </Field>
        <Field label="Emergency Phone Number" error={errors.emergencyPhoneNumber?.message}>
          <input className={inputCls} {...register("emergencyPhoneNumber")} />
        </Field>
      </Section>

      <Section title="Address">
        <Field label="Address Line 1" error={errors.addressLine1?.message}>
          <input className={inputCls} {...register("addressLine1")} />
        </Field>
        <Field label="Address Line 2" error={errors.addressLine2?.message}>
          <input className={inputCls} {...register("addressLine2")} />
        </Field>
        <Field label="City" error={errors.city?.message}>
          <input className={inputCls} {...register("city")} />
        </Field>
        <Field label="State" error={errors.state?.message}>
          <input className={inputCls} {...register("state")} />
        </Field>
        <Field label="Postal Code" error={errors.postalCode?.message}>
          <input className={inputCls} {...register("postalCode")} />
        </Field>
        <Field label="Country" error={errors.country?.message}>
          <input className={inputCls} {...register("country")} />
        </Field>
      </Section>

      <Section title="Insurance Information">
        <Field label="Insurance Provider" error={errors.insuranceProvider?.message}>
          <input className={inputCls} {...register("insuranceProvider")} />
        </Field>
        <Field label="Policy Number" error={errors.policyNumber?.message}>
          <input className={inputCls} {...register("policyNumber")} />
        </Field>
      </Section>

      <section className="rounded-lg border border-tertiary/20 bg-tertiary-container/20 p-4">
        <h2 className="mb-3 text-sm font-bold text-on-surface">Medical Information</h2>
        <div className="grid grid-cols-1 gap-3">
          <Field label="Allergies" error={errors.allergies?.message}>
            <textarea className="w-full rounded-md border border-outline-variant bg-surface p-3 text-sm outline-none focus:border-primary" rows={3} {...register("allergies")} />
          </Field>
          <Field label="Chronic Conditions" error={errors.chronicConditions?.message}>
            <textarea className="w-full rounded-md border border-outline-variant bg-surface p-3 text-sm outline-none focus:border-primary" rows={3} {...register("chronicConditions")} />
          </Field>
          <Field label="Clinical Notes" error={errors.clinicalNotes?.message}>
            <textarea className="w-full rounded-md border border-outline-variant bg-surface p-3 text-sm outline-none focus:border-primary" rows={4} {...register("clinicalNotes")} />
          </Field>
        </div>
      </section>

      <Section title="Administrative">
        <Field label="Tenant" error={errors.tenant?.message}>
          <input className={inputCls} {...register("tenant")} />
        </Field>
        <Field label="Status" error={errors.status?.message}>
          <select className={inputCls} {...register("status")}>
            <option value="ACTIVE">ACTIVE</option>
            <option value="INACTIVE">INACTIVE</option>
          </select>
        </Field>
        <Field label="Registration Date" error={errors.registrationDate?.message}>
          <input type="date" className={inputCls} {...register("registrationDate")} />
        </Field>
      </Section>

      <div className="pl-64 fixed bottom-0 left-0 right-0 z-20 border-t border-outline-variant bg-surface-container-lowest/95 backdrop-blur">
        <div className="mx-auto flex max-w-[1400px] items-center justify-between gap-2 p-3">
          <button type="button" onClick={() => router.push("/patients")} className="h-9 rounded-md border border-outline-variant px-3 text-xs font-semibold">
            Cancel
          </button>
          <div className="ml-auto flex items-center gap-2">
            <button
              type="button"
              onClick={() => {
                const values = watch();
                localStorage.setItem("patient_registration_draft", JSON.stringify(values));
                setShowDraftSaved(true);
              }}
              className="h-9 rounded-md border border-outline-variant px-3 text-xs font-semibold"
            >
              Save Draft
            </button>
            <button
              type="submit"
              disabled={createPatient.isPending || duplicateCheck.isPending}
              className="h-9 rounded-md bg-primary px-3 text-xs font-bold text-on-primary disabled:opacity-50"
            >
              {createPatient.isPending ? "Registering..." : "Register Patient"}
            </button>
          </div>
        </div>
      </div>

      <AlertDialog open={showDuplicateDialog}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Possible duplicate patient found</AlertDialogTitle>
            <AlertDialogDescription>
              Matching patient demographics were detected. Please verify patient identity before registration.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogAction onClick={() => setShowDuplicateDialog(false)}>Review Details</AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </form>
  );
}
