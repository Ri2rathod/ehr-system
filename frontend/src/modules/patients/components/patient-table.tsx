"use client";

import { useRouter } from "next/navigation";
import Link from "next/link";
import { Patient } from "../types/patient.types";
import { PatientStatusBadge } from "./patient-status-badge";

function formatDate(value?: string) {
  if (!value) return "-";
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return "-";
  return new Intl.DateTimeFormat("en-GB", {
    day: "2-digit",
    month: "short",
    year: "numeric",
  }).format(d);
}

function formatBloodGroup(value?: string) {
  if (!value) return "-";
  const map: Record<string, string> = {
    A_POSITIVE: "A+",
    A_NEGATIVE: "A-",
    B_POSITIVE: "B+",
    B_NEGATIVE: "B-",
    AB_POSITIVE: "AB+",
    AB_NEGATIVE: "AB-",
    O_POSITIVE: "O+",
    O_NEGATIVE: "O-",
    UNKNOWN: "Unknown",
  };
  return map[value] || value;
}

function getAge(value?: string) {
  if (!value) return "-";
  const dob = new Date(value);
  if (Number.isNaN(dob.getTime())) return "-";
  const now = new Date();
  let age = now.getFullYear() - dob.getFullYear();
  const m = now.getMonth() - dob.getMonth();
  if (m < 0 || (m === 0 && now.getDate() < dob.getDate())) {
    age -= 1;
  }
  return `${age}y`;
}

export function PatientTable({ items }: { items: Patient[] }) {
  const router = useRouter();

  if (!items.length) {
    return (
      <section className="rounded-lg border border-outline-variant bg-surface-container-lowest p-6">
        <p className="text-sm font-bold text-on-surface">No patients found</p>
        <p className="mt-2 text-xs text-on-surface-variant">Try changing filters, searching by MRN, or clearing archived filter.</p>
      </section>
    );
  }

  return (
    <section className="overflow-hidden rounded-lg border border-outline-variant bg-surface-container-lowest">
      <div className="max-h-[62vh] overflow-auto">
        <table className="w-full border-collapse text-left text-xs">
          <thead className="sticky top-0 z-10 bg-surface-container-low">
            <tr className="border-b border-outline-variant text-[10px] uppercase tracking-wider text-on-surface-variant">
              <th className="px-3 py-2">Avatar</th>
              <th className="px-3 py-2">MRN</th>
              <th className="px-3 py-2">Patient Name</th>
              <th className="px-3 py-2">Gender / Age</th>
              <th className="px-3 py-2">Contact</th>
              <th className="px-3 py-2">Blood</th>
              <th className="px-3 py-2">Status</th>
              <th className="px-3 py-2">Registered</th>
              <th className="px-3 py-2">Tenant</th>
              <th className="px-3 py-2">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-outline-variant">
            {items.map((patient) => {
              const patientUuid = patient.patientUuid || patient.uuid || patient.id;
              const fullName = `${patient.firstName} ${patient.lastName}`.trim();
              const initials = `${patient.firstName?.[0] || ""}${patient.lastName?.[0] || ""}`.toUpperCase();
              const isDeceased = patient.status === "DECEASED" || patient.isDeceased;

              return (
                <tr
                  key={patient.id}
                  onDoubleClick={() => router.push(`/patients/${patientUuid}`)}
                  className={`cursor-pointer hover:bg-surface ${isDeceased ? "bg-rose-50/30" : ""}`}
                  title={isDeceased && patient.deceasedAt ? `Deceased: ${formatDate(patient.deceasedAt)}` : undefined}
                >
                  <td className="px-3 py-2">
                    <div className="flex h-7 w-7 items-center justify-center rounded-full bg-primary-container text-[10px] font-bold text-primary">
                      {initials || "--"}
                    </div>
                  </td>
                  <td className="px-3 py-2 font-bold text-on-surface">
                    <Link href={`/patients/${patientUuid}`} className="hover:underline">
                      {patient.mrn || "-"}
                    </Link>
                  </td>
                  <td className="px-3 py-2">
                    <Link href={`/patients/${patientUuid}`} className="font-semibold text-on-surface hover:underline">
                      {fullName || "-"}
                    </Link>
                    <p className="text-[10px] text-on-surface-variant">DOB: {formatDate(patient.dateOfBirth)}</p>
                  </td>
                  <td className="px-3 py-2 text-on-surface-variant">
                    {(patient.gender || "UNKNOWN").toLowerCase()} / {getAge(patient.dateOfBirth)}
                  </td>
                  <td className="px-3 py-2 text-on-surface-variant">{patient.phone || patient.email || "-"}</td>
                  <td className="px-3 py-2 font-semibold text-on-surface">{formatBloodGroup(patient.bloodGroup)}</td>
                  <td className="px-3 py-2">
                    <PatientStatusBadge status={patient.status} />
                  </td>
                  <td className="px-3 py-2 text-on-surface-variant">{formatDate(patient.registeredAt)}</td>
                  <td className="px-3 py-2 text-on-surface-variant">{patient.tenantName || "-"}</td>
                  <td className="px-3 py-2">
                    <div className="flex items-center gap-1">
                      <Link href={`/patients/${patientUuid}`} className="rounded border border-outline-variant px-2 py-1 text-[10px] font-bold text-primary hover:bg-primary/5">
                        Open Patient Chart
                      </Link>
                      <Link href={`/patients/${patientUuid}/edit`} className="rounded border border-outline-variant px-2 py-1 text-[10px]">
                        Edit Patient
                      </Link>
                    </div>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </section>
  );
}
