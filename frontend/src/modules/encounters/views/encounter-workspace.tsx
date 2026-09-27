"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { EncounterStatusChip } from "../components/encounter-status-chip";
import { VitalsSection } from "../components/vitals-section";
import { DiagnosisSection } from "@/modules/diagnoses/components/diagnosis-section";
import { TreatmentPlanSection } from "@/modules/treatment-plans/components/treatment-plan-section";
import {
  useCancelEncounter,
  useCompleteEncounter,
  useEncounter,
  useUpdateEncounter,
} from "../hooks/use-encounters";
import { Encounter } from "../types/encounter.types";
import { ageFrom, encounterTypeLabel, formatEncounterDateTime } from "../utils/encounter-format";

interface ClinicalForm {
  chiefComplaint: string;
  historyOfPresentIllness: string;
  clinicalNotes: string;
  assessment: string;
  treatmentPlan: string;
  followUpNotes: string;
}

function toForm(encounter: Encounter): ClinicalForm {
  return {
    chiefComplaint: encounter.chiefComplaint || "",
    historyOfPresentIllness: encounter.historyOfPresentIllness || "",
    clinicalNotes: encounter.clinicalNotes || "",
    assessment: encounter.assessment || "",
    treatmentPlan: encounter.treatmentPlan || "",
    followUpNotes: encounter.followUpNotes || "",
  };
}

function describeError(caught: unknown): { message: string; conflict: boolean } {
  const response = (caught as { response?: { status?: number; data?: { message?: string } } })?.response;
  const status = response?.status;
  const message = String(response?.data?.message || "");
  if (status === 403) return { message: "You do not have permission to perform this action.", conflict: false };
  if (status === 409)
    return {
      message: message || "Encounter was modified by another user. Reload and try again.",
      conflict: !message.toLowerCase().includes("already exists"),
    };
  if (status === 400 && message) return { message, conflict: false };
  return { message: "Something went wrong. Please try again.", conflict: false };
}

export function EncounterWorkspace({ uuid }: { uuid: string }) {
  const { data: encounter, isLoading, isError, refetch } = useEncounter(uuid);
  const updateEncounter = useUpdateEncounter();
  const completeEncounter = useCompleteEncounter();
  const cancelEncounter = useCancelEncounter();
  const [form, setForm] = useState<ClinicalForm | null>(null);
  const [baseline, setBaseline] = useState<ClinicalForm | null>(null);
  const [error, setError] = useState("");
  const [conflict, setConflict] = useState(false);
  const [notice, setNotice] = useState("");
  const [cancelOpen, setCancelOpen] = useState(false);
  const [cancelReason, setCancelReason] = useState("");

  const encounterVersion = encounter ? `${encounter.uuid}:${encounter.updatedAt || ""}` : "";
  useEffect(() => {
    if (!encounter) return;
    const loaded = toForm(encounter);
    setForm(loaded);
    setBaseline(loaded);
    setError("");
    setConflict(false);
    // Reset local draft whenever the server version of this encounter changes (load + saves).
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [encounterVersion]);

  const dirty = Boolean(form && baseline && JSON.stringify(form) !== JSON.stringify(baseline));

  useEffect(() => {
    if (!dirty) return;
    const handler = (event: BeforeUnloadEvent) => {
      event.preventDefault();
      event.returnValue = "";
    };
    window.addEventListener("beforeunload", handler);
    return () => window.removeEventListener("beforeunload", handler);
  }, [dirty]);

  if (isLoading || !encounter || !form) {
    if (isError) {
      return (
        <div className="space-y-3 p-5">
          <Link href="/encounters" className="text-xs font-semibold text-primary hover:underline">
            ← All encounters
          </Link>
          <div className="rounded-lg border border-error/30 bg-error-container/30 p-4 text-sm text-on-error-container">
            Encounter not found or you do not have permission to view it.
          </div>
        </div>
      );
    }
    return (
      <div className="space-y-3 p-5">
        <div className="h-8 w-48 animate-pulse rounded bg-surface-container" />
        <div className="h-56 animate-pulse rounded-lg bg-surface-container" />
      </div>
    );
  }

  const editable = encounter.status === "DRAFT" || encounter.status === "IN_PROGRESS";
  const saving = updateEncounter.isPending || completeEncounter.isPending;

  const setField = (key: keyof ClinicalForm, value: string) => {
    setForm((prev) => (prev ? { ...prev, [key]: value } : prev));
    setNotice("");
    setConflict(false);
  };

  const handleError = (caught: unknown) => {
    const described = describeError(caught);
    setError(described.message);
    setConflict(described.conflict);
  };

  const handleSave = async () => {
    setError("");
    setConflict(false);
    setNotice("");
    try {
      await updateEncounter.mutateAsync({ uuid, payload: { ...form, version: encounter.version } });
      setNotice("Draft saved.");
    } catch (caught) {
      handleError(caught);
    }
  };

  const handleComplete = async () => {
    setError("");
    setConflict(false);
    setNotice("");
    if (!form.chiefComplaint.trim()) {
      setError("Chief complaint is required to complete the encounter.");
      return;
    }
    try {
      await completeEncounter.mutateAsync({ uuid, payload: { ...form, version: encounter.version } });
      setNotice("Encounter completed.");
    } catch (caught) {
      handleError(caught);
    }
  };

  const handleCancelEncounter = async () => {
    setError("");
    setConflict(false);
    setNotice("");
    try {
      await cancelEncounter.mutateAsync({ uuid, reason: cancelReason.trim() });
      setCancelOpen(false);
      setCancelReason("");
    } catch (caught) {
      handleError(caught);
    }
  };

  const handleReload = async () => {
    await refetch();
    setConflict(false);
    setError("");
  };

  const age = ageFrom(encounter.patientDateOfBirth);

  return (
    <div className="space-y-4 p-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex flex-wrap items-center gap-3">
          <Link href="/encounters" className="text-xs font-semibold text-primary hover:underline">
            ← All encounters
          </Link>
          <span className="font-mono text-lg font-bold text-on-surface">{encounter.encounterNumber}</span>
          <EncounterStatusChip status={encounter.status} />
          <span className="text-xs text-on-surface-variant">
            {encounterTypeLabel[encounter.encounterType] || encounter.encounterType}
          </span>
          {dirty && (
            <span className="rounded-full bg-amber-100 px-2 py-1 text-[10px] font-bold text-amber-900">
              Unsaved changes
            </span>
          )}
          {!dirty && encounter.updatedAt && (
            <span className="text-[10px] text-on-surface-variant">
              Last saved {formatEncounterDateTime(encounter.updatedAt)}
            </span>
          )}
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <Link
            href={`/appointments/${encounter.appointmentUuid}`}
            className="h-9 rounded-md border border-outline-variant px-3 text-xs font-bold text-primary hover:bg-primary/5"
          >
            Open appointment
          </Link>
          {editable && (
            <>
              <button
                onClick={handleSave}
                disabled={!dirty || saving}
                className="h-9 rounded-md border border-outline-variant px-3 text-xs font-bold text-primary disabled:opacity-50"
              >
                {updateEncounter.isPending ? "Saving…" : "Save Draft"}
              </button>
              <button
                onClick={handleComplete}
                disabled={saving}
                className="h-9 rounded-md bg-primary px-3 text-xs font-bold text-on-primary disabled:opacity-50"
              >
                {completeEncounter.isPending ? "Completing…" : "Complete Encounter"}
              </button>
              <button
                onClick={() => setCancelOpen(true)}
                disabled={saving}
                className="h-9 rounded-md border border-error px-3 text-xs font-bold text-error disabled:opacity-50"
              >
                Cancel Encounter
              </button>
            </>
          )}
          {encounter.status === "COMPLETED" && (
            <span className="rounded-full bg-emerald-100 px-3 py-1 text-[10px] font-bold text-emerald-900">
              Read-only • completed {formatEncounterDateTime(encounter.endedAt)}
            </span>
          )}
          {encounter.status === "CANCELLED" && (
            <span className="rounded-full bg-error-container px-3 py-1 text-[10px] font-bold text-on-error-container">
              Cancelled
            </span>
          )}
        </div>
      </div>

      {error && (
        <div
          role="alert"
          className="flex flex-wrap items-center justify-between gap-2 rounded-md border border-error/30 bg-error-container/30 p-3 text-sm text-on-error-container"
        >
          <span>{error}</span>
          {conflict && (
            <button
              onClick={handleReload}
              className="h-8 rounded border border-error/40 px-3 text-xs font-bold"
            >
              Reload encounter
            </button>
          )}
        </div>
      )}
      {notice && (
        <div className="rounded-md border border-emerald-200 bg-emerald-50 p-3 text-sm text-emerald-900">
          {notice}
        </div>
      )}
      {encounter.status === "CANCELLED" && encounter.cancellationReason && (
        <div className="rounded-md border border-error/30 bg-error-container/30 p-3 text-xs text-on-error-container">
          Cancellation reason: {encounter.cancellationReason}
        </div>
      )}

      <section className="grid gap-4 lg:grid-cols-3">
        <InfoSection title="Patient">
          <p className="font-semibold">{encounter.patientName || "—"}</p>
          <p className="mt-1 font-mono text-xs text-on-surface-variant">{encounter.patientMrn || "—"}</p>
          <p className="mt-1 text-xs text-on-surface-variant">
            {age != null ? `${age} yrs` : "—"} • {encounter.patientGender || "—"}
          </p>
        </InfoSection>
        <InfoSection title="Doctor">
          <p className="font-semibold">{encounter.doctorName || "—"}</p>
          <p className="mt-1 font-mono text-xs text-on-surface-variant">{encounter.doctorCode || "—"}</p>
        </InfoSection>
        <InfoSection title="Visit">
          <dl className="space-y-2 text-xs">
            <Row label="Appointment" value={encounter.appointmentNumber || "—"} />
            <Row label="Type" value={encounterTypeLabel[encounter.encounterType] || encounter.encounterType} />
            <Row label="Started" value={formatEncounterDateTime(encounter.startedAt)} />
            <Row label="Ended" value={formatEncounterDateTime(encounter.endedAt)} />
          </dl>
        </InfoSection>
      </section>

      <section className="grid gap-4 lg:grid-cols-2">
        <InfoSection title="Chief complaint">
          <textarea
            value={form.chiefComplaint}
            onChange={(event) => setField("chiefComplaint", event.target.value)}
            maxLength={500}
            rows={2}
            disabled={!editable}
            placeholder="Required to complete the encounter"
            className="block w-full rounded border border-outline-variant bg-surface p-2 text-sm disabled:opacity-70"
          />
        </InfoSection>
        <InfoSection title="History of present illness">
          <textarea
            value={form.historyOfPresentIllness}
            onChange={(event) => setField("historyOfPresentIllness", event.target.value)}
            maxLength={5000}
            rows={4}
            disabled={!editable}
            className="block w-full rounded border border-outline-variant bg-surface p-2 text-sm disabled:opacity-70"
          />
        </InfoSection>
      </section>

      <VitalsSection encounterUuid={uuid} editable={editable} />

      <section className="grid gap-4 lg:grid-cols-2">
        <InfoSection title="Clinical notes">
          <textarea
            value={form.clinicalNotes}
            onChange={(event) => setField("clinicalNotes", event.target.value)}
            maxLength={10000}
            rows={6}
            disabled={!editable}
            className="block w-full rounded border border-outline-variant bg-surface p-2 text-sm disabled:opacity-70"
          />
        </InfoSection>
        <InfoSection title="Assessment">
          <textarea
            value={form.assessment}
            onChange={(event) => setField("assessment", event.target.value)}
            maxLength={10000}
            rows={6}
            disabled={!editable}
            className="block w-full rounded border border-outline-variant bg-surface p-2 text-sm disabled:opacity-70"
          />
        </InfoSection>
      </section>

      <DiagnosisSection encounterUuid={uuid} editable={editable} />

      <TreatmentPlanSection encounterUuid={uuid} editable={editable} />

      <section className="grid gap-4 lg:grid-cols-2">
        <InfoSection title="Treatment plan">
          <textarea
            value={form.treatmentPlan}
            onChange={(event) => setField("treatmentPlan", event.target.value)}
            maxLength={10000}
            rows={6}
            disabled={!editable}
            className="block w-full rounded border border-outline-variant bg-surface p-2 text-sm disabled:opacity-70"
          />
        </InfoSection>
        <InfoSection title="Follow-up notes">
          <textarea
            value={form.followUpNotes}
            onChange={(event) => setField("followUpNotes", event.target.value)}
            maxLength={5000}
            rows={6}
            disabled={!editable}
            className="block w-full rounded border border-outline-variant bg-surface p-2 text-sm disabled:opacity-70"
          />
        </InfoSection>
      </section>

      <AlertDialog open={cancelOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Cancel encounter?</AlertDialogTitle>
            <AlertDialogDescription>
              {encounter.encounterNumber} will be cancelled. A reason is required for audit history.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <label className="mt-3 block text-xs font-semibold">
            Cancellation reason
            <textarea
              value={cancelReason}
              onChange={(event) => setCancelReason(event.target.value)}
              rows={3}
              className="mt-1 block w-full rounded border border-outline-variant bg-surface p-2"
            />
          </label>
          <AlertDialogFooter>
            <button
              onClick={() => setCancelOpen(false)}
              className="h-8 rounded-md border border-outline-variant px-3 text-xs font-semibold"
            >
              Keep Encounter
            </button>
            <button
              disabled={!cancelReason.trim() || cancelEncounter.isPending}
              onClick={handleCancelEncounter}
              className="h-8 rounded-md bg-error px-3 text-xs font-bold text-on-error disabled:opacity-50"
            >
              Cancel Encounter
            </button>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}

function InfoSection({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="rounded-lg border border-outline-variant bg-surface-container-lowest p-4">
      <h2 className="mb-3 text-xs font-bold uppercase tracking-wider text-on-surface-variant">
        {title}
      </h2>
      {children}
    </section>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between gap-3">
      <dt className="text-on-surface-variant">{label}</dt>
      <dd className="font-semibold text-on-surface">{value}</dd>
    </div>
  );
}
