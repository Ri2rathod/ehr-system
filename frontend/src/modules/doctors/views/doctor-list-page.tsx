"use client";

import { useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { useDoctors } from "../hooks/use-doctors";
import { DoctorTable } from "../components/doctor-table";

export default function DoctorListPage() {
  const router = useRouter();
  const [q, setQ] = useState("");
  const [page, setPage] = useState(0);
  const [size] = useState(25);

  const params = useMemo(() => ({ q: q || undefined, page, size }), [q, page, size]);
  const { data, isLoading } = useDoctors(params);

  return (
    <div className="space-y-4 p-5">
      <header className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold">Doctors</h1>
          <p className="text-xs text-on-surface-variant">Manage clinical resources and availability.</p>
        </div>
        <button onClick={() => router.push("/doctors/new")} className="h-9 rounded-md bg-primary px-3 text-xs font-bold text-on-primary">Add Doctor</button>
      </header>

      <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Search doctor name, specialization, department" className="h-9 w-full rounded-md border border-outline-variant bg-surface px-3 text-sm" />

      {isLoading ? <div className="rounded-lg border border-outline-variant p-4 text-sm">Loading doctors...</div> : <DoctorTable items={data?.items || []} />}

      <div className="flex justify-end gap-2">
        <button disabled={page === 0} onClick={() => setPage((p) => Math.max(0, p - 1))} className="rounded border border-outline-variant px-2 py-1 text-xs disabled:opacity-40">Prev</button>
        <button onClick={() => setPage((p) => p + 1)} className="rounded border border-outline-variant px-2 py-1 text-xs">Next</button>
      </div>
    </div>
  );
}
