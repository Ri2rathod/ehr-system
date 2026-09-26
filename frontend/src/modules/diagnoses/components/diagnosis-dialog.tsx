"use client";

import { useEffect, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { useCreateDiagnosis, useUpdateDiagnosis } from "../hooks/use-diagnoses";
import { Diagnosis, DiagnosisStatus, DiagnosisType } from "../types/diagnosis.types";

interface DiagnosisDialogProps {
  open: boolean;
  encounterUuid: string;
  /** null = create mode */
  diagnosis: Diagnosis | null;
  hasPrimary: boolean;
  onClose: () => void;
}

function describeError(caught: unknown): string {
  const response = (caught as { response?: { status?: number; data?: { message?: string } } })?.response;
  const message = String(response?.data?.message || "");
  if (response?.status === 403) return "You do not have permission to perform this action.";
  if (message) return message;
  return "Unable to save the diagnosis.";
}

export function DiagnosisDialog({
  open,
  encounterUuid,
  diagnosis,
  hasPrimary,
  onClose,
}: DiagnosisDialogProps) {
  const queryClient = useQueryClient();
  const createDiagnosis = useCreateDiagnosis(encounterUuid);
  const updateDiagnosis = useUpdateDiagnosis(encounterUuid);

  const [name, setName] = useState("");
  const [code, setCode] = useState("");
  const [codeSystem, setCodeSystem] = useState("");
  const [diagnosisType, setDiagnosisType] = useState<DiagnosisType>("SECONDARY");
  const [clinicalStatus, setClinicalStatus] = useState<DiagnosisStatus>("ACTIVE");
  const [onsetDate, setOnsetDate] = useState("");
  const [resolvedDate, setResolvedDate] = useState("");
  const [notes, setNotes] = useState("");
  const [replacePrimary, setReplacePrimary] = useState(false);
  const [error, setError] = useState("");

  const editing = diagnosis !== null;
  const saving = createDiagnosis.isPending || updateDiagnosis.isPending;

  useEffect(() => {
    if (!open) return;
    setError("");
    setReplacePrimary(false);
    setName(diagnosis?.name || "");
    setCode(diagnosis?.code || "");
    setCodeSystem(diagnosis?.codeSystem || "");
    setDiagnosisType(diagnosis?.diagnosisType || "SECONDARY");
    setClinicalStatus(diagnosis?.clinicalStatus || "ACTIVE");
    setOnsetDate(diagnosis?.onsetDate || "");
    setResolvedDate(diagnosis?.resolvedDate || "");
    setNotes(diagnosis?.notes || "");
    // Initialize when the dialog opens or a different diagnosis is selected.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, diagnosis?.uuid]);

  const showReplaceWarning = !editing && diagnosisType === "PRIMARY" && hasPrimary;

  const submit = async () => {
    setError("");
    if (!name.trim()) {
      setError("Diagnosis name is required.");
      return;
    }
    try {
      if (diagnosis) {
        await updateDiagnosis.mutateAsync({
          diagnosisUuid: diagnosis.uuid,
          payload: {
            name: name.trim(),
            clinicalStatus,
            onsetDate: onsetDate || undefined,
            resolvedDate: resolvedDate || undefined,
            notes: notes.trim() || undefined,
            version: diagnosis.version,
          },
        });
      } else {
        await createDiagnosis.mutateAsync({
          name: name.trim(),
          code: code.trim() || undefined,
          codeSystem: codeSystem.trim() || undefined,
          diagnosisType,
          clinicalStatus,
          onsetDate: onsetDate || undefined,
          notes: notes.trim() || undefined,
          replacePrimary: replacePrimary || undefined,
        });
      }
      onClose();
    } catch (caught: unknown) {
      const response = (caught as { response?: { status?: number } })?.response;
      setError(describeError(caught));
      if (response?.status === 409) {
        // Refresh the list so a retry uses the current server version.
        queryClient.invalidateQueries({ queryKey: ["encounter-diagnoses", encounterUuid] });
      }
    }
  };

  return (
    <AlertDialog open={open}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>{editing ? "Edit diagnosis" : "Add diagnosis"}</AlertDialogTitle>
          <AlertDialogDescription>
            {editing
              ? "Update the clinical details of this diagnosis."
              : "Record a diagnosis for this encounter. Code and code system are optional manual entries."}
          </AlertDialogDescription>
        </AlertDialogHeader>

        <label className="mt-3 block text-xs font-semibold">
          Diagnosis
          <textarea
            value={name}
            onChange={(event) => setName(event.target.value)}
            maxLength={500}
            rows={2}
            placeholder="e.g. Type 2 diabetes mellitus without complications"
            className="mt-1 block w-full rounded border border-outline-variant bg-surface p-2 text-sm"
          />
        </label>

        <div className="mt-3 grid grid-cols-2 gap-2">
          <label className="block text-xs font-semibold">
            Code
            <input
              value={code}
              onChange={(event) => setCode(event.target.value)}
              maxLength={32}
              disabled={editing}
              placeholder="e.g. E11.9"
              className="mt-1 block h-9 w-full rounded border border-outline-variant bg-surface px-2 font-mono text-sm disabled:opacity-60"
            />
          </label>
          <label className="block text-xs font-semibold">
            Code system
            <input
              value={codeSystem}
              onChange={(event) => setCodeSystem(event.target.value)}
              maxLength={50}
              disabled={editing}
              placeholder="e.g. ICD-10-CM"
              className="mt-1 block h-9 w-full rounded border border-outline-variant bg-surface px-2 font-mono text-sm disabled:opacity-60"
            />
          </label>
        </div>

        <div className="mt-3 grid grid-cols-2 gap-2">
          <label className="block text-xs font-semibold">
            Diagnosis type
            {editing ? (
              <p className="mt-1 flex h-9 items-center rounded border border-outline-variant bg-surface-container px-2 text-xs font-bold text-on-surface-variant">
                {diagnosisType === "PRIMARY" ? "Primary" : "Secondary"}
              </p>
            ) : (
              <select
                value={diagnosisType}
                onChange={(event) => setDiagnosisType(event.target.value as DiagnosisType)}
                className="mt-1 block h-9 w-full rounded border border-outline-variant bg-surface px-2"
              >
                <option value="PRIMARY">Primary</option>
                <option value="SECONDARY">Secondary</option>
              </select>
            )}
          </label>
          <label className="block text-xs font-semibold">
            Clinical status
            <select
              value={clinicalStatus}
              onChange={(event) => setClinicalStatus(event.target.value as DiagnosisStatus)}
              className="mt-1 block h-9 w-full rounded border border-outline-variant bg-surface px-2"
            >
              <option value="ACTIVE">Active</option>
              <option value="RESOLVED">Resolved</option>
              <option value="INACTIVE">Inactive</option>
            </select>
          </label>
        </div>

        <div className="mt-3 grid grid-cols-2 gap-2">
          <label className="block text-xs font-semibold">
            Onset date <span className="font-normal text-on-surface-variant">(optional)</span>
            <input
              type="date"
              value={onsetDate}
              onChange={(event) => setOnsetDate(event.target.value)}
              className="mt-1 block h-9 w-full rounded border border-outline-variant bg-surface px-2"
            />
          </label>
          {editing && (
            <label className="block text-xs font-semibold">
              Resolved date <span className="font-normal text-on-surface-variant">(optional)</span>
              <input
                type="date"
                value={resolvedDate}
                onChange={(event) => setResolvedDate(event.target.value)}
                className="mt-1 block h-9 w-full rounded border border-outline-variant bg-surface px-2"
              />
            </label>
          )}
        </div>

        <label className="mt-3 block text-xs font-semibold">
          Notes <span className="font-normal text-on-surface-variant">(optional)</span>
          <textarea
            value={notes}
            onChange={(event) => setNotes(event.target.value)}
            maxLength={5000}
            rows={2}
            className="mt-1 block w-full rounded border border-outline-variant bg-surface p-2 text-sm"
          />
        </label>

        {showReplaceWarning && (
          <div className="mt-3 rounded-md border border-amber-300 bg-amber-50 p-2 text-xs text-amber-900">
            <p className="font-semibold">A primary diagnosis already exists for this encounter.</p>
            <label className="mt-2 flex items-center gap-2 text-xs font-semibold">
              <input
                type="checkbox"
                checked={replacePrimary}
                onChange={(event) => setReplacePrimary(event.target.checked)}
              />
              Replace primary diagnosis
            </label>
          </div>
        )}

        {error && (
          <p role="alert" className="mt-3 text-xs font-semibold text-error">
            {error}
          </p>
        )}

        <AlertDialogFooter>
          <button
            onClick={onClose}
            className="h-8 rounded-md border border-outline-variant px-3 text-xs font-semibold"
          >
            Cancel
          </button>
          <button
            disabled={saving}
            onClick={submit}
            className="h-8 rounded-md bg-primary px-3 text-xs font-bold text-on-primary disabled:opacity-50"
          >
            {saving ? "Saving…" : editing ? "Save Changes" : "Add Diagnosis"}
          </button>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
