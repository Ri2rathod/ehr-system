"use client";

import { useState } from "react";
import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { formatAppointmentDate } from "@/modules/appointments/utils/appointment-format";
import { useDeactivateDiagnosis, useDiagnoses } from "../hooks/use-diagnoses";
import { Diagnosis, DiagnosisStatus } from "../types/diagnosis.types";
import { DiagnosisDialog } from "./diagnosis-dialog";

interface DiagnosisSectionProps {
  encounterUuid: string;
  editable: boolean;
}

const statusStyles: Record<DiagnosisStatus, string> = {
  ACTIVE: "bg-emerald-100 text-emerald-900",
  RESOLVED: "bg-sky-100 text-sky-900",
  INACTIVE: "bg-surface-container-high text-on-surface-variant",
};

const statusLabels: Record<DiagnosisStatus, string> = {
  ACTIVE: "Active",
  RESOLVED: "Resolved",
  INACTIVE: "Inactive",
};

function describeError(caught: unknown): string {
  const response = (caught as { response?: { status?: number; data?: { message?: string } } })?.response;
  const message = String(response?.data?.message || "");
  if (response?.status === 403) return "You do not have permission to perform this action.";
  if (message) return message;
  return "Unable to deactivate the diagnosis.";
}

export function DiagnosisSection({ encounterUuid, editable }: DiagnosisSectionProps) {
  const { data: diagnoses, isLoading, isError, refetch } = useDiagnoses(encounterUuid);
  const deactivateDiagnosis = useDeactivateDiagnosis(encounterUuid);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<Diagnosis | null>(null);
  const [deactivateTarget, setDeactivateTarget] = useState<Diagnosis | null>(null);
  const [deactivateError, setDeactivateError] = useState("");

  const hasPrimary = Boolean(diagnoses?.some((item) => item.diagnosisType === "PRIMARY"));

  const openCreate = () => {
    setEditing(null);
    setDialogOpen(true);
  };

  const openEdit = (diagnosis: Diagnosis) => {
    setEditing(diagnosis);
    setDialogOpen(true);
  };

  const handleDeactivate = async () => {
    if (!deactivateTarget) return;
    setDeactivateError("");
    try {
      await deactivateDiagnosis.mutateAsync(deactivateTarget.uuid);
      setDeactivateTarget(null);
    } catch (caught) {
      setDeactivateError(describeError(caught));
    }
  };

  return (
    <section className="rounded-lg border border-outline-variant bg-surface-container-lowest p-4">
      <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
        <h2 className="text-xs font-bold uppercase tracking-wider text-on-surface-variant">
          Diagnoses
        </h2>
        {editable && !isLoading && !isError && (
          <button
            onClick={openCreate}
            className="h-8 rounded bg-primary px-3 text-xs font-bold text-on-primary"
          >
            + Add Diagnosis
          </button>
        )}
      </div>

      {isLoading ? (
        <div className="space-y-2">
          {[0, 1, 2].map((index) => (
            <div key={index} className="h-14 animate-pulse rounded bg-surface-container" />
          ))}
        </div>
      ) : isError ? (
        <div>
          <p className="text-sm font-bold text-on-surface">Unable to load diagnoses</p>
          <button
            onClick={() => refetch()}
            className="mt-2 h-8 rounded border border-outline-variant px-3 text-xs font-bold text-primary"
          >
            Try again
          </button>
        </div>
      ) : !diagnoses?.length ? (
        <div>
          <p className="text-sm font-bold text-on-surface">No diagnoses recorded</p>
          <p className="mt-1 text-xs text-on-surface-variant">
            Add the patient&apos;s primary or secondary diagnosis for this encounter.
          </p>
          {editable && (
            <button
              onClick={openCreate}
              className="mt-3 h-8 rounded-md bg-primary px-3 text-xs font-bold text-on-primary"
            >
              + Add Diagnosis
            </button>
          )}
        </div>
      ) : (
        <div>
          {diagnoses.map((diagnosis) => (
            <div
              key={diagnosis.uuid}
              className="flex items-start justify-between gap-3 border-b border-outline-variant py-3 last:border-b-0"
            >
              <div className="min-w-0 space-y-1">
                <div className="flex flex-wrap items-center gap-2">
                  <span
                    className={`rounded-full px-2 py-0.5 text-[10px] font-bold tracking-wide ${
                      diagnosis.diagnosisType === "PRIMARY"
                        ? "bg-primary-container text-on-primary-container"
                        : "bg-surface-container-high text-on-surface-variant"
                    }`}
                  >
                    {diagnosis.diagnosisType}
                  </span>
                  <p className="text-sm font-semibold text-on-surface">{diagnosis.name}</p>
                </div>
                {(diagnosis.code || diagnosis.codeSystem) && (
                  <p className="text-xs text-on-surface-variant">
                    {diagnosis.code && (
                      <span className="font-mono font-bold text-on-surface">{diagnosis.code}</span>
                    )}
                    {diagnosis.code && diagnosis.codeSystem && " · "}
                    {diagnosis.codeSystem}
                  </p>
                )}
                <p className="flex flex-wrap items-center gap-2 text-[11px] text-on-surface-variant">
                  <span
                    className={`rounded-full px-2 py-0.5 text-[10px] font-bold ${statusStyles[diagnosis.clinicalStatus]}`}
                  >
                    {statusLabels[diagnosis.clinicalStatus]}
                  </span>
                  {diagnosis.onsetDate && <span>Onset: {formatAppointmentDate(diagnosis.onsetDate)}</span>}
                  {diagnosis.resolvedDate && (
                    <span>Resolved: {formatAppointmentDate(diagnosis.resolvedDate)}</span>
                  )}
                </p>
              </div>
              {editable && (
                <div className="flex shrink-0 gap-1">
                  <button
                    onClick={() => openEdit(diagnosis)}
                    className="rounded border border-outline-variant px-2 py-1 text-[10px] font-bold text-primary hover:bg-primary/5"
                  >
                    Edit
                  </button>
                  <button
                    onClick={() => {
                      setDeactivateError("");
                      setDeactivateTarget(diagnosis);
                    }}
                    className="rounded border border-outline-variant px-2 py-1 text-[10px] font-bold text-error hover:bg-error/5"
                  >
                    Deactivate
                  </button>
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      <DiagnosisDialog
        open={dialogOpen}
        encounterUuid={encounterUuid}
        diagnosis={editing}
        hasPrimary={hasPrimary}
        onClose={() => setDialogOpen(false)}
      />

      <AlertDialog open={Boolean(deactivateTarget)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Deactivate diagnosis?</AlertDialogTitle>
            <AlertDialogDescription>
              This diagnosis will no longer be active in this encounter.
            </AlertDialogDescription>
          </AlertDialogHeader>
          {deactivateError && (
            <p role="alert" className="mt-3 text-xs font-semibold text-error">
              {deactivateError}
            </p>
          )}
          <AlertDialogFooter>
            <button
              onClick={() => setDeactivateTarget(null)}
              className="h-8 rounded-md border border-outline-variant px-3 text-xs font-semibold"
            >
              Cancel
            </button>
            <button
              disabled={deactivateDiagnosis.isPending}
              onClick={handleDeactivate}
              className="h-8 rounded-md bg-error px-3 text-xs font-bold text-on-error disabled:opacity-50"
            >
              {deactivateDiagnosis.isPending ? "Deactivating…" : "Deactivate"}
            </button>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </section>
  );
}
