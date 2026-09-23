"use client";

import { useDoctor } from "../hooks/use-doctor";
import { DoctorHeader } from "../components/doctor-header";
import { DoctorProfileCard } from "../components/doctor-profile-card";
import { DoctorAvailabilityCard } from "../components/doctor-availability-card";

export default function DoctorDetailsPage({ uuid }: { uuid: string }) {
  const { data, isLoading } = useDoctor(uuid);

  if (isLoading) return <div className="p-5 text-sm">Loading doctor profile...</div>;
  if (!data) return <div className="p-5 text-sm">Doctor not found.</div>;

  return (
    <div className="space-y-4 p-5">
      <DoctorHeader doctor={data} />
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <DoctorProfileCard doctor={data} />
        <DoctorAvailabilityCard doctor={data} />
      </div>
    </div>
  );
}
