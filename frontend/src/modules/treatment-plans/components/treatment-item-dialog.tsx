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
import { useDiagnoses } from "@/modules/diagnoses/hooks/use-diagnoses";
import {
  useCreateTreatmentItem,
  useUpdateTreatmentItem,
} from "../hooks/use-treatment-plans";
import {
  DurationUnit,
  TreatmentPlanItem,
  TreatmentPriority,
  TreatmentType,
} from "../types/treatment-plan.types";

interface TreatmentItemDialogProps {
  open: boolean;
  encounterUuid: string;
  planUuid: string;
  /** null = create mode */
  item: TreatmentPlanItem | null;
  onClose: () => void;
}

const treatmentTypeOptions: { value: TreatmentType; label: string }[] = [
  { value: "LIFESTYLE", label: "Lifestyle" },
  { value: "DIET", label: "Diet" },
  { value: "EXERCISE", label: "Exercise" },
  { value: "PHYSIOTHERAPY", label: "Physiotherapy" },
  { value: "BEHAVIORAL", label: "Behavioral" },
  { value: "EDUCATION", label: "Education" },
  { value: "MONITORING", label: "Monitoring" },
  { value: "FOLLOW_UP", label: "Follow-up" },
  { value: "PROCEDURE", label: "Procedure" },
  { value: "OTHER", label: "Other" },
];

const durationUnitOptions: { value: DurationUnit; label: string }[] = [
  { value: "DAYS", label: "Days" },
  { value: "WEEKS", label: "Weeks" },
  { value: "MONTHS", label: "Months" },
];

function describeError(caught: unknown): string {
  const response = (caught as { response?: { status?: number; data?: { message?: string } } })?.response;
  const message = String(response?.data?.message || "");
  if (response?.status === 403) return "You do not have permission to perform this action.";
  if (message) return message;
  return "Unable to save the treatment.";
}

export function TreatmentItemDialog({
  open,
  encounterUuid,
  planUuid,
  item,
  onClose,
}: TreatmentItemDialogProps) {
  const queryClient = useQueryClient();
  const createItem = useCreateTreatmentItem(encounterUuid);
  const updateItem = useUpdateTreatmentItem(encounterUuid);
  const { data: diagnoses } = useDiagnoses(encounterUuid, open);

  const [diagnosisUuid, setDiagnosisUuid] = useState("");
  const [treatmentType, setTreatmentType] = useState<TreatmentType>("LIFESTYLE");
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [instructions, setInstructions] = useState("");
  const [frequency, setFrequency] = useState("");
  const [duration, setDuration] = useState("");
  const [durationUnit, setDurationUnit] = useState<DurationUnit | "">("");
  const [priority, setPriority] = useState<TreatmentPriority>("MEDIUM");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [error, setError] = useState("");

  const editing = item !== null;
  const saving = createItem.isPending || updateItem.isPending;

  useEffect(() => {
    if (!open) return;
    setError("");
    setDiagnosisUuid(item?.diagnosisUuid || "");
    setTreatmentType(item?.treatmentType || "LIFESTYLE");
    setName(item?.name || "");
    setDescription(item?.description || "");
    setInstructions(item?.instructions || "");
    setFrequency(item?.frequency || "");
    setDuration(item?.duration != null ? String(item.duration) : "");
    setDurationUnit(item?.durationUnit || "");
    setPriority(item?.priority || "MEDIUM");
    setStartDate(item?.startDate || "");
    setEndDate(item?.endDate || "");
    // Initialize when the dialog opens or a different item is selected.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, item?.uuid]);

  const submit = async () => {
    setError("");
    if (!name.trim()) {
      setError("Name is required.");
      return;
    }
    if (duration && !durationUnit) {
      setError("Duration unit is required when duration is provided.");
      return;
    }
    if (durationUnit && !duration) {
      setError("Duration is required when duration unit is provided.");
      return;
    }
    if (startDate && endDate && endDate < startDate) {
      setError("End date must be on or after start date.");
      return;
    }

    try {
      if (item) {
        const originalDiagnosis = item.diagnosisUuid || "";
        const diagnosisChanged = diagnosisUuid !== originalDiagnosis;
        await updateItem.mutateAsync({
          planUuid,
          itemUuid: item.uuid,
          payload: {
            treatmentType,
            name: name.trim(),
            description: description.trim() || undefined,
            instructions: instructions.trim() || undefined,
            frequency: frequency.trim() || undefined,
            duration: duration ? Number(duration) : undefined,
            durationUnit: durationUnit || undefined,
            priority,
            startDate: startDate || undefined,
            endDate: endDate || undefined,
            ...(diagnosisChanged
              ? diagnosisUuid
                ? { diagnosisUuid }
                : { unlinkDiagnosis: true }
              : {}),
            version: item.version,
          },
        });
      } else {
        await createItem.mutateAsync({
          planUuid,
          payload: {
            diagnosisUuid: diagnosisUuid || undefined,
            treatmentType,
            name: name.trim(),
            description: description.trim() || undefined,
            instructions: instructions.trim() || undefined,
            frequency: frequency.trim() || undefined,
            duration: duration ? Number(duration) : undefined,
            durationUnit: durationUnit || undefined,
            priority,
            startDate: startDate || undefined,
            endDate: endDate || undefined,
          },
        });
      }
      onClose();
    } catch (caught: unknown) {
      const response = (caught as { response?: { status?: number } })?.response;
      setError(describeError(caught));
      if (response?.status === 409) {
        // Refresh so a retry uses the current server version.
        queryClient.invalidateQueries({ queryKey: ["treatment-plan-items"] });
      }
    }
  };

  return (
    <AlertDialog open={open}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>{editing ? "Edit treatment" : "Add treatment"}</AlertDialogTitle>
          <AlertDialogDescription>
            {editing
              ? "Update this treatment item's details."
              : "Add a concrete care action to this plan. Medications are recorded in a future module."}
          </AlertDialogDescription>
        </AlertDialogHeader>

        <label className="mt-3 block text-xs font-semibold">
          Linked diagnosis <span className="font-normal text-on-surface-variant">(optional)</span>
          <select
            value={diagnosisUuid}
            onChange={(event) => setDiagnosisUuid(event.target.value)}
            className="mt-1 block h-9 w-full rounded border border-outline-variant bg-surface px-2 text-sm"
          >
            <option value="">No linked diagnosis</option>
            {diagnoses?.map((diagnosis) => (
              <option key={diagnosis.uuid} value={diagnosis.uuid}>
                {diagnosis.code ? `${diagnosis.code} · ` : ""}
                {diagnosis.name}
              </option>
            ))}
          </select>
        </label>

        <div className="mt-3 grid grid-cols-2 gap-2">
          <label className="block text-xs font-semibold">
            Treatment type
            <select
              value={treatmentType}
              onChange={(event) => setTreatmentType(event.target.value as TreatmentType)}
              className="mt-1 block h-9 w-full rounded border border-outline-variant bg-surface px-2 text-sm"
            >
              {treatmentTypeOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>
          <label className="block text-xs font-semibold">
            Priority
            <select
              value={priority}
              onChange={(event) => setPriority(event.target.value as TreatmentPriority)}
              className="mt-1 block h-9 w-full rounded border border-outline-variant bg-surface px-2 text-sm"
            >
              <option value="LOW">Low</option>
              <option value="MEDIUM">Medium</option>
              <option value="HIGH">High</option>
            </select>
          </label>
        </div>

        <label className="mt-3 block text-xs font-semibold">
          Name
          <input
            value={name}
            onChange={(event) => setName(event.target.value)}
            maxLength={500}
            placeholder="e.g. 30-minute brisk walk"
            className="mt-1 block h-9 w-full rounded border border-outline-variant bg-surface px-2 text-sm"
          />
        </label>

        <label className="mt-3 block text-xs font-semibold">
          Description <span className="font-normal text-on-surface-variant">(optional)</span>
          <textarea
            value={description}
            onChange={(event) => setDescription(event.target.value)}
            maxLength={5000}
            rows={2}
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

        <div className="mt-3 grid grid-cols-3 gap-2">
          <label className="block text-xs font-semibold">
            Frequency <span className="font-normal text-on-surface-variant">(optional)</span>
            <input
              value={frequency}
              onChange={(event) => setFrequency(event.target.value)}
              maxLength={200}
              placeholder="e.g. 5 days/week"
              className="mt-1 block h-9 w-full rounded border border-outline-variant bg-surface px-2 text-sm"
            />
          </label>
          <label className="block text-xs font-semibold">
            Duration <span className="font-normal text-on-surface-variant">(optional)</span>
            <input
              type="number"
              min={1}
              value={duration}
              onChange={(event) => setDuration(event.target.value)}
              placeholder="e.g. 12"
              className="mt-1 block h-9 w-full rounded border border-outline-variant bg-surface px-2 font-mono text-sm"
            />
          </label>
          <label className="block text-xs font-semibold">
            Unit <span className="font-normal text-on-surface-variant">(optional)</span>
            <select
              value={durationUnit}
              onChange={(event) => setDurationUnit(event.target.value as DurationUnit | "")}
              className="mt-1 block h-9 w-full rounded border border-outline-variant bg-surface px-2 text-sm"
            >
              <option value="">—</option>
              {durationUnitOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>
        </div>

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
            {saving ? "Saving…" : editing ? "Save Changes" : "Add Treatment"}
          </button>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
