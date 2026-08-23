"use client";

import { use } from "react";
import { useDoctor } from "@/modules/doctors/hooks/use-doctor";
import { DoctorScheduleView } from "@/modules/doctors/components/doctor-schedule-view";

export default function DoctorSchedulePage({
  params,
}: {
  params: Promise<{ uuid: string }>;
}) {
  const { uuid } = use(params);
  const { data: doctor, isLoading } = useDoctor(uuid);

  if (isLoading) {
    return (
      <div className="flex h-64 w-full items-center justify-center">
        <p className="text-sm font-semibold text-on-surface-variant animate-pulse">
          Loading doctor schedule...
        </p>
      </div>
    );
  }

  const doctorData = doctor || {
    uuid,
    firstName: "Julian",
    lastName: "Miller",
    displayName: "Dr. Julian Miller",
    specialization: "Internal Medicine",
    department: "Internal Medicine Ward 4B",
    status: "ACTIVE" as const,
  };

  return <DoctorScheduleView doctor={doctorData} />;
}