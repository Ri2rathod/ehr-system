"use client";

import { useState } from "react";
import { Clock, CalendarDays } from "lucide-react";
import { useAvailableSlots } from "../../hooks/use-scheduling";

function todayString() {
  return new Date().toISOString().split("T")[0];
}

interface Props {
  doctorUuid: string;
}

export function AvailableSlotsTab({ doctorUuid }: Props) {
  const [date, setDate] = useState(todayString());
  const { data, isLoading, isError } = useAvailableSlots(doctorUuid, date);

  const slots = data?.slots ?? [];
  const available = slots.filter((s) => s.available).length;
  const booked = slots.filter((s) => !s.available).length;

  return (
    <div className="space-y-4">
      {/* Date picker */}
      <div className="bg-surface-container-lowest border border-outline-variant rounded-xl p-4 shadow-xs flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
        <div>
          <h3 className="font-bold text-sm text-on-surface">Available Appointment Slots</h3>
          <p className="text-xs text-on-surface-variant">Select a date to see patient appointment slots</p>
        </div>
        <input
          type="date"
          value={date}
          onChange={(e) => setDate(e.target.value)}
          className="rounded-lg border border-outline-variant bg-surface px-3 py-2 text-sm text-on-surface font-medium focus:outline-primary"
        />
      </div>

      {/* Stats strip */}
      {!isLoading && slots.length > 0 && (
        <div className="flex items-center gap-4 text-xs font-semibold">
          <span className="flex items-center gap-1.5">
            <span className="w-3 h-3 rounded-full bg-primary-container" />
            {booked} Booked
          </span>
          <span className="flex items-center gap-1.5">
            <span className="w-3 h-3 rounded-full bg-surface-container-high border border-outline-variant" />
            {available} Available
          </span>
          {data?.slotDurationMinutes && (
            <span className="text-outline">{data.slotDurationMinutes} min slots</span>
          )}
        </div>
      )}

      {/* Slots grid */}
      {isLoading ? (
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3">
          {[...Array(8)].map((_, i) => (
            <div key={i} className="h-20 rounded-lg bg-surface-container animate-pulse" />
          ))}
        </div>
      ) : isError ? (
        <div className="p-8 text-center bg-surface-container-lowest rounded-xl border border-outline-variant">
          <p className="text-sm text-error font-medium">Failed to load slots. Please try again.</p>
        </div>
      ) : slots.length === 0 ? (
        <div className="p-8 text-center bg-surface-container-lowest rounded-xl border border-outline-variant">
          <CalendarDays className="w-10 h-10 text-outline mx-auto mb-2" />
          <p className="text-sm text-on-surface-variant font-medium">No available slots for this date</p>
          <p className="text-xs text-outline mt-1">The doctor may not have schedule configured for this day</p>
        </div>
      ) : (
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3">
          {slots.map((slot, idx) => (
            <div
              key={idx}
              className={`p-3.5 rounded-lg border flex flex-col gap-2 transition-colors ${
                slot.available
                  ? "bg-surface-container-lowest border-outline-variant/60 hover:border-primary cursor-pointer"
                  : "bg-primary-container/10 border-primary-container/30"
              }`}
            >
              <div className="flex items-center gap-2">
                <Clock className="w-3.5 h-3.5 text-primary shrink-0" />
                <span className="font-bold text-sm text-on-surface">{slot.startTime}</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-[10px] text-outline font-medium">→ {slot.endTime}</span>
                <span className={`text-[10px] font-bold px-2 py-0.5 rounded ${
                  slot.available
                    ? "bg-surface-container text-on-surface-variant"
                    : "bg-primary-container text-on-primary-container"
                }`}>
                  {slot.available ? "FREE" : "BOOKED"}
                </span>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
