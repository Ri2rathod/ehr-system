"use client";

import Link from "next/link";
import { Doctor } from "../types/doctor.types";
import { DoctorStatusBadge } from "./doctor-status-badge";
import { DoctorActions } from "./doctor-actions";

export function DoctorTable({ items }: { items: Doctor[] }) {
  if (!items.length) {
    return <div className="rounded-lg border border-outline-variant bg-surface-container-lowest p-4 text-sm">No doctors found.</div>;
  }

  return (
    <section className="overflow-hidden rounded-lg border border-outline-variant bg-surface-container-lowest">
      <div className="max-h-[62vh] overflow-auto">
        <table className="w-full border-collapse text-left text-xs">
          <thead className="sticky top-0 bg-surface-container-low">
            <tr className="border-b border-outline-variant text-[10px] uppercase tracking-wider text-on-surface-variant">
              <th className="px-3 py-2">Avatar</th>
              <th className="px-3 py-2">Doctor Name</th>
              <th className="px-3 py-2">Specialization</th>
              <th className="px-3 py-2">Department</th>
              <th className="px-3 py-2">Phone</th>
              <th className="px-3 py-2">Email</th>
              <th className="px-3 py-2">Fee</th>
              <th className="px-3 py-2">Appointments Today</th>
              <th className="px-3 py-2">Status</th>
              <th className="px-3 py-2">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-outline-variant">
            {items.map((doctor) => {
              const name = doctor.displayName || `${doctor.firstName} ${doctor.lastName}`;
              const initials = `${doctor.firstName?.[0] || ""}${doctor.lastName?.[0] || ""}`.toUpperCase();
              return (
                <tr key={doctor.uuid} className="hover:bg-surface">
                  <td className="px-3 py-2">
                    <div className="flex h-7 w-7 items-center justify-center rounded-full bg-primary-container text-[10px] font-bold text-primary">{initials || "DR"}</div>
                  </td>
                  <td className="px-3 py-2 font-semibold"><Link href={`/doctors/${doctor.uuid}`} className="hover:underline">{name}</Link></td>
                  <td className="px-3 py-2">{doctor.specialization || "-"}</td>
                  <td className="px-3 py-2">{doctor.department || "-"}</td>
                  <td className="px-3 py-2">{doctor.phone || "-"}</td>
                  <td className="px-3 py-2">{doctor.email || "-"}</td>
                  <td className="px-3 py-2">{typeof doctor.consultationFee === "number" ? doctor.consultationFee : "-"}</td>
                  <td className="px-3 py-2">{doctor.appointmentsToday ?? "-"}</td>
                  <td className="px-3 py-2"><DoctorStatusBadge status={doctor.status} /></td>
                  <td className="px-3 py-2"><DoctorActions uuid={doctor.uuid} /></td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </section>
  );
}
