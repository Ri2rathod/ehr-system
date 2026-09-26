"use client";

import Link from "next/link";
import { Encounter } from "../types/encounter.types";
import { encounterTypeLabel, formatEncounterDateTime } from "../utils/encounter-format";
import { EncounterStatusChip } from "./encounter-status-chip";

export function EncounterTable({ items }: { items: Encounter[] }) {
  if (!items.length)
    return (
      <section className="rounded-lg border border-outline-variant bg-surface-container-lowest p-6">
        <p className="text-sm font-bold text-on-surface">No encounters found.</p>
        <p className="mt-1 text-xs text-on-surface-variant">
          Start a visit from an appointment that has been checked in.
        </p>
      </section>
    );
  return (
    <section className="overflow-hidden rounded-lg border border-outline-variant bg-surface-container-lowest">
      <div className="max-h-[58vh] overflow-auto">
        <table className="w-full min-w-[960px] border-collapse text-left text-xs">
          <thead className="sticky top-0 z-10 bg-surface-container-low">
            <tr className="border-b border-outline-variant text-[10px] uppercase tracking-wider text-on-surface-variant">
              <th className="px-3 py-2">Encounter</th>
              <th className="px-3 py-2">Patient</th>
              <th className="px-3 py-2">Doctor</th>
              <th className="px-3 py-2">Appointment</th>
              <th className="px-3 py-2">Type</th>
              <th className="px-3 py-2">Status</th>
              <th className="px-3 py-2">Started</th>
              <th className="px-3 py-2">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-outline-variant">
            {items.map((encounter) => (
              <tr key={encounter.uuid} className="hover:bg-surface">
                <td className="px-3 py-2 font-mono text-[11px] font-bold text-on-surface">
                  {encounter.encounterNumber}
                </td>
                <td className="px-3 py-2">
                  <p className="font-semibold text-on-surface">{encounter.patientName || "—"}</p>
                  <p className="font-mono text-[10px] text-on-surface-variant">
                    {encounter.patientMrn || "—"}
                  </p>
                </td>
                <td className="px-3 py-2">
                  <p className="font-semibold text-on-surface">{encounter.doctorName || "—"}</p>
                  <p className="font-mono text-[10px] text-on-surface-variant">
                    {encounter.doctorCode || "—"}
                  </p>
                </td>
                <td className="px-3 py-2 font-mono text-[11px] text-on-surface-variant">
                  {encounter.appointmentNumber || "—"}
                </td>
                <td className="px-3 py-2 text-on-surface-variant">
                  {encounterTypeLabel[encounter.encounterType] || encounter.encounterType}
                </td>
                <td className="px-3 py-2">
                  <EncounterStatusChip status={encounter.status} />
                </td>
                <td className="px-3 py-2 text-on-surface-variant">
                  {formatEncounterDateTime(encounter.startedAt)}
                </td>
                <td className="px-3 py-2">
                  <Link
                    href={`/encounters/${encounter.uuid}`}
                    className="rounded border border-outline-variant px-2 py-1 text-[10px] font-bold text-primary hover:bg-primary/5"
                  >
                    Open
                  </Link>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}
