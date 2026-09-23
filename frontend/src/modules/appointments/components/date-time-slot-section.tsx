"use client";

import React, { useMemo, useRef, useState } from "react";
import {
  AlertCircle,
  CalendarDays,
  Check,
  ChevronLeft,
  ChevronRight,
  RefreshCw,
} from "lucide-react";

import { useAvailableSlots } from "../hooks/use-appointments";
import { AvailableSlot } from "../types/appointment.types";
import {
  formatAppointmentTime12,
  formatCompactDate,
  formatLongDate,
  shiftLocalDate,
  toLocalDate,
} from "../utils/appointment-format";

interface DateTimeSlotSectionProps {
  doctorUuid: string;
  date: string;
  onDateChange: (date: string) => void;
  slotStart: string;
  slotEnd: string;
  onSlotChange: (start: string, end: string) => void;
}

type SlotGroupLabel = "Morning" | "Afternoon" | "Evening";

function groupOf(startTime: string): SlotGroupLabel {
  const hour = Number(startTime.slice(0, 2));
  if (Number.isNaN(hour)) return "Morning";
  if (hour < 12) return "Morning";
  if (hour < 17) return "Afternoon";
  return "Evening";
}

function SlotSkeleton() {
  return (
    <div className="h-10 w-[88px] animate-pulse rounded-md bg-surface-container" aria-hidden />
  );
}

export function DateTimeSlotSection({
  doctorUuid,
  date,
  onDateChange,
  slotStart,
  slotEnd,
  onSlotChange,
}: DateTimeSlotSectionProps) {
  const dateInputRef = useRef<HTMLInputElement>(null);
  const [showUnavailable, setShowUnavailable] = useState(false);

  const today = toLocalDate(new Date());
  const slots = useAvailableSlots(doctorUuid || undefined, date || undefined);

  const slotList = slots.data?.slots ?? [];
  const availableCount = useMemo(
    () => slotList.filter((slot) => slot.available).length,
    [slotList]
  );
  const unavailableCount = slotList.length - availableCount;
  const durationMinutes = slots.data?.slotDurationMinutes;

  const grouped = useMemo(() => {
    const buckets: Record<SlotGroupLabel, AvailableSlot[]> = {
      Morning: [],
      Afternoon: [],
      Evening: [],
    };

    for (const slot of slotList) {
      if (!slot.available && !showUnavailable) continue;
      buckets[groupOf(slot.startTime)].push(slot);
    }

    return buckets;
  }, [slotList, showUnavailable]);

  const hasVisibleSlots = grouped.Morning.length + grouped.Afternoon.length + grouped.Evening.length > 0;
  const selectedSlot =
    slotStart && slotEnd
      ? slotList.find(
          (slot) => slot.startTime === slotStart && slot.endTime === slotEnd
        ) ?? { startTime: slotStart, endTime: slotEnd, available: true }
      : null;

  const isLoadingAvailability =
    Boolean(doctorUuid && date) && (slots.isLoading || slots.isPending);

  const stepDone = Boolean(doctorUuid && date && slotStart);

  function handleDateChange(next: string) {
    onDateChange(next);
    setShowUnavailable(false);
  }

  function stepDay(delta: number) {
    handleDateChange(shiftLocalDate(date || today, delta));
  }

  function openNativePicker() {
    if (!date) handleDateChange(today);
    const el = dateInputRef.current;
    if (!el) return;
    if (typeof el.showPicker === "function") {
      try {
        el.showPicker();
        return;
      } catch {
        /* fall through to focus */
      }
    }
    el.focus();
  }

  function renderGroup(label: SlotGroupLabel, groupSlots: AvailableSlot[]) {
    if (groupSlots.length === 0) return null;

    return (
      <div key={label} className="space-y-2">
        <h4 className="text-[10px] font-bold uppercase tracking-wider text-on-surface-variant">
          {label}
        </h4>
        <div className="flex flex-wrap gap-2">
          {groupSlots.map((slot) => {
            const isSelected = slotStart === slot.startTime && slotEnd === slot.endTime;
            const label12 = formatAppointmentTime12(slot.startTime);

            if (!slot.available) {
              return (
                <span
                  key={`${slot.startTime}-${slot.endTime}`}
                  className="inline-flex h-10 min-w-[88px] items-center justify-center gap-1.5 rounded-md border border-outline-variant/60 bg-surface-container px-3 text-xs font-medium text-outline"
                  aria-disabled="true"
                >
                  <span className="font-mono">{label12}</span>
                  <span className="text-[10px] uppercase tracking-wide">Booked</span>
                </span>
              );
            }

            return (
              <button
                type="button"
                key={`${slot.startTime}-${slot.endTime}`}
                aria-pressed={isSelected}
                aria-label={`${label12}${isSelected ? ", selected" : ""}`}
                onClick={() => onSlotChange(slot.startTime, slot.endTime)}
                className={`inline-flex h-10 min-w-[88px] items-center justify-center gap-1.5 rounded-md border px-3 text-xs font-semibold transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary ${
                  isSelected
                    ? "border-primary bg-primary text-on-primary"
                    : "border-outline-variant bg-surface text-on-surface hover:border-primary/50 hover:bg-primary/5"
                }`}
              >
                {isSelected && (
                  <Check className="h-3.5 w-3.5 shrink-0" aria-hidden />
                )}
                <span className="font-mono">{label12}</span>
              </button>
            );
          })}
        </div>
      </div>
    );
  }

  return (
    <section
      aria-labelledby="date-time-heading"
      className="rounded-lg border border-outline-variant bg-surface-container-lowest p-4"
    >
      {/* Section header */}
      <div className="mb-4 flex items-start justify-between gap-2 border-b border-outline-variant/60 pb-3">
        <div className="flex items-start gap-2.5">
          <span
            className={`mt-0.5 flex h-6 w-6 shrink-0 items-center justify-center rounded-full text-[11px] font-bold ${
              stepDone
                ? "bg-primary text-on-primary"
                : doctorUuid
                  ? "border border-primary bg-primary/10 text-primary"
                  : "border border-outline-variant bg-surface text-on-surface-variant"
            }`}
          >
            {stepDone ? <Check className="h-3.5 w-3.5" aria-hidden /> : 3}
          </span>
          <div>
            <h2
              id="date-time-heading"
              className="text-base font-bold leading-tight text-on-surface"
            >
              {stepDone ? "Date & Time" : "Date & Time"}
            </h2>
            <p className="mt-0.5 text-xs text-on-surface-variant">
              {stepDone
                ? "Time selected — continue to appointment details"
                : "Choose an available appointment time"}
            </p>
          </div>
        </div>
        {stepDone && (
          <span className="inline-flex shrink-0 items-center gap-1 rounded border border-primary/30 bg-primary/5 px-2 py-0.5 text-[10px] font-bold uppercase tracking-wide text-primary">
            <Check className="h-3 w-3" aria-hidden />
            Confirmed
          </span>
        )}
      </div>

      {!doctorUuid ? (
        <div className="rounded-md border border-dashed border-outline-variant bg-surface-container-low px-4 py-6 text-center">
          <p className="text-sm font-bold text-on-surface">
            Select a doctor first
          </p>
          <p className="mx-auto mt-1 max-w-sm text-xs text-on-surface-variant">
            Choose a date to view available appointment times.
          </p>
        </div>
      ) : (
        <div className="space-y-4">
          {/* Date selector */}
          <div className="space-y-1.5">
            <p className="text-xs font-bold text-on-surface-variant">
              Appointment date
            </p>

            <div className="flex flex-wrap items-center gap-2">
              <div className="inline-flex h-11 items-stretch rounded-md border border-outline-variant bg-surface">
                <button
                  type="button"
                  aria-label="Previous day"
                  disabled={Boolean(date && date <= today)}
                  onClick={() => stepDay(-1)}
                  className="flex w-10 items-center justify-center border-r border-outline-variant text-on-surface-variant hover:bg-surface-container hover:text-on-surface disabled:cursor-not-allowed disabled:opacity-40 disabled:hover:bg-transparent focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
                >
                  <ChevronLeft className="h-4 w-4" aria-hidden />
                </button>

                <div className="relative flex min-w-[180px] items-center justify-center gap-2 px-3 text-sm font-bold text-on-surface sm:min-w-[220px]">
                  <CalendarDays
                    className="h-4 w-4 shrink-0 text-primary"
                    aria-hidden
                  />
                  <span>{formatCompactDate(date)}</span>
                  <input
                    ref={dateInputRef}
                    type="date"
                    aria-label="Appointment date"
                    value={date}
                    min={today}
                    onChange={(event) => {
                      if (event.target.value) handleDateChange(event.target.value);
                    }}
                    className="absolute inset-0 h-full w-full cursor-pointer opacity-0"
                  />
                </div>

                <button
                  type="button"
                  aria-label="Next day"
                  onClick={() => stepDay(1)}
                  className="flex w-10 items-center justify-center border-l border-outline-variant text-on-surface-variant hover:bg-surface-container hover:text-on-surface focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
                >
                  <ChevronRight className="h-4 w-4" aria-hidden />
                </button>
              </div>

              <button
                type="button"
                onClick={openNativePicker}
                className="inline-flex h-11 items-center gap-1.5 rounded-md border border-outline-variant bg-surface px-3 text-xs font-semibold text-on-surface hover:bg-surface-container focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
              >
                <CalendarDays className="h-4 w-4" aria-hidden />
                Calendar
              </button>
            </div>

            {date && (
              <p className="text-xs text-on-surface-variant">
                <span className="font-semibold text-on-surface">
                  {formatLongDate(date)}
                </span>
                {!isLoadingAvailability &&
                  !slots.isError &&
                  slots.data &&
                  availableCount > 0 && (
                    <span className="ml-2 text-primary">
                      {availableCount} available slot
                      {availableCount === 1 ? "" : "s"}
                    </span>
                  )}
              </p>
            )}
          </div>

          {/* Availability */}
          <div className="rounded-md border border-outline-variant bg-surface-container-low/70 p-3.5">
            <div className="mb-3 flex flex-wrap items-baseline justify-between gap-x-3 gap-y-1">
              <div>
                <h3 className="text-sm font-bold text-on-surface">
                  Available times
                </h3>
                <p className="mt-0.5 text-xs text-on-surface-variant">
                  {date ? (
                    <>
                      {formatLongDate(date)}
                      {!isLoadingAvailability && !slots.isError && slots.data && (
                        <>
                          {" · "}
                          {availableCount} slot{availableCount === 1 ? "" : "s"}{" "}
                          available
                        </>
                      )}
                    </>
                  ) : (
                    "Select a date to load availability"
                  )}
                </p>
              </div>
            </div>

            {!date ? (
              <div className="rounded-md border border-dashed border-outline-variant bg-surface px-4 py-5 text-center">
                <p className="text-xs font-bold text-on-surface">
                  Choose a date
                </p>
                <p className="mt-1 text-xs text-on-surface-variant">
                  Pick an appointment date above to see open times.
                </p>
              </div>
            ) : slots.isError ? (
              <div
                role="alert"
                className="rounded-md border-l-4 border-error bg-error-container/40 p-3"
              >
                <div className="flex items-start gap-2.5">
                  <AlertCircle
                    className="mt-0.5 h-4 w-4 shrink-0 text-error"
                    aria-hidden
                  />
                  <div className="min-w-0 flex-1">
                    <p className="text-xs font-bold text-on-error-container">
                      Unable to load availability
                    </p>
                    <p className="mt-0.5 text-xs text-on-error-container/90">
                      We couldn&apos;t retrieve appointment times for this date.
                    </p>
                    <button
                      type="button"
                      onClick={() => slots.refetch()}
                      className="mt-2 inline-flex h-8 items-center gap-1.5 rounded-md bg-error px-3 text-xs font-bold text-on-error hover:opacity-90 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-error"
                    >
                      <RefreshCw className="h-3.5 w-3.5" aria-hidden />
                      Try again
                    </button>
                  </div>
                </div>
              </div>
            ) : isLoadingAvailability ? (
              <div className="space-y-3" aria-busy="true" aria-live="polite">
                <span className="sr-only">Loading available times</span>
                <div className="flex flex-wrap gap-2">
                  <SlotSkeleton />
                  <SlotSkeleton />
                  <SlotSkeleton />
                  <SlotSkeleton />
                </div>
                <div className="flex flex-wrap gap-2">
                  <SlotSkeleton />
                  <SlotSkeleton />
                  <SlotSkeleton />
                </div>
              </div>
            ) : slots.data && availableCount === 0 && !showUnavailable ? (
              <div className="rounded-md border border-dashed border-outline-variant bg-surface px-4 py-5 text-center">
                <p className="text-sm font-bold text-on-surface">
                  No available appointments
                </p>
                <p className="mx-auto mt-1 max-w-md text-xs text-on-surface-variant">
                  There are no available time slots for this doctor on this
                  date.
                </p>
                <div className="mt-3 flex flex-wrap items-center justify-center gap-2">
                  <button
                    type="button"
                    onClick={() => openNativePicker()}
                    className="h-8 rounded-md border border-outline-variant bg-surface px-3 text-xs font-semibold text-on-surface hover:bg-surface-container focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
                  >
                    Choose another date
                  </button>
                  <button
                    type="button"
                    onClick={() => stepDay(1)}
                    className="h-8 rounded-md bg-primary px-3 text-xs font-bold text-on-primary hover:opacity-90 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
                  >
                    View next available date
                  </button>
                </div>
                {unavailableCount > 0 && (
                  <button
                    type="button"
                    onClick={() => setShowUnavailable(true)}
                    className="mt-3 text-xs font-semibold text-primary hover:underline"
                  >
                    Show unavailable times ({unavailableCount})
                  </button>
                )}
              </div>
            ) : hasVisibleSlots ? (
              <>
                <div className="space-y-4">
                  {renderGroup("Morning", grouped.Morning)}
                  {renderGroup("Afternoon", grouped.Afternoon)}
                  {renderGroup("Evening", grouped.Evening)}
                </div>

                {unavailableCount > 0 && (
                  <div className="mt-4 border-t border-outline-variant/60 pt-3">
                    <button
                      type="button"
                      onClick={() => setShowUnavailable((value) => !value)}
                      aria-expanded={showUnavailable}
                      className="text-xs font-semibold text-primary hover:underline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
                    >
                      {showUnavailable
                        ? "Hide unavailable times"
                        : `Show unavailable times (${unavailableCount})`}
                    </button>
                    {!showUnavailable && (
                      <p className="mt-1 text-[11px] text-on-surface-variant">
                        Booked times stay hidden so open slots stay easy to scan.
                      </p>
                    )}
                  </div>
                )}
              </>
            ) : (
              <div className="rounded-md border border-dashed border-outline-variant bg-surface px-4 py-5 text-center">
                <p className="text-sm font-bold text-on-surface">
                  No available appointments
                </p>
                <p className="mx-auto mt-1 max-w-md text-xs text-on-surface-variant">
                  There are no available time slots for this doctor on this
                  date.
                </p>
                <div className="mt-3 flex flex-wrap items-center justify-center gap-2">
                  <button
                    type="button"
                    onClick={openNativePicker}
                    className="h-8 rounded-md border border-outline-variant bg-surface px-3 text-xs font-semibold text-on-surface hover:bg-surface-container"
                  >
                    Choose another date
                  </button>
                  <button
                    type="button"
                    onClick={() => stepDay(1)}
                    className="h-8 rounded-md bg-primary px-3 text-xs font-bold text-on-primary hover:opacity-90"
                  >
                    View next available date
                  </button>
                </div>
              </div>
            )}
          </div>

          {/* Selected confirmation */}
          {selectedSlot && date && (
            <div
              role="status"
              className="rounded-md border border-primary/40 bg-primary/5 px-3.5 py-3"
            >
              <p className="text-[10px] font-bold uppercase tracking-wider text-on-surface-variant">
                Selected appointment time
              </p>
              <div className="mt-1 flex flex-wrap items-baseline gap-x-3 gap-y-1">
                <p className="font-mono text-sm font-bold text-on-surface">
                  {formatAppointmentTime12(selectedSlot.startTime)} –{" "}
                  {formatAppointmentTime12(selectedSlot.endTime)}
                </p>
                {typeof durationMinutes === "number" && (
                  <span className="text-[11px] text-on-surface-variant">
                    {durationMinutes} min
                  </span>
                )}
              </div>
              <p className="mt-0.5 text-xs text-on-surface-variant">
                {formatLongDate(date)}
              </p>
            </div>
          )}
        </div>
      )}
    </section>
  );
}
