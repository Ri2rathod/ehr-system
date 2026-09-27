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
  useCreateTreatmentPlan,
  useUpdateTreatmentPlan,
} from "../hooks/use-treatment-plans";
import { TreatmentPlan } from "../types/treatment-plan.types";

interface TreatmentPlanDialogProps {
  open: boolean;
  encounterUuid: string;
  /** null = create mode */
  plan: TreatmentPlan | null;
  onClose: () => void;
}

function describeError(caught: unknown): string {
  const response = (caught as { response?: { status?: number; data?: { message?: string } } })?.response;
  const message = String(response?.data?.message || "");
  if (response?.status === 403) return "You do not have permission to perform this action.";
  if (message) return message;
  return "Unable to save the treatment plan.";
}

export function TreatmentPlanDialog({
  open,
  encounterUuid,
  plan,
  onClose,
}: TreatmentPlanDialogProps) {
  const queryClient = useQueryClient();
  const createPlan = useCreateTreatmentPlan(encounterUuid);
  const updatePlan = useUpdateTreatmentPlan(encounterUuid);

  const [title, setTitle] = useState("");
  const [goals, setGoals] = useState("");
  const [instructions, setInstructions] = useState("");
  const [followUpInstructions, setFollowUpInstructions] = useState("");
  const [notes, setNotes] = useState("");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [error, setError] = useState("");

  const editing = plan !== null;
  const saving = createPlan.isPending || updatePlan.isPending;

  useEffect(() => {
    if (!open) return;
    setError("");
    setTitle(plan?.title || "");
    setGoals(plan?.goals || "");
    setInstructions(plan?.instructions || "");
    setFollowUpInstructions(plan?.followUpInstructions || "");
    setNotes(plan?.notes || "");
    setStartDate(plan?.startDate || "");
    setEndDate(plan?.endDate || "");
    // Initialize when the dialog opens or a different plan is selected.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, plan?.uuid]);

  const submit = async () => {
    setError("");
    if (!title.trim()) {
      setError("Title is required.");
      return;
    }
    if (startDate && endDate && endDate < startDate) {
      setError("End date must be on or after start date.");
      return;
    }
    try {
      if (plan) {
        await updatePlan.mutateAsync({
          planUuid: plan.uuid,
          payload: {
            title: title.trim(),
            goals: goals.trim() || undefined,
            instructions: instructions.trim() || undefined,
            followUpInstructions: followUpInstructions.trim() || undefined,
            notes: notes.trim() || undefined,
            startDate: startDate || undefined,
            endDate: endDate || undefined,
            version: plan.version,
          },
        });
      } else {
        await createPlan.mutateAsync({
          title: title.trim(),
          goals: goals.trim() || undefined,
          instructions: instructions.trim() || undefined,
          followUpInstructions: followUpInstructions.trim() || undefined,
          notes: notes.trim() || undefined,
          startDate: startDate || undefined,
          endDate: endDate || undefined,
        });
      }
      onClose();
    } catch (caught: unknown) {
      const response = (caught as { response?: { status?: number } })?.response;
      setError(describeError(caught));
      if (response?.status === 409) {
        // Refresh the list so a retry uses the current server version.
        queryClient.invalidateQueries({
          queryKey: ["encounter-treatment-plans", encounterUuid],
        });
      }
    }
  };

  return (
    <AlertDialog open={open}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>{editing ? "Edit treatment plan" : "Add treatment plan"}</AlertDialogTitle>
          <AlertDialogDescription>
            {editing
              ? "Update the plan goals and care instructions."
              : "Define the overall goals and instructions for this encounter's care."}
          </AlertDialogDescription>
        </AlertDialogHeader>

        <label className="mt-3 block text-xs font-semibold">
          Title
          <input
            value={title}
            onChange={(event) => setTitle(event.target.value)}
            maxLength={500}
            placeholder="e.g. Diabetes management plan"
            className="mt-1 block h-9 w-full rounded border border-outline-variant bg-surface px-2 text-sm"
          />
        </label>

        <label className="mt-3 block text-xs font-semibold">
          Goals <span className="font-normal text-on-surface-variant">(optional)</span>
          <textarea
            value={goals}
            onChange={(event) => setGoals(event.target.value)}
            maxLength={5000}
            rows={2}
            placeholder="e.g. HbA1c below 7%"
            className="mt-1 block w-full rounded border border-outline-variant bg-surface p-2 text-sm"
          />
        </label>

        <label className="mt-3 block text-xs font-semibold">
          Instructions <span className="font-normal text-on-surface-variant">(optional)</span>
          <textarea
            value={instructions}
            onChange={(event) => setInstructions(event.target.value)}
            maxLength={5000}
            rows={2}
            className="mt-1 block w-full rounded border border-outline-variant bg-surface p-2 text-sm"
          />
        </label>

        <label className="mt-3 block text-xs font-semibold">
          Follow-up instructions <span className="font-normal text-on-surface-variant">(optional)</span>
          <textarea
            value={followUpInstructions}
            onChange={(event) => setFollowUpInstructions(event.target.value)}
            maxLength={5000}
            rows={2}
            className="mt-1 block w-full rounded border border-outline-variant bg-surface p-2 text-sm"
          />
        </label>

        <div className="mt-3 grid grid-cols-2 gap-2">
          <label className="block text-xs font-semibold">
            Start date <span className="font-normal text-on-surface-variant">(optional)</span>
            <input
              type="date"
              value={startDate}
              onChange={(event) => setStartDate(event.target.value)}
              className="mt-1 block h-9 w-full rounded border border-outline-variant bg-surface px-2"
            />
          </label>
          <label className="block text-xs font-semibold">
            End date <span className="font-normal text-on-surface-variant">(optional)</span>
            <input
              type="date"
              value={endDate}
              onChange={(event) => setEndDate(event.target.value)}
              className="mt-1 block h-9 w-full rounded border border-outline-variant bg-surface px-2"
            />
          </label>
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
            {saving ? "Saving…" : editing ? "Save Changes" : "Add Plan"}
          </button>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
