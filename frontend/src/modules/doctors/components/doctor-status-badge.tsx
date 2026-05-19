import { DoctorStatus } from "../types/doctor.types";

const cls: Record<DoctorStatus, string> = {
  ACTIVE: "bg-green-100 text-green-800 border-green-200",
  ON_LEAVE: "bg-amber-100 text-amber-800 border-amber-200",
  INACTIVE: "bg-slate-100 text-slate-700 border-slate-200",
  SUSPENDED: "bg-red-100 text-red-800 border-red-200",
};

export function DoctorStatusBadge({ status }: { status: DoctorStatus }) {
  return <span className={`inline-flex rounded border px-2 py-0.5 text-[10px] font-bold ${cls[status]}`}>{status}</span>;
}
