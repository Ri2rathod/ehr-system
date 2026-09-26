"use client";

import { useMemo, useState } from "react";
import { PatientSearch } from "@/modules/appointments/components/patient-search";
import { DoctorSearch } from "@/modules/appointments/components/doctor-search";
import { Patient } from "@/modules/patients/types/patient.types";
import { Doctor } from "@/modules/doctors/types/doctor.types";
import { toLocalDate } from "@/modules/appointments/utils/appointment-format";
import { useEncounters } from "../hooks/use-encounters";
import { EncounterStatus, EncounterType } from "../types/encounter.types";
import { EncounterTable } from "../components/encounter-table";

const statuses: EncounterStatus[] = ["DRAFT", "IN_PROGRESS", "COMPLETED", "CANCELLED"];
const encounterTypes: EncounterType[] = [
  "CONSULTATION",
  "FOLLOW_UP",
  "EMERGENCY",
  "TELEMEDICINE",
  "PROCEDURE",
  "VACCINATION",
  "WALK_IN",
];

export default function EncounterListPage() {
  const [status, setStatus] = useState<EncounterStatus | "">("");
  const [encounterType, setEncounterType] = useState<EncounterType | "">("");
  const [dateFrom, setDateFrom] = useState("");
  const [dateTo, setDateTo] = useState("");
  const [patient, setPatient] = useState<Patient | null>(null);
  const [doctor, setDoctor] = useState<Doctor | null>(null);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);

  const patientUuid = patient ? patient.patientUuid || patient.uuid || patient.id : undefined;
  const doctorUuid = doctor ? doctor.uuid || doctor.id : undefined;

  const filters = useMemo(
    () => ({
      status: status || undefined,
      encounterType: encounterType || undefined,
      patientUuid,
      doctorUuid,
      dateFrom: dateFrom || undefined,
      dateTo: dateTo || undefined,
      page,
      size,
      sortBy: "startedAt",
      sortDir: "desc" as const,
    }),
    [status, encounterType, patientUuid, doctorUuid, dateFrom, dateTo, page, size],
  );

  const { data, isLoading, isError } = useEncounters(filters);
  const items = data?.items || [];
  const totalPages = data?.totalPages || 1;

  const setRange = (range: "today" | "week") => {
    const now = new Date();
    const from = toLocalDate(now);
    const to = new Date(now);
    if (range === "week") to.setDate(now.getDate() + 6);
    setDateFrom(from);
    setDateTo(toLocalDate(to));
    setPage(0);
  };

  const clearFilters = () => {
    setStatus("");
    setEncounterType("");
    setDateFrom("");
    setDateTo("");
    setPatient(null);
    setDoctor(null);
    setPage(0);
  };

  const count = (value: EncounterStatus) => items.filter((item) => item.status === value).length;

  return (
    <div className="space-y-4 p-5">
      <header className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-xl font-bold text-on-surface">Encounters</h1>
          <p className="text-xs text-on-surface-variant">
            Clinical visits started from checked-in appointments.
          </p>
        </div>
      </header>

      <section className="grid grid-cols-2 gap-2 md:grid-cols-4">
        {[
          ["Loaded", items.length],
          ["In progress", count("IN_PROGRESS")],
          ["Completed", count("COMPLETED")],
          ["Cancelled", count("CANCELLED")],
        ].map(([label, value]) => (
          <div
            key={String(label)}
            className="rounded-md border border-outline-variant bg-surface-container-lowest p-3"
          >
            <p className="text-[10px] font-bold uppercase tracking-wider text-on-surface-variant">
              {label}
            </p>
            <p className="mt-1 text-lg font-bold text-on-surface">{value}</p>
          </div>
        ))}
      </section>

      <section className="space-y-3 rounded-lg border border-outline-variant bg-surface-container-lowest p-3">
        <div className="flex flex-wrap items-end gap-2">
          <div>
            <label className="block text-[10px] font-bold uppercase tracking-wide text-on-surface-variant">
              Date
            </label>
            <div className="mt-1 flex gap-1">
              <button
                onClick={() => setRange("today")}
                className="rounded border border-outline-variant px-2 py-1 text-xs"
              >
                Today
              </button>
              <button
                onClick={() => setRange("week")}
                className="rounded border border-outline-variant px-2 py-1 text-xs"
              >
                This week
              </button>
            </div>
          </div>
          <label className="text-xs text-on-surface-variant">
            From
            <input
              aria-label="Start date"
              type="date"
              value={dateFrom}
              onChange={(event) => {
                setDateFrom(event.target.value);
                setPage(0);
              }}
              className="mt-1 block h-9 rounded border border-outline-variant bg-surface px-2"
            />
          </label>
          <label className="text-xs text-on-surface-variant">
            To
            <input
              aria-label="End date"
              type="date"
              value={dateTo}
              onChange={(event) => {
                setDateTo(event.target.value);
                setPage(0);
              }}
              className="mt-1 block h-9 rounded border border-outline-variant bg-surface px-2"
            />
          </label>
          <label className="text-xs text-on-surface-variant">
            Status
            <select
              value={status}
              onChange={(event) => {
                setStatus(event.target.value as EncounterStatus | "");
                setPage(0);
              }}
              className="mt-1 block h-9 rounded border border-outline-variant bg-surface px-2"
            >
              <option value="">All statuses</option>
              {statuses.map((item) => (
                <option key={item}>{item}</option>
              ))}
            </select>
          </label>
          <label className="text-xs text-on-surface-variant">
            Encounter type
            <select
              value={encounterType}
              onChange={(event) => {
                setEncounterType(event.target.value as EncounterType | "");
                setPage(0);
              }}
              className="mt-1 block h-9 rounded border border-outline-variant bg-surface px-2"
            >
              <option value="">All types</option>
              {encounterTypes.map((item) => (
                <option key={item}>{item}</option>
              ))}
            </select>
          </label>
          <button
            onClick={clearFilters}
            className="h-9 rounded border border-outline-variant px-3 text-xs font-semibold text-on-surface-variant"
          >
            Clear
          </button>
        </div>
        <div className="grid gap-2 md:grid-cols-2">
          <div>
            <label className="block text-[10px] font-bold uppercase tracking-wide text-on-surface-variant">
              Patient
            </label>
            <div className="mt-1">
              <PatientSearch patient={patient} onChange={setPatient} />
            </div>
          </div>
          <div>
            <label className="block text-[10px] font-bold uppercase tracking-wide text-on-surface-variant">
              Doctor
            </label>
            <div className="mt-1">
              <DoctorSearch doctor={doctor} onChange={setDoctor} />
            </div>
          </div>
        </div>
      </section>

      {isLoading ? (
        <section className="rounded-lg border border-outline-variant bg-surface-container-lowest p-6 text-sm text-on-surface-variant">
          Loading encounters…
        </section>
      ) : isError ? (
        <section className="rounded-lg border border-error/30 bg-error-container/30 p-4 text-sm text-on-error-container">
          Unable to load encounters. Please refresh and try again.
        </section>
      ) : (
        <EncounterTable items={items} />
      )}

      <section className="flex flex-wrap items-center justify-between gap-2 rounded-lg border border-outline-variant bg-surface-container-lowest p-3 text-xs">
        <span className="text-on-surface-variant">
          Page {page + 1} of {Math.max(totalPages, 1)} • {data?.totalElements || 0} records
        </span>
        <div className="flex items-center gap-2">
          <select
            value={size}
            onChange={(event) => {
              setSize(Number(event.target.value));
              setPage(0);
            }}
            aria-label="Rows per page"
            className="h-8 rounded border border-outline-variant bg-surface px-2"
          >
            <option value={20}>20</option>
            <option value={50}>50</option>
            <option value={100}>100</option>
          </select>
          <button
            disabled={page === 0}
            onClick={() => setPage((value) => Math.max(0, value - 1))}
            className="h-8 rounded border border-outline-variant px-2 disabled:opacity-40"
          >
            Previous
          </button>
          <button
            disabled={page + 1 >= totalPages}
            onClick={() => setPage((value) => value + 1)}
            className="h-8 rounded border border-outline-variant px-2 disabled:opacity-40"
          >
            Next
          </button>
        </div>
      </section>
    </div>
  );
}
