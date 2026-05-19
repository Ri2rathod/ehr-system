import { Doctor } from "../types/doctor.types";

export function DoctorAvailabilityCard({ doctor }: { doctor: Doctor }) {
  return (
    <section className="rounded-lg border border-outline-variant bg-surface-container-lowest p-4">
      <h2 className="text-sm font-bold text-on-surface">Availability</h2>
      <div className="mt-3 space-y-1 text-sm">
        <p><span className="text-on-surface-variant">Available Days:</span> {doctor.availableDays?.join(", ") || "-"}</p>
        <p><span className="text-on-surface-variant">Consultation Hours:</span> {doctor.consultationHours || "-"}</p>
        <p><span className="text-on-surface-variant">Break:</span> {doctor.breakTimes || "-"}</p>
        <p><span className="text-on-surface-variant">Online Availability:</span> {doctor.onlineAvailable ? "Yes" : "No"}</p>
      </div>
    </section>
  );
}
