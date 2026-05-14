import { PatientStatus } from "../types/patient.types";

const statusClassMap: Record<PatientStatus, string> = {
  ACTIVE: "bg-green-100 text-green-800 border-green-200",
  INACTIVE: "bg-slate-100 text-slate-700 border-slate-200",
  ARCHIVED: "bg-amber-100 text-amber-800 border-amber-200",
  BLOCKED: "bg-red-100 text-red-800 border-red-200",
  DECEASED: "bg-rose-100 text-rose-900 border-rose-200",
};

export function PatientStatusBadge({ status }: { status: PatientStatus }) {
  return (
    <span
      className={`inline-flex items-center rounded px-2 py-0.5 text-[10px] font-bold uppercase tracking-wider border ${statusClassMap[status]}`}
    >
      {status}
    </span>
  );
}
