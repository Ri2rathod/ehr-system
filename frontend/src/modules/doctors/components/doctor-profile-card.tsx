import { Doctor } from "../types/doctor.types";

export function DoctorProfileCard({ doctor }: { doctor: Doctor }) {
  return (
    <section className="rounded-lg border border-outline-variant bg-surface-container-lowest p-4">
      <h2 className="text-sm font-bold text-on-surface">Basic Information</h2>
      <div className="mt-3 grid grid-cols-1 gap-2 text-sm md:grid-cols-2">
        <p><span className="text-on-surface-variant">Name:</span> {doctor.displayName || `${doctor.firstName} ${doctor.lastName}`}</p>
        <p><span className="text-on-surface-variant">Specialization:</span> {doctor.specialization || "-"}</p>
        <p><span className="text-on-surface-variant">Department:</span> {doctor.department || "-"}</p>
        <p><span className="text-on-surface-variant">License:</span> {doctor.licenseNumber || "-"}</p>
      </div>
    </section>
  );
}
