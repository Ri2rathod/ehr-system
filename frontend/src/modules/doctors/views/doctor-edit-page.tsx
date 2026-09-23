"use client";

import { DoctorForm } from "../forms/doctor-form";
import { useDoctor } from "../hooks/use-doctor";

export default function DoctorEditPage({ uuid }: { uuid: string }) {
  const { data, isLoading } = useDoctor(uuid);

  if (isLoading) return <div className="p-5 text-sm">Loading doctor...</div>;
  if (!data) return <div className="p-5 text-sm">Doctor not found.</div>;

  return (
    <div className="mx-auto w-full max-w-[1400px] space-y-4 p-5">
      <h1 className="text-xl font-bold">Edit Doctor</h1>
      <DoctorForm uuid={uuid} initial={data} />
    </div>
  );
}
