"use client";

import React, { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { usePatients } from "../hooks/use-patients";
import { PatientToolbar } from "../components/patient-toolbar";
import { PatientTable } from "../components/patient-table";

export default function PatientListPage() {
  const router = useRouter();
  const [search, setSearch] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");
  const [status, setStatus] = useState("");
  const [gender, setGender] = useState("");
  const [bloodGroup, setBloodGroup] = useState("");
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(25);

  useEffect(() => {
    const timeout = setTimeout(() => {
      setDebouncedSearch(search.trim());
      setPage(0);
    }, 300);

    return () => clearTimeout(timeout);
  }, [search]);

  const queryParams = useMemo(
    () => ({
      q: debouncedSearch || undefined,
      status: status || undefined,
      gender: gender || undefined,
      bloodGroup: bloodGroup || undefined,
      page,
      size,
      sortBy: "registeredAt",
      sortDir: "desc" as const,
    }),
    [debouncedSearch, status, gender, bloodGroup, page, size]
  );

  const { data, isLoading } = usePatients(queryParams);

  const items = data?.items || [];
  const total = data?.totalElements || 0;
  const active = items.filter((p) => p.status === "ACTIVE").length;
  const inactive = items.filter((p) => p.status === "INACTIVE").length;
  const deceased = items.filter((p) => p.status === "DECEASED" || p.isDeceased).length;
  const totalPages = data?.totalPages || 1;

  return (
    <div className="space-y-4 p-5">
      <header className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-xl font-bold text-on-surface">Patients</h1>
          <p className="text-xs text-on-surface-variant">Manage patient records and demographics</p>
        </div>
        <div className="flex items-center gap-2">
          <button className="h-9 rounded-md border border-outline-variant px-3 text-xs font-semibold">Export</button>
          <button className="h-9 rounded-md border border-outline-variant px-3 text-xs font-semibold">Import</button>
          <button
            onClick={() => router.push("/patients/new")}
            className="h-9 rounded-md bg-primary px-3 text-xs font-bold text-on-primary"
          >
            Register Patient
          </button>
        </div>
      </header>

      <PatientToolbar
        search={search}
        status={status}
        gender={gender}
        bloodGroup={bloodGroup}
        onSearchChange={setSearch}
        onStatusChange={(value) => {
          setStatus(value);
          setPage(0);
        }}
        onGenderChange={(value) => {
          setGender(value);
          setPage(0);
        }}
        onBloodGroupChange={(value) => {
          setBloodGroup(value);
          setPage(0);
        }}
      />

      <section className="grid grid-cols-2 gap-2 md:grid-cols-5">
        <StatCard label="Total Patients" value={total} />
        <StatCard label="Active" value={active} />
        <StatCard label="Inactive" value={inactive} />
        <StatCard label="Deceased" value={deceased} />
        <StatCard label="Today's Registrations" value={items.filter((p) => !!p.registeredAt).length} />
      </section>

      {isLoading ? (
        <section className="rounded-lg border border-outline-variant bg-surface-container-lowest p-6 text-sm text-on-surface-variant">
          Loading patients...
        </section>
      ) : (
        <PatientTable items={items} />
      )}

      <section className="flex flex-wrap items-center justify-between gap-2 rounded-lg border border-outline-variant bg-surface-container-lowest p-3 text-xs">
        <div className="text-on-surface-variant">
          Page {page + 1} of {Math.max(totalPages, 1)} • {total} records
        </div>
        <div className="flex items-center gap-2">
          <label className="text-on-surface-variant">Rows:</label>
          <select
            value={size}
            onChange={(e) => {
              setSize(Number(e.target.value));
              setPage(0);
            }}
            className="h-8 rounded border border-outline-variant bg-surface px-2"
          >
            <option value={25}>25</option>
            <option value={50}>50</option>
            <option value={100}>100</option>
          </select>
          <button
            disabled={page === 0}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            className="h-8 rounded border border-outline-variant px-2 disabled:opacity-40"
          >
            Prev
          </button>
          <button
            disabled={page + 1 >= totalPages}
            onClick={() => setPage((p) => p + 1)}
            className="h-8 rounded border border-outline-variant px-2 disabled:opacity-40"
          >
            Next
          </button>
        </div>
      </section>
    </div>
  );
}

function StatCard({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-md border border-outline-variant bg-surface-container-lowest p-3">
      <p className="text-[10px] font-bold uppercase tracking-wider text-on-surface-variant">{label}</p>
      <p className="mt-1 text-lg font-bold text-on-surface">{value}</p>
    </div>
  );
}
