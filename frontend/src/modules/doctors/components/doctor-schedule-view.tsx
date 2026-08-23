"use client";

import { useState } from "react";
import Link from "next/link";
import { Calendar, Edit2, MapPin } from "lucide-react";
import { Doctor } from "../types/doctor.types";
import { useWeeklyAvailability } from "../hooks/use-scheduling";
import { WeeklyScheduleTab } from "./schedule/weekly-schedule-tab";
import { ExceptionsTab } from "./schedule/exceptions-tab";
import { AvailableSlotsTab } from "./schedule/available-slots-tab";

type Tab = "weekly" | "exceptions" | "slots";

export function DoctorScheduleView({ doctor }: { doctor: Doctor }) {
  const [activeTab, setActiveTab] = useState<Tab>("weekly");
  const doctorName = doctor.displayName || `${doctor.firstName} ${doctor.lastName}`;

  const { data: availability = [], isLoading: availabilityLoading } =
    useWeeklyAvailability(doctor.uuid);

  const tabs: { key: Tab; label: string }[] = [
    { key: "weekly", label: "Weekly Schedule" },
    { key: "exceptions", label: "Exceptions / Leave" },
    { key: "slots", label: "Available Slots" },
  ];

  return (
    <div className="space-y-6 p-4 md:p-6 bg-background min-h-screen text-on-surface">
      {/* Doctor Info Banner */}
      <section className="bg-surface-container-lowest border border-outline-variant rounded-xl p-5 shadow-xs flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div className="flex items-center gap-4">
          <div className="w-14 h-14 rounded-full bg-primary-container text-on-primary-container flex items-center justify-center font-bold text-xl shrink-0 shadow-xs">
            {doctor.firstName?.[0]}{doctor.lastName?.[0]}
          </div>
          <div>
            <div className="flex items-center gap-3 flex-wrap">
              <h1 className="text-2xl font-bold text-on-surface">{doctorName}</h1>
              <span className="bg-primary-container text-on-primary-container text-[11px] font-bold uppercase tracking-wider px-2.5 py-0.5 rounded-full">
                {doctor.status || "ACTIVE"}
              </span>
            </div>
            {doctor.specialization && (
              <p className="text-sm font-medium text-on-surface-variant mt-0.5">
                {doctor.specialization}
              </p>
            )}
            {doctor.department && (
              <p className="text-xs text-outline flex items-center gap-1 mt-1">
                <MapPin className="w-3.5 h-3.5" />
                {doctor.department}
              </p>
            )}
          </div>
        </div>

        <div className="flex items-center gap-3 w-full md:w-auto">
          <Link
            href={`/doctors/${doctor.uuid}/edit`}
            className="flex-1 md:flex-none px-4 py-2 bg-surface-container text-primary font-semibold text-xs rounded-lg border border-outline-variant hover:bg-surface-container-high transition-colors flex items-center justify-center gap-1.5"
          >
            <Edit2 className="w-3.5 h-3.5" /> Edit Doctor
          </Link>
          <Link
            href={`/doctors/${doctor.uuid}/appointments`}
            className="flex-1 md:flex-none px-4 py-2 bg-primary text-on-primary font-semibold text-xs rounded-lg hover:opacity-90 transition-colors flex items-center justify-center gap-1.5 shadow-xs"
          >
            <Calendar className="w-3.5 h-3.5" /> Appointments
          </Link>
        </div>
      </section>

      {/* Tabs */}
      <div className="border-b border-outline-variant flex gap-8">
        {tabs.map((t) => (
          <button
            key={t.key}
            onClick={() => setActiveTab(t.key)}
            className={`pb-3 text-sm font-bold transition-colors relative ${
              activeTab === t.key
                ? "text-primary border-b-2 border-primary"
                : "text-on-surface-variant hover:text-on-surface"
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>

      {/* Tab Content */}
      {activeTab === "weekly" && (
        <WeeklyScheduleTab
          doctorUuid={doctor.uuid}
          availability={availability}
          isLoading={availabilityLoading}
        />
      )}
      {activeTab === "exceptions" && (
        <ExceptionsTab doctorUuid={doctor.uuid} />
      )}
      {activeTab === "slots" && (
        <AvailableSlotsTab doctorUuid={doctor.uuid} />
      )}
    </div>
  );
}
