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
import {
  useCreatePrescription,
  useUpdatePrescription,
} from "../hooks/use-prescriptions";
import { Prescription } from "../types/prescription.types";

interface PrescriptionDialogProps {
  open: boolean;
  encounterUuid: string;
  /** null = create mode */
  prescription: Prescription | null;
  onClose: () => void;
}

function describeError(caught: unknown): string {
  const response = (caught as { response?: { status?: number; data?: { message?: string } } })
    ?.response;
  const message = String(response?.data?.message || "");
  if (response?.status === 403) return "You do not have permission to perform this action.";
  if (message) return message;
  return "Unable to save the prescription.";
}

export function PrescriptionDialog({
  open,
  encounterUuid,
  prescription,
  onClose,
}: PrescriptionDialogProps) {
  const queryClient = useQueryClient();
  const createPrescription = useCreatePrescription(encounterUuid);
  const updatePrescription = useUpdatePrescription(encounterUuid);

  const [notes, setNotes] = useState("");
  const [error, setError] = useState("");

  const editing = prescription !== null;
  const saving = createPrescription.isPending || updatePrescription.isPending;

  useEffect(() => {
    if (!open) return;
    setError("");
    setNotes(prescription?.notes || "");
    // Initialize when the dialog opens or a different prescription is selected.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, prescription?.uuid]);

  const submit = async () => {
    setError("");
    try {
      if (prescription) {
        await updatePrescription.mutateAsync({
          prescriptionUuid: prescription.uuid,
          payload: {
            notes: notes.trim() || undefined,
            version: prescription.version,
          },
        });
      } else {
        await createPrescription.mutateAsync({ notes: notes.trim() || undefined });
      }
      onClose();
    } catch (caught: unknown) {
      const response = (caught as { response?: { status?: number } })?.response;
      setError(describeError(caught));
      if (response?.status === 409) {
        // Refresh the list so a retry uses the current server version.
        queryClient.invalidateQueries({
          queryKey: ["encounter-prescriptions", encounterUuid],
        });
      }
    }
  };

  return (
    <AlertDialog open={open}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>{editing ? "Edit prescription" : "New prescription"}</AlertDialogTitle>
          <AlertDialogDescription>
            {editing
              ? "Update the prescription notes. Status changes are made with the explicit actions."
              : "Create a prescription for this encounter. Add medications, then activate it."}
          </AlertDialogDescription>
        </AlertDialogHeader>

        <label className="mt-3 block text-xs font-semibold">
          Notes <span className="font-normal text-on-surface-variant">(optional)</span>
          <textarea
            value={notes}
            onChange={(event) => setNotes(event.target.value)}
            maxLength={5000}
            rows={4}
            placeholder="e.g. Start metformin, review in 4 weeks"
            className="mt-1 block w-full rounded border border-outline-variant bg-surface p-2 text-sm"
          />
        </label>

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
            {saving ? "Saving…" : editing ? "Save Changes" : "Create Prescription"}
          </button>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
