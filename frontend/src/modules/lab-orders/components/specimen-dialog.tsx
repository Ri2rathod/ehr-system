"use client";

import { useEffect, useState } from "react";
import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { useCreateSpecimen, useRejectSpecimen } from "../hooks/use-lab-orders";
import { Specimen, SpecimenType } from "../types/lab-order.types";

const SPECIMEN_TYPES: SpecimenType[] = [
  "BLOOD",
  "SERUM",
  "PLASMA",
  "URINE",
  "STOOL",
  "SWAB",
  "SALIVA",
  "TISSUE",
  "OTHER",
];

function describeError(caught: unknown, fallback: string): string {
  const response = (caught as { response?: { status?: number; data?: { message?: string } } })
    ?.response;
  const message = String(response?.data?.message || "");
  if (response?.status === 403) return "You do not have permission to perform this action.";
  if (message) return message;
  return fallback;
}

interface SpecimenDialogProps {
  open: boolean;
  labOrderUuid: string;
  onClose: () => void;
}

export function SpecimenDialog({ open, labOrderUuid, onClose }: SpecimenDialogProps) {
  const createSpecimen = useCreateSpecimen(labOrderUuid);
  const [specimenType, setSpecimenType] = useState<SpecimenType>("BLOOD");
  const [specimenIdentifier, setSpecimenIdentifier] = useState("");
  const [notes, setNotes] = useState("");
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!open) return;
    setError("");
    setSpecimenType("BLOOD");
    setSpecimenIdentifier("");
    setNotes("");
  }, [open]);

  const submit = async () => {
    setError("");
    setSaving(true);
    try {
      await createSpecimen.mutateAsync({
        specimenType,
        specimenIdentifier: specimenIdentifier.trim() || undefined,
        notes: notes.trim() || undefined,
      });
      onClose();
    } catch (caught: unknown) {
      setError(describeError(caught, "Unable to register the specimen."));
    } finally {
      setSaving(false);
    }
  };

  return (
    <AlertDialog open={open}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Register Specimen</AlertDialogTitle>
          <AlertDialogDescription>
            Register the specimen to collect for this order.
          </AlertDialogDescription>
        </AlertDialogHeader>

        <div className="space-y-3">
          <div>
            <label
              className="text-xs font-bold text-on-surface-variant"
              htmlFor="specimen-type"
            >
              Specimen type
            </label>
            <select
              id="specimen-type"
              value={specimenType}
              onChange={(event) => setSpecimenType(event.target.value as SpecimenType)}
              className="mt-1 block h-9 w-full rounded border border-outline-variant bg-surface px-2 text-sm"
            >
              {SPECIMEN_TYPES.map((type) => (
                <option key={type} value={type}>
                  {type}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label
              className="text-xs font-bold text-on-surface-variant"
              htmlFor="specimen-identifier"
            >
              Identifier (optional)
            </label>
            <input
              id="specimen-identifier"
              value={specimenIdentifier}
              onChange={(event) => setSpecimenIdentifier(event.target.value)}
              maxLength={100}
              placeholder="Tube-001"
              className="mt-1 block h-9 w-full rounded border border-outline-variant bg-surface px-2 text-sm"
            />
          </div>

          <div>
            <label className="text-xs font-bold text-on-surface-variant" htmlFor="specimen-notes">
              Notes (optional)
            </label>
            <textarea
              id="specimen-notes"
              value={notes}
              onChange={(event) => setNotes(event.target.value)}
              maxLength={500}
              className="mt-1 block w-full rounded border border-outline-variant bg-surface p-2 text-sm"
            />
          </div>
        </div>

        {error && (
          <p role="alert" className="mt-2 text-xs font-semibold text-error">
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
            {saving ? "Saving…" : "Register Specimen"}
          </button>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}

interface SpecimenRejectDialogProps {
  open: boolean;
  labOrderUuid: string;
  specimen: Specimen | null;
  onClose: () => void;
}

export function SpecimenRejectDialog({
  open,
  labOrderUuid,
  specimen,
  onClose,
}: SpecimenRejectDialogProps) {
  const rejectSpecimen = useRejectSpecimen(labOrderUuid);
  const [rejectionReason, setRejectionReason] = useState("");
  const [notes, setNotes] = useState("");
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!open) return;
    setError("");
    setRejectionReason("");
    setNotes("");
  }, [open]);

  const submit = async () => {
    setError("");
    if (!rejectionReason.trim()) {
      setError("A rejection reason is required.");
      return;
    }
    if (!specimen) return;
    setSaving(true);
    try {
      await rejectSpecimen.mutateAsync({
        specimenUuid: specimen.uuid,
        payload: {
          rejectionReason: rejectionReason.trim(),
          notes: notes.trim() || undefined,
        },
      });
      onClose();
    } catch (caught: unknown) {
      setError(describeError(caught, "Unable to reject the specimen."));
    } finally {
      setSaving(false);
    }
  };

  return (
    <AlertDialog open={open}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Reject Specimen</AlertDialogTitle>
          <AlertDialogDescription>
            {specimen
              ? `Record why ${specimen.specimenType} specimen ${specimen.specimenIdentifier || specimen.uuid.slice(0, 8)} is being rejected.`
              : "Record the rejection reason."}
          </AlertDialogDescription>
        </AlertDialogHeader>

        <div className="space-y-3">
          <div>
            <label
              className="text-xs font-bold text-on-surface-variant"
              htmlFor="rejection-reason"
            >
              Rejection reason
            </label>
            <input
              id="rejection-reason"
              value={rejectionReason}
              onChange={(event) => setRejectionReason(event.target.value)}
              maxLength={300}
              placeholder="Hemolyzed sample"
              className="mt-1 block h-9 w-full rounded border border-outline-variant bg-surface px-2 text-sm"
            />
          </div>
          <div>
            <label
              className="text-xs font-bold text-on-surface-variant"
              htmlFor="rejection-notes"
              >
              Notes (optional)
            </label>
            <textarea
              id="rejection-notes"
              value={notes}
              onChange={(event) => setNotes(event.target.value)}
              maxLength={500}
              className="mt-1 block w-full rounded border border-outline-variant bg-surface p-2 text-sm"
            />
          </div>
        </div>

        {error && (
          <p role="alert" className="mt-2 text-xs font-semibold text-error">
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
            className="h-8 rounded-md bg-error px-3 text-xs font-bold text-on-error disabled:opacity-50"
          >
            {saving ? "Saving…" : "Reject Specimen"}
          </button>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
