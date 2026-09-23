"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Check } from "lucide-react";

import { Doctor } from "@/modules/doctors/types/doctor.types";
import { Patient } from "@/modules/patients/types/patient.types";

import {
  useCreateAppointment,
} from "../hooks/use-appointments";

import { DoctorSearch } from "../components/doctor-search";
import { PatientSearch } from "../components/patient-search";
import { DateTimeSlotSection } from "../components/date-time-slot-section";
import { VisitType } from "../types/appointment.types";
import {
  formatAppointmentDate,
  formatAppointmentTime12,
  visitTypeLabel,
} from "../utils/appointment-format";

const visitTypes: VisitType[] = [
  "CONSULTATION",
  "FOLLOW_UP",
  "EMERGENCY",
  "TELEMEDICINE",
  "PROCEDURE",
  "VACCINATION",
];

function StepIndicator({
  index,
  label,
  state,
}: {
  index: number;
  label: string;
  state: "done" | "active" | "todo";
}) {
  return (
    <li
      className={`flex items-center gap-1.5 ${
        state === "todo" ? "text-on-surface-variant" : "text-on-surface"
      }`}
    >
      <span
        className={`flex h-5 w-5 items-center justify-center rounded-full text-[10px] font-bold ${
          state === "done"
            ? "bg-primary text-on-primary"
            : state === "active"
              ? "border border-primary bg-primary/10 text-primary"
              : "border border-outline-variant bg-surface text-on-surface-variant"
        }`}
      >
        {state === "done" ? <Check className="h-3 w-3" aria-hidden /> : index}
      </span>
      <span
        className={`text-xs ${
          state === "active" ? "font-bold" : state === "done" ? "font-semibold" : "font-medium"
        }`}
      >
        {label}
      </span>
    </li>
  );
}

function SummaryRow({
  label,
  children,
}: {
  label: string;
  children: React.ReactNode;
}) {
  return (
    <div className="py-3">
      <dt className="text-[10px] font-bold uppercase tracking-wider text-on-surface-variant">
        {label}
      </dt>
      <dd className="mt-1 text-xs text-on-surface">{children}</dd>
    </div>
  );
}

function NotSelected() {
  return <span className="text-on-surface-variant">Not selected</span>;
}

export default function AppointmentForm() {
  const router = useRouter();

  const [patient, setPatient] = useState<Patient | null>(null);
  const [doctor, setDoctor] = useState<Doctor | null>(null);

  const [date, setDate] = useState("");
  const [slotStart, setSlotStart] = useState("");
  const [slotEnd, setSlotEnd] = useState("");

  const [visitType, setVisitType] =
    useState<VisitType>("CONSULTATION");

  const [reasonForVisit, setReasonForVisit] = useState("");
  const [notes, setNotes] = useState("");
  const [error, setError] = useState("");

  const patientUuid = patient
    ? patient.patientUuid || patient.uuid || patient.id
    : "";

  const doctorUuid = doctor?.uuid || "";

  const handleDoctorChange = (next: Doctor | null) => {
    setDoctor(next);
    setSlotStart("");
    setSlotEnd("");
  };

  const create = useCreateAppointment();

  const patientStep: "done" | "active" = patient ? "done" : "active";
  const doctorStep: "done" | "active" | "todo" = patient
    ? doctorUuid
      ? "done"
      : "active"
    : "todo";
  const slotStep: "done" | "active" | "todo" =
    !patient || !doctorUuid
      ? "todo"
      : slotStart
        ? "done"
        : "active";
  const detailsStep: "done" | "active" | "todo" =
    patient && doctorUuid && slotStart ? "active" : "todo";

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    setError("");

    if (
      !patientUuid ||
      !doctorUuid ||
      !date ||
      !slotStart ||
      !slotEnd
    ) {
      setError(
        "Select a patient, doctor, date, and available time slot."
      );

      return;
    }

    try {
      const appointment = await create.mutateAsync({
        patientUuid,
        doctorUuid,
        startTime: `${date}T${slotStart}`,
        endTime: `${date}T${slotEnd}`,
        visitType,
        reasonForVisit: reasonForVisit || undefined,
        notes: notes || undefined,
      });

      router.push(`/appointments/${appointment.uuid}`);
    } catch (caught: any) {
      const status = caught?.response?.status;

      if (status === 409) {
        setError(
          "This appointment conflicts with an existing schedule. Select another slot."
        );
      } else if (status === 403) {
        setError(
          "You do not have permission to create appointments."
        );
      } else {
        setError(
          "Unable to create the appointment. Review the information and try again."
        );
      }
    }
  };

  return (
    <form
      onSubmit={submit}
      className="space-y-4 p-5 pb-24"
    >
      <header>
        <h1 className="text-xl font-bold text-on-surface">
          New Appointment
        </h1>

        <p className="text-xs text-on-surface-variant">
          Select a patient, clinical resource, and an available slot.
        </p>
      </header>

      {error && (
        <div
          role="alert"
          className="rounded-md border border-error/30 bg-error-container/30 p-3 text-sm text-on-error-container"
        >
          {error}
        </div>
      )}

      {/* Booking progress */}
      <nav
        aria-label="Booking progress"
        className="flex flex-wrap items-center gap-x-4 gap-y-2 rounded-lg border border-outline-variant bg-surface-container-lowest px-4 py-3"
      >
        <ol className="flex flex-wrap items-center gap-x-4 gap-y-2">
          <StepIndicator index={1} label="Patient" state={patientStep} />
          <StepIndicator index={2} label="Doctor" state={doctorStep} />
          <StepIndicator index={3} label="Date & Time" state={slotStep} />
          <StepIndicator index={4} label="Details" state={detailsStep} />
          <StepIndicator index={5} label="Confirm" state="todo" />
        </ol>
      </nav>

      <div className="grid items-start gap-4 lg:grid-cols-12">
        {/* Main booking column */}
        <div className="space-y-4 lg:col-span-8">
          <section className="rounded-lg border border-outline-variant bg-surface-container-lowest p-4">
            <div className="mb-3 flex items-center justify-between gap-2">
              <div className="flex items-center gap-2">
                <span className="flex h-5 w-5 items-center justify-center rounded-full bg-primary text-[10px] font-bold text-on-primary">
                  1
                </span>
                <h2 className="text-sm font-bold text-on-surface">
                  Select Patient
                </h2>
              </div>
              {patient && (
                <span className="inline-flex items-center gap-1 text-[11px] font-bold text-primary">
                  <Check className="h-3.5 w-3.5" aria-hidden />
                  {patient.firstName} {patient.lastName} selected
                </span>
              )}
            </div>

            <p className="mb-3 text-xs text-on-surface-variant">
              Search by patient name, MRN, or phone number
            </p>

            <PatientSearch patient={patient} onChange={setPatient} />
          </section>

          <section className="rounded-lg border border-outline-variant bg-surface-container-lowest p-4">
            <div className="mb-3 flex items-center justify-between gap-2">
              <div className="flex items-center gap-2">
                <span
                  className={`flex h-5 w-5 items-center justify-center rounded-full text-[10px] font-bold ${
                    doctor
                      ? "bg-primary text-on-primary"
                      : patient
                        ? "border border-primary bg-primary/10 text-primary"
                        : "border border-outline-variant bg-surface text-on-surface-variant"
                  }`}
                >
                  {doctor ? (
                    <Check className="h-3 w-3" aria-hidden />
                  ) : (
                    2
                  )}
                </span>
                <h2 className="text-sm font-bold text-on-surface">
                  Select Doctor
                </h2>
              </div>
              {doctor && (
                <span className="inline-flex items-center gap-1 text-[11px] font-bold text-primary">
                  <Check className="h-3.5 w-3.5" aria-hidden />
                  {doctor.displayName ||
                    `${doctor.firstName} ${doctor.lastName}`}{" "}
                  selected
                </span>
              )}
            </div>

            <p className="mb-3 text-xs text-on-surface-variant">
              Search by name or specialization
            </p>

            <DoctorSearch doctor={doctor} onChange={handleDoctorChange} />
          </section>

          <DateTimeSlotSection
            doctorUuid={doctorUuid}
            date={date}
            onDateChange={(next) => {
              setDate(next);
              setSlotStart("");
              setSlotEnd("");
            }}
            slotStart={slotStart}
            slotEnd={slotEnd}
            onSlotChange={(start, end) => {
              setSlotStart(start);
              setSlotEnd(end);
            }}
          />

          {/* Appointment Details */}
          <section className="rounded-lg border border-outline-variant bg-surface-container-lowest p-4">
            <div className="mb-3 flex items-center gap-2.5">
              <span
                className={`flex h-6 w-6 shrink-0 items-center justify-center rounded-full text-[11px] font-bold ${
                  slotStart
                    ? "border border-primary bg-primary/10 text-primary"
                    : "border border-outline-variant bg-surface text-on-surface-variant"
                }`}
              >
                4
              </span>
              <div>
                <h2 className="text-base font-bold leading-tight text-on-surface">
                  Appointment Details
                </h2>
                <p className="mt-0.5 text-xs text-on-surface-variant">
                  Visit classification and clinical notes
                </p>
              </div>
            </div>

            <div className="space-y-4">
              <label className="block text-xs text-on-surface-variant">
                <span className="font-bold text-on-surface-variant">
                  Visit type
                </span>
                <select
                  value={visitType}
                  onChange={(event) =>
                    setVisitType(event.target.value as VisitType)
                  }
                  className="mt-1.5 block h-10 w-full rounded-md border border-outline-variant bg-surface px-2 text-sm text-on-surface focus:border-primary focus:outline-none"
                >
                  {visitTypes.map((type) => (
                    <option key={type} value={type}>
                      {visitTypeLabel[type]}
                    </option>
                  ))}
                </select>
              </label>

              <div className="grid gap-4 lg:grid-cols-2">
                <label className="block text-xs text-on-surface-variant">
                  <span className="font-bold">Reason for visit</span>
                  <textarea
                    value={reasonForVisit}
                    onChange={(event) =>
                      setReasonForVisit(event.target.value)
                    }
                    rows={4}
                    placeholder="Primary complaint or follow-up focus"
                    className="mt-1.5 block w-full rounded-md border border-outline-variant bg-surface p-3 text-sm text-on-surface placeholder:text-on-surface-variant/70 focus:border-primary focus:outline-none"
                  />
                </label>

                <label className="block text-xs text-on-surface-variant">
                  <span className="font-bold">Notes</span>
                  <textarea
                    value={notes}
                    onChange={(event) => setNotes(event.target.value)}
                    rows={4}
                    placeholder="Front-desk or clinical notes"
                    className="mt-1.5 block w-full rounded-md border border-outline-variant bg-surface p-3 text-sm text-on-surface placeholder:text-on-surface-variant/70 focus:border-primary focus:outline-none"
                  />
                </label>
              </div>
            </div>
          </section>
        </div>

        {/* Sticky summary panel */}
        <aside className="lg:col-span-4">
          <div className="space-y-4 lg:sticky lg:top-20">
            <section className="overflow-hidden rounded-lg border border-outline-variant bg-surface-container-lowest">
              <header className="flex items-center justify-between bg-surface-container-high px-4 py-3">
                <h2 className="text-sm font-bold text-on-surface">
                  Appointment Summary
                </h2>
                <span className="rounded border border-outline-variant bg-surface px-1.5 py-0.5 font-mono text-[10px] font-bold uppercase text-on-surface-variant">
                  Draft
                </span>
              </header>

              <dl className="divide-y divide-outline-variant/60 px-4 text-xs">
                <SummaryRow label="Patient">
                  {patient ? (
                    <div className="space-y-0.5">
                      <p className="text-sm font-bold text-on-surface">
                        {patient.firstName} {patient.lastName}
                      </p>
                      <p className="font-mono text-[11px] text-on-surface-variant">
                        {patient.mrn}
                      </p>
                      {patient.phone && (
                        <p className="font-mono text-[11px] text-on-surface-variant">
                          {patient.phone}
                        </p>
                      )}
                    </div>
                  ) : (
                    <NotSelected />
                  )}
                </SummaryRow>

                <SummaryRow label="Doctor">
                  {doctor ? (
                    <div className="space-y-0.5">
                      <p className="font-bold text-on-surface">
                        {doctor.displayName ||
                          `${doctor.firstName} ${doctor.lastName}`}
                      </p>
                      <p className="text-on-surface-variant">
                        {doctor.specialization || "General"}
                      </p>
                      {doctor.department && (
                        <p className="text-on-surface-variant">
                          {doctor.department}
                        </p>
                      )}
                    </div>
                  ) : (
                    <NotSelected />
                  )}
                </SummaryRow>

                <SummaryRow label="Date">
                  {date ? (
                    <span className="font-semibold">
                      {formatAppointmentDate(date)}
                    </span>
                  ) : (
                    <NotSelected />
                  )}
                </SummaryRow>

                <SummaryRow label="Time">
                  {slotStart && slotEnd ? (
                    <span className="font-mono font-semibold">
                      {formatAppointmentTime12(slotStart)} –{" "}
                      {formatAppointmentTime12(slotEnd)}
                    </span>
                  ) : (
                    <NotSelected />
                  )}
                </SummaryRow>

                <SummaryRow label="Visit type">
                  <span className="font-semibold">
                    {visitTypeLabel[visitType]}
                  </span>
                </SummaryRow>
              </dl>

              <div className="border-t border-outline-variant bg-surface-container-low px-4 py-3">
                <p className="text-[11px] text-on-surface-variant">
                  {patient && doctorUuid && date && slotStart
                    ? "Ready to confirm. Use Create Appointment to book."
                    : "Complete each step to enable booking."}
                </p>
              </div>
            </section>
          </div>
        </aside>
      </div>

      {/* Footer Actions */}
      <div className="fixed bottom-0 left-0 right-0 z-20 border-t border-outline-variant bg-surface-container-lowest/95 p-3">
        <div className="mx-auto flex max-w-[1400px] justify-between">
          <button
            type="button"
            onClick={() => router.back()}
            className="h-9 rounded-md border border-outline-variant px-3 text-xs font-semibold"
          >
            Cancel
          </button>

          <button
            type="submit"
            disabled={create.isPending}
            className="h-9 rounded-md bg-primary px-3 text-xs font-bold text-on-primary disabled:opacity-50"
          >
            {create.isPending
              ? "Creating…"
              : "Create Appointment"}
          </button>
        </div>
      </div>
    </form>
  );
}
