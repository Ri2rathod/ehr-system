import Link from "next/link";
import { Doctor } from "../types/doctor.types";

export function DoctorHeader({ doctor }: { doctor: Doctor }) {
  const name = doctor.displayName || `${doctor.firstName} ${doctor.lastName}`;

  return (
    <header className="rounded-lg border border-outline-variant bg-surface-container-lowest p-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-bold text-on-surface">{name}</h1>
          <p className="text-xs text-on-surface-variant">
            {doctor.specialization || "-"} • Department: {doctor.department || "-"}
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Link href={`/doctors/${doctor.uuid}/edit`} className="rounded border border-outline-variant px-3 py-2 text-xs font-semibold">Edit</Link>
          <Link href={`/doctors/${doctor.uuid}/schedule`} className="rounded border border-outline-variant px-3 py-2 text-xs font-semibold">View Schedule</Link>
          <Link href={`/doctors/${doctor.uuid}/appointments`} className="rounded bg-primary px-3 py-2 text-xs font-bold text-on-primary">Appointments</Link>
        </div>
      </div>
    </header>
  );
}
