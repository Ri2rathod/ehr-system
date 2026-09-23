"use client";

import React, { useEffect, useId, useMemo, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { AlertCircle, Loader2, Search, Stethoscope, UserPlus, X } from "lucide-react";

import {
  DOCTOR_SEARCH_MIN_LENGTH,
  useDoctorSearch,
} from "@/modules/doctors/hooks/use-doctor-search";
import { Doctor } from "@/modules/doctors/types/doctor.types";

const DEBOUNCE_MS = 300;
const MAX_VISIBLE_RESULTS = 7;

const statusLabel: Record<string, string> = {
  ACTIVE: "Active",
  ON_LEAVE: "On leave",
  INACTIVE: "Inactive",
  SUSPENDED: "Suspended",
};

interface DoctorSearchProps {
  doctor: Doctor | null;
  onChange: (doctor: Doctor | null) => void;
}

function doctorName(doctor: Doctor) {
  return (
    doctor.displayName ||
    `${doctor.firstName ?? ""} ${doctor.lastName ?? ""}`.trim()
  );
}

function initialsOf(doctor: Doctor) {
  const source = doctor.displayName
    ? doctor.displayName.replace(/^Dr\.?\s*/i, "")
    : `${doctor.firstName ?? ""} ${doctor.lastName ?? ""}`;
  const parts = source.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return "?";
  if (parts.length === 1) return parts[0][0].toUpperCase();
  return (parts[0][0] + parts[1][0]).toUpperCase();
}

function Highlight({ text, query }: { text: string; query: string }) {
  if (!text) return null;
  const q = query.trim();
  if (!q) return <>{text}</>;

  const idx = text.toLowerCase().indexOf(q.toLowerCase());
  if (idx < 0) return <>{text}</>;

  return (
    <>
      {text.slice(0, idx)}
      <mark className="rounded-[2px] bg-primary/15 px-px text-inherit">
        {text.slice(idx, idx + q.length)}
      </mark>
      {text.slice(idx + q.length)}
    </>
  );
}

function SkeletonRow() {
  return (
    <div className="flex animate-pulse items-center gap-3 px-3 py-2.5">
      <div className="h-8 w-8 shrink-0 rounded bg-surface-container" />
      <div className="min-w-0 flex-1 space-y-1.5">
        <div className="h-3 w-40 rounded bg-surface-container" />
        <div className="h-2.5 w-48 max-w-full rounded bg-surface-container" />
      </div>
    </div>
  );
}

function Kbd({ children }: { children: React.ReactNode }) {
  return (
    <kbd className="rounded border border-outline-variant bg-surface px-1 py-px font-mono text-[10px] font-semibold text-on-surface">
      {children}
    </kbd>
  );
}

export function DoctorSearch({ doctor, onChange }: DoctorSearchProps) {
  const router = useRouter();
  const listboxId = useId();
  const containerRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  const [query, setQuery] = useState("");
  const [debouncedQuery, setDebouncedQuery] = useState("");
  const [isDebouncing, setIsDebouncing] = useState(false);
  const [isOpen, setIsOpen] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [activeIndex, setActiveIndex] = useState(0);

  const rawQuery = query.trim();
  const search = useDoctorSearch(debouncedQuery.trim());

  const items = search.data?.items ?? [];
  const totalElements = search.data?.totalElements ?? items.length;
  const visible = items.slice(0, MAX_VISIBLE_RESULTS);
  const isFetching = search.isFetching || isDebouncing;
  const panelOpen =
    isOpen &&
    (isEditing || !doctor) &&
    rawQuery.length >= DOCTOR_SEARCH_MIN_LENGTH;

  useEffect(() => {
    if (query.trim() === debouncedQuery) {
      setIsDebouncing(false);
      return;
    }
    setIsDebouncing(true);
    const timer = setTimeout(() => {
      setDebouncedQuery(query.trim());
      setIsDebouncing(false);
    }, DEBOUNCE_MS);
    return () => clearTimeout(timer);
  }, [query, debouncedQuery]);

  useEffect(() => {
    setActiveIndex(0);
  }, [debouncedQuery]);

  useEffect(() => {
    if (doctor) setIsEditing(false);
  }, [doctor]);

  useEffect(() => {
    function handleMouseDown(event: MouseEvent) {
      if (
        containerRef.current &&
        !containerRef.current.contains(event.target as Node)
      ) {
        setIsOpen(false);
      }
    }
    document.addEventListener("mousedown", handleMouseDown);
    return () => document.removeEventListener("mousedown", handleMouseDown);
  }, []);

  useEffect(() => {
    function handleShortcut(event: KeyboardEvent) {
      if (
        (event.metaKey || event.ctrlKey) &&
        event.shiftKey &&
        event.key.toLowerCase() === "d"
      ) {
        event.preventDefault();
        if (doctor) {
          setIsEditing(true);
          setIsOpen(query.trim().length >= DOCTOR_SEARCH_MIN_LENGTH);
        } else {
          inputRef.current?.focus();
          if (query.trim().length >= DOCTOR_SEARCH_MIN_LENGTH) {
            setIsOpen(true);
          }
        }
      }
    }
    window.addEventListener("keydown", handleShortcut);
    return () => window.removeEventListener("keydown", handleShortcut);
  }, [doctor, query]);

  useEffect(() => {
    if (isEditing && doctor) {
      inputRef.current?.focus();
      inputRef.current?.select();
    }
  }, [isEditing, doctor]);

  const statusMessage = useMemo(() => {
    if (!panelOpen) return "";
    if (isFetching) return "Searching doctors";
    if (search.isError) return "Unable to search doctors";
    if (items.length === 0) return "No doctors found";
    return `${items.length} doctor${items.length === 1 ? "" : "s"} found`;
  }, [panelOpen, isFetching, search.isError, items.length]);

  function handleQueryChange(value: string) {
    setQuery(value);
    setActiveIndex(0);
    setIsOpen(value.trim().length >= DOCTOR_SEARCH_MIN_LENGTH);
  }

  function selectResult(result: Doctor) {
    onChange(result);
    setIsEditing(false);
    setIsOpen(false);
  }

  function clearSearch() {
    setQuery("");
    setDebouncedQuery("");
    setIsDebouncing(false);
    setActiveIndex(0);
    inputRef.current?.focus();
  }

  function startEditing() {
    setIsEditing(true);
    setIsOpen(query.trim().length >= DOCTOR_SEARCH_MIN_LENGTH);
  }

  function handleKeyDown(event: React.KeyboardEvent<HTMLInputElement>) {
    if (event.key === "ArrowDown") {
      event.preventDefault();
      if (!panelOpen) {
        if (
          (isEditing || !doctor) &&
          rawQuery.length >= DOCTOR_SEARCH_MIN_LENGTH
        ) {
          setIsOpen(true);
        }
        return;
      }
      if (!isFetching && visible.length > 0) {
        setActiveIndex((index) => (index + 1) % visible.length);
      }
      return;
    }

    if (event.key === "ArrowUp") {
      event.preventDefault();
      if (panelOpen && !isFetching && visible.length > 0) {
        setActiveIndex(
          (index) => (index - 1 + visible.length) % visible.length
        );
      }
      return;
    }

    if (event.key === "Enter") {
      if (panelOpen) {
        event.preventDefault();
        if (!isFetching && !search.isError && visible[activeIndex]) {
          selectResult(visible[activeIndex]);
        }
      }
      return;
    }

    if (event.key === "Escape") {
      if (panelOpen) {
        event.preventDefault();
        setIsOpen(false);
      }
    }
  }

  if (doctor && !isEditing) {
    return (
      <div className="space-y-3">
        <div className="flex flex-col gap-3 rounded-lg border border-primary/40 bg-primary/5 p-4 sm:flex-row sm:items-start sm:justify-between">
          <div className="flex min-w-0 items-start gap-3">
            <span
              aria-hidden
              className="flex h-11 w-11 shrink-0 items-center justify-center rounded bg-primary font-mono text-xs font-bold text-on-primary"
            >
              {initialsOf(doctor)}
            </span>
            <div className="min-w-0 space-y-1">
              <div className="flex flex-wrap items-center gap-2">
                <h3 className="text-sm font-bold text-on-surface">
                  {doctorName(doctor)}
                </h3>
                <span className="inline-flex items-center gap-1 rounded border border-primary/30 bg-surface px-1.5 py-0.5 text-[10px] font-bold uppercase tracking-wide text-primary">
                  Selected
                </span>
                <span className="rounded bg-surface-container px-1.5 py-0.5 text-[10px] font-semibold text-on-surface-variant">
                  {statusLabel[doctor.status] ?? doctor.status}
                </span>
              </div>
              <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-on-surface-variant">
                <span className="rounded border border-outline-variant bg-surface px-1.5 py-0.5 text-[11px] font-bold text-on-surface">
                  {doctor.specialization || "General"}
                </span>
                {doctor.department && <span>{doctor.department}</span>}
                {doctor.licenseNumber && (
                  <span className="font-mono text-[11px]">
                    {doctor.licenseNumber}
                  </span>
                )}
                {typeof doctor.experienceYears === "number" && (
                  <span>{doctor.experienceYears}y exp</span>
                )}
              </div>
            </div>
          </div>

          <button
            type="button"
            onClick={startEditing}
            className="h-8 shrink-0 self-start rounded-md border border-outline-variant bg-surface px-3 text-xs font-bold text-on-surface hover:bg-surface-container focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
          >
            Change
          </button>
        </div>

        <p className="text-[11px] text-on-surface-variant">
          Doctor assigned for this booking. Continue to date &amp; slot
          selection.
          <span className="ml-2 font-mono text-[10px] text-outline">
            Ctrl/Cmd + Shift + D to change
          </span>
        </p>
      </div>
    );
  }

  const showEmptyHelper = query.trim().length === 0;
  const showNoResults =
    panelOpen &&
    !isFetching &&
    !search.isError &&
    search.isSuccess &&
    items.length === 0;
  const showResults =
    panelOpen && !search.isError && search.isSuccess && items.length > 0;
  const showSkeletons =
    panelOpen && !search.isError && isFetching && !search.data;

  return (
    <div ref={containerRef} className="relative">
      <div className="mb-1.5 flex items-baseline justify-between gap-2">
        <label
          htmlFor="doctor-search-input"
          className="text-xs font-bold text-on-surface-variant"
        >
          Search by name or specialization
        </label>
        <span className="hidden text-[10px] text-outline sm:inline">
          One field · name or specialty
        </span>
      </div>

      <div className="relative">
        <span className="pointer-events-none absolute inset-y-0 left-0 flex w-11 items-center justify-center text-on-surface-variant">
          <Search className="h-4 w-4" aria-hidden />
        </span>

        <input
          ref={inputRef}
          id="doctor-search-input"
          type="text"
          role="combobox"
          aria-expanded={panelOpen}
          aria-controls={listboxId}
          aria-autocomplete="list"
          aria-activedescendant={
            panelOpen && visible[activeIndex]
              ? `${listboxId}-option-${activeIndex}`
              : undefined
          }
          autoComplete="off"
          spellCheck={false}
          value={query}
          onChange={(event) => handleQueryChange(event.target.value)}
          onKeyDown={handleKeyDown}
          onFocus={() => {
            if (
              (isEditing || !doctor) &&
              query.trim().length >= DOCTOR_SEARCH_MIN_LENGTH
            ) {
              setIsOpen(true);
            }
          }}
          placeholder="Search by name or specialty"
          className="h-12 w-full rounded-md border border-outline-variant bg-surface pl-11 pr-24 text-sm font-semibold text-on-surface outline-none placeholder:font-normal placeholder:text-on-surface-variant/70 focus:border-primary focus:ring-1 focus:ring-primary"
        />

        <div className="absolute inset-y-0 right-0 flex items-center gap-1 pr-2">
          {isFetching && (
            <Loader2
              className="h-4 w-4 animate-spin text-primary"
              aria-hidden
            />
          )}

          {query.length > 0 && (
            <button
              type="button"
              onClick={clearSearch}
              aria-label="Clear search"
              className="rounded p-1 text-on-surface-variant hover:bg-surface-container hover:text-on-surface focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
            >
              <X className="h-4 w-4" aria-hidden />
            </button>
          )}
        </div>
      </div>

      <span role="status" aria-live="polite" className="sr-only">
        {statusMessage}
      </span>

      {showEmptyHelper && (
        <div className="mt-2 rounded-md border border-dashed border-outline-variant bg-surface-container-low px-3 py-2.5">
          <p className="text-xs font-bold text-on-surface">Search doctors</p>
          <p className="mt-0.5 text-xs text-on-surface-variant">
            Find an attending physician by name or specialty (e.g.{" "}
            <span className="font-mono">Cardiology</span>).
          </p>
          <div className="mt-2 flex flex-wrap items-center gap-2 text-[11px] text-on-surface-variant">
            <span className="flex items-center gap-1">
              <Kbd>↑</Kbd>
              <Kbd>↓</Kbd> Navigate
            </span>
            <span className="flex items-center gap-1">
              <Kbd>Enter</Kbd> Select
            </span>
            <span className="flex items-center gap-1">
              <Kbd>Esc</Kbd> Close
            </span>
            <span className="flex items-center gap-1">
              <Kbd>Ctrl</Kbd>
              <Kbd>Shift</Kbd>
              <Kbd>D</Kbd> Focus
            </span>
          </div>
        </div>
      )}

      {panelOpen && (
        <div className="absolute left-0 right-0 top-full z-30 mt-1 overflow-hidden rounded-md border border-outline-variant bg-surface-container-lowest shadow-lg">
          <div className="flex items-center justify-between border-b border-outline-variant bg-surface-container-low px-3 py-1.5 text-[11px] text-on-surface-variant">
            <span className="font-semibold">
              {isFetching
                ? "Searching doctors…"
                : search.isError
                  ? "Search failed"
                  : items.length === 0
                    ? "No matches"
                    : `Found ${totalElements} match${totalElements === 1 ? "" : "es"}`}
            </span>
            <span className="hidden items-center gap-2 font-mono text-[10px] sm:flex">
              <span className="flex items-center gap-1">
                <Kbd>↑</Kbd>
                <Kbd>↓</Kbd> Navigate
              </span>
              <span className="flex items-center gap-1">
                <Kbd>Enter</Kbd> Select
              </span>
            </span>
          </div>

          {search.isError && (
            <div
              role="alert"
              className="border-l-4 border-error bg-error-container/40 p-4"
            >
              <div className="flex items-start gap-2.5">
                <AlertCircle
                  className="mt-0.5 h-4 w-4 shrink-0 text-error"
                  aria-hidden
                />
                <div className="min-w-0 flex-1">
                  <p className="text-xs font-bold text-on-error-container">
                    Unable to search doctors
                  </p>
                  <p className="mt-0.5 text-xs text-on-error-container/90">
                    Something went wrong while searching. Please try again.
                  </p>
                  <button
                    type="button"
                    onClick={() => search.refetch()}
                    className="mt-2.5 h-8 rounded-md bg-error px-3 text-xs font-bold text-on-error hover:opacity-90 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-error"
                  >
                    Retry
                  </button>
                </div>
              </div>
            </div>
          )}

          {!search.isError && showSkeletons && (
            <div aria-hidden>
              <SkeletonRow />
              <div className="border-t border-outline-variant/50" />
              <SkeletonRow />
              <div className="border-t border-outline-variant/50" />
              <SkeletonRow />
            </div>
          )}

          {showNoResults && (
            <div className="px-4 py-5 text-center">
              <p className="text-sm font-bold text-on-surface">
                No doctors found
              </p>
              <p className="mx-auto mt-1 max-w-sm text-xs text-on-surface-variant">
                Try searching with a different name or specialization.
              </p>
              <div className="mt-3 flex flex-wrap items-center justify-center gap-2">
                <button
                  type="button"
                  onClick={clearSearch}
                  className="h-8 rounded-md border border-outline-variant px-3 text-xs font-semibold text-on-surface hover:bg-surface-container"
                >
                  Clear search
                </button>
                <button
                  type="button"
                  onClick={() => router.push("/doctors/new")}
                  className="inline-flex h-8 items-center gap-1.5 rounded-md border border-outline-variant px-3 text-xs font-semibold text-on-surface-variant hover:bg-surface-container"
                >
                  <UserPlus className="h-3.5 w-3.5" aria-hidden />
                  Register doctor
                </button>
              </div>
            </div>
          )}

          {showResults && (
            <div
              id={listboxId}
              role="listbox"
              aria-label="Doctor search results"
              className="max-h-[380px] divide-y divide-outline-variant/50 overflow-y-auto"
            >
              {visible.map((result, index) => {
                const isActive = index === activeIndex;

                return (
                  <div
                    key={result.uuid}
                    id={`${listboxId}-option-${index}`}
                    role="option"
                    aria-selected={isActive}
                    tabIndex={-1}
                    onMouseEnter={() => setActiveIndex(index)}
                    onMouseDown={(event) => {
                      event.preventDefault();
                      selectResult(result);
                    }}
                    className={`flex cursor-pointer items-center gap-3 border-l-2 px-3 py-2.5 outline-none ${
                      isActive
                        ? "border-l-primary bg-primary/5"
                        : "border-l-transparent hover:bg-surface-container-low"
                    }`}
                  >
                    <span
                      aria-hidden
                      className="flex h-8 w-8 shrink-0 items-center justify-center rounded bg-secondary-container font-mono text-[11px] font-bold text-on-secondary-container"
                    >
                      {initialsOf(result)}
                    </span>

                    <div className="min-w-0 flex-1">
                      <div className="flex flex-wrap items-center gap-x-2 gap-y-0.5">
                        <span className="truncate text-sm font-bold text-on-surface">
                          <Highlight
                            text={doctorName(result)}
                            query={debouncedQuery}
                          />
                        </span>
                        <span className="rounded bg-surface-container px-1.5 py-px text-[10px] font-semibold text-on-surface-variant">
                          {statusLabel[result.status] ?? result.status}
                        </span>
                      </div>
                      <div className="mt-0.5 flex flex-wrap items-center gap-x-2 text-[11px] text-on-surface-variant">
                        <span className="rounded border border-outline-variant/70 bg-surface px-1 text-[11px] font-bold text-on-surface">
                          <Highlight
                            text={result.specialization || "General"}
                            query={debouncedQuery}
                          />
                        </span>
                        {result.department && (
                          <>
                            <span className="text-outline">·</span>
                            <span className="truncate">
                              <Highlight
                                text={result.department}
                                query={debouncedQuery}
                              />
                            </span>
                          </>
                        )}
                        {result.licenseNumber && (
                          <>
                            <span className="text-outline">·</span>
                            <span className="font-mono">
                              <Highlight
                                text={result.licenseNumber}
                                query={debouncedQuery}
                              />
                            </span>
                          </>
                        )}
                      </div>
                    </div>

                    <div className="hidden shrink-0 text-right text-[11px] text-on-surface-variant sm:block">
                      {typeof result.experienceYears === "number" && (
                        <div>
                          Exp:{" "}
                          <span className="text-on-surface">
                            {result.experienceYears}y
                          </span>
                        </div>
                      )}
                      {result.consultationHours && (
                        <div className="text-[10px] text-outline">
                          {result.consultationHours}
                        </div>
                      )}
                      {!result.consultationHours && result.qualification && (
                        <div className="text-[10px] text-outline">
                          {result.qualification}
                        </div>
                      )}
                    </div>

                    <span
                      className="shrink-0 text-on-surface-variant"
                      aria-hidden
                    >
                      <Stethoscope className="h-4 w-4" />
                    </span>
                  </div>
                );
              })}
            </div>
          )}

          {showResults && totalElements > visible.length && (
            <div className="flex items-center justify-between border-t border-outline-variant bg-surface-container-low px-3 py-2 text-[11px]">
              <span className="text-on-surface-variant">
                Showing {visible.length} of {totalElements}
              </span>
              <button
                type="button"
                onClick={() => router.push("/doctors")}
                className="inline-flex items-center gap-1 text-xs font-bold text-primary hover:underline"
              >
                View all doctors
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
