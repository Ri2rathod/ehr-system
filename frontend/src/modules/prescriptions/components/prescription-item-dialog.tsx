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
import { useMedicationSearch } from "@/modules/medications/hooks/use-medications";
import { Medication } from "@/modules/medications/types/medication.types";
import {
  useCreatePrescriptionItem,
  useUpdatePrescriptionItem,
} from "../hooks/use-prescriptions";
import {
  CreatePrescriptionItemPayload,
  DoseUnit,
  DurationUnit,
  FrequencyUnit,
  MedicationFrequency,
  PrescriptionItem,
  QuantityUnit,
  Route,
} from "../types/prescription.types";

interface PrescriptionItemDialogProps {
  open: boolean;
  encounterUuid: string;
  prescriptionUuid: string;
  /** null = create mode (medication is selectable) */
  item: PrescriptionItem | null;
  onClose: () => void;
}

const doseUnitOptions: DoseUnit[] = [
  "MG",
  "MCG",
  "G",
  "ML",
  "IU",
  "MEQ",
  "PERCENT",
  "PUFF",
  "DROP",
  "UNIT",
  "OTHER",
];

const routeOptions: Route[] = [
  "ORAL",
  "TOPICAL",
  "INTRAVENOUS",
  "INTRAMUSCULAR",
  "SUBCUTANEOUS",
  "INHALATION",
  "OPHTHALMIC",
  "OTIC",
  "NASAL",
  "RECTAL",
  "OTHER",
];

const frequencyOptions: MedicationFrequency[] = [
  "ONCE_DAILY",
  "TWICE_DAILY",
  "THREE_TIMES_DAILY",
  "FOUR_TIMES_DAILY",
  "EVERY_MORNING",
  "EVERY_EVENING",
  "AT_BEDTIME",
  "WEEKLY",
  "AS_NEEDED",
  "CUSTOM",
];

const frequencyLabel: Record<MedicationFrequency, string> = {
  ONCE_DAILY: "Once daily",
  TWICE_DAILY: "Twice daily",
  THREE_TIMES_DAILY: "Three times daily",
  FOUR_TIMES_DAILY: "Four times daily",
  EVERY_MORNING: "Every morning",
  EVERY_EVENING: "Every evening",
  AT_BEDTIME: "At bedtime",
  WEEKLY: "Weekly",
  AS_NEEDED: "As needed (PRN)",
  CUSTOM: "Custom…",
};

const durationUnitOptions: DurationUnit[] = ["DAYS", "WEEKS", "MONTHS"];
const frequencyUnitOptions: FrequencyUnit[] = ["HOURS", "DAYS"];

const quantityUnitOptions: QuantityUnit[] = [
  "TABLETS",
  "CAPSULES",
  "ML",
  "G",
  "PUFFS",
  "DROPS",
  "UNITS",
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

function medicationLabel(medication: Medication): string {
  const strength =
    medication.strength != null && medication.strengthUnit
      ? ` ${medication.strength} ${medication.strengthUnit}`
      : "";
  const form = medication.dosageForm ? ` · ${medication.dosageForm.toLowerCase()}` : "";
  const code = medication.code ? ` (${medication.code})` : "";
  return `${medication.genericName}${strength}${form}${code}`;
}

export function PrescriptionItemDialog({
  open,
  encounterUuid,
  prescriptionUuid,
  item,
  onClose,
}: PrescriptionItemDialogProps) {
  const queryClient = useQueryClient();
  const createItem = useCreatePrescriptionItem(encounterUuid);
  const updateItem = useUpdatePrescriptionItem(encounterUuid);

  const editing = item !== null;

  const [query, setQuery] = useState("");
  const [debouncedQuery, setDebouncedQuery] = useState("");
  const [medication, setMedication] = useState<Medication | null>(null);
  const [dose, setDose] = useState("");
  const [doseUnit, setDoseUnit] = useState<DoseUnit>("MG");
  const [route, setRoute] = useState<Route>("ORAL");
  const [frequency, setFrequency] = useState<MedicationFrequency>("ONCE_DAILY");
  const [frequencyValue, setFrequencyValue] = useState("");
  const [frequencyUnit, setFrequencyUnit] = useState<FrequencyUnit | "">("");
  const [duration, setDuration] = useState("");
  const [durationUnit, setDurationUnit] = useState<DurationUnit | "">("");
  const [quantity, setQuantity] = useState("");
  const [quantityUnit, setQuantityUnit] = useState<QuantityUnit | "">("");
  const [refills, setRefills] = useState("0");
  const [instructions, setInstructions] = useState("");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [error, setError] = useState("");

  const search = useMedicationSearch(debouncedQuery);
  const saving = createItem.isPending || updateItem.isPending;

  useEffect(() => {
    if (!open) return;
    setError("");
    setQuery("");
    setDebouncedQuery("");
    setMedication(
      item
        ? {
            uuid: item.medicationUuid,
            code: item.medicationCode,
            genericName: item.medicationGenericName,
            brandName: item.medicationBrandName,
            strength: item.medicationStrength,
            strengthUnit: item.medicationStrengthUnit,
            dosageForm: item.medicationDosageForm,
            route: item.medicationRoute,
            isActive: true,
          }
        : null,
    );
    setDose(item?.dose != null ? String(item.dose) : "");
    setDoseUnit(item?.doseUnit || "MG");
    setRoute(item?.route || "ORAL");
    setFrequency(item?.frequency || "ONCE_DAILY");
    setFrequencyValue(item?.frequencyValue != null ? String(item.frequencyValue) : "");
    setFrequencyUnit(item?.frequencyUnit || "");
    setDuration(item?.duration != null ? String(item.duration) : "");
    setDurationUnit(item?.durationUnit || "");
    setQuantity(item?.quantity != null ? String(item.quantity) : "");
    setQuantityUnit(item?.quantityUnit || "");
    setRefills(item?.refills != null ? String(item.refills) : "0");
    setInstructions(item?.instructions || "");
    setStartDate(item?.startDate || "");
    setEndDate(item?.endDate || "");
    // Initialize when the dialog opens or a different item is selected.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, item?.uuid]);

  useEffect(() => {
    if (!open) return;
    const handle = window.setTimeout(() => setDebouncedQuery(query), 250);
    return () => window.clearTimeout(handle);
  }, [query, open]);

  const validate = (): string => {
    if (!editing && !medication) return "Select a medication from the catalog.";
    const doseNumber = Number(dose);
    if (dose.trim() === "" || Number.isNaN(doseNumber) || doseNumber < 0) {
      return "Dose must be a number that is not negative.";
    }
    if (startDate && endDate && endDate < startDate) {
      return "End date must be on or after start date.";
    }
    if ((duration && !durationUnit) || (durationUnit && !duration)) {
      return "Duration and duration unit must appear together.";
    }
    if ((quantity && !quantityUnit) || (quantityUnit && !quantity)) {
      return "Quantity and quantity unit must appear together.";
    }
    if ((frequencyValue && !frequencyUnit) || (frequencyUnit && !frequencyValue)) {
      return "Frequency value and frequency unit must appear together.";
    }
    if (frequency === "CUSTOM" && !instructions.trim()) {
      return "Instructions are required for a custom frequency.";
    }
    const refillNumber = Number(refills || "0");
    if (Number.isNaN(refillNumber) || refillNumber < 0) {
      return "Refills must not be negative.";
    }
    return "";
  };

  const submit = async () => {
    setError("");
    const problem = validate();
    if (problem) {
      setError(problem);
      return;
    }

    const refillCount = Number(refills || "0");
    const shared = {
      dose: Number(dose),
      doseUnit,
      route,
      frequency,
      frequencyValue: frequencyValue ? Number(frequencyValue) : undefined,
      frequencyUnit: frequencyUnit || undefined,
      duration: duration ? Number(duration) : undefined,
      durationUnit: durationUnit || undefined,
      quantity: quantity ? Number(quantity) : undefined,
      quantityUnit: quantityUnit || undefined,
      refills: Number.isNaN(refillCount) ? 0 : refillCount,
      instructions: instructions.trim() || undefined,
      startDate: startDate || undefined,
      endDate: endDate || undefined,
    };

    try {
      if (item) {
        await updateItem.mutateAsync({
          prescriptionUuid,
          itemUuid: item.uuid,
          payload: { ...shared, version: item.version },
        });
      } else {
        const payload: CreatePrescriptionItemPayload = {
          ...shared,
          medicationUuid: medication!.uuid,
        };
        await createItem.mutateAsync({ prescriptionUuid, payload });
      }
      onClose();
    } catch (caught: unknown) {
      const response = (caught as { response?: { status?: number } })?.response;
      setError(describeError(caught, "Unable to save the medication."));
      if (response?.status === 409) {
        // Refresh so a retry uses the current server version.
        queryClient.invalidateQueries({ queryKey: ["prescription-items"] });
      }
    }
  };

  const results = search.data?.content ?? [];

  return (
    <AlertDialog open={open}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>{editing ? "Edit medication" : "Add medication"}</AlertDialogTitle>
          <AlertDialogDescription>
            {editing
              ? "Update dose, route, and instructions. The medication itself cannot be changed - discontinue it and add a new one instead."
              : "Search the medication catalog and define how it should be taken."}
          </AlertDialogDescription>
        </AlertDialogHeader>

        {!editing && (
          <div className="mt-3">
            <label className="block text-xs font-semibold">
              Medication
              <input
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                placeholder="Search by name, brand, or code…"
                autoComplete="off"
                className="mt-1 block h-9 w-full rounded border border-outline-variant bg-surface px-2 text-sm"
              />
            </label>
            {debouncedQuery.trim() && (
              <div className="mt-2 max-h-36 overflow-y-auto rounded border border-outline-variant">
                {search.isLoading ? (
                  <p className="p-2 text-xs text-on-surface-variant">Searching…</p>
                ) : search.isError ? (
                  <p className="p-2 text-xs font-semibold text-error">
                    Unable to search medications.
                  </p>
                ) : results.length === 0 ? (
                  <p className="p-2 text-xs text-on-surface-variant">No medications found.</p>
                ) : (
                  results.map((option) => (
                    <button
                      key={option.uuid}
                      type="button"
                      onClick={() => {
                        setMedication(option);
                        setQuery("");
                        setDebouncedQuery("");
                      }}
                      className="block w-full border-b border-outline-variant px-2 py-1.5 text-left text-xs last:border-b-0 hover:bg-surface-container"
                    >
                      <span className="font-semibold text-on-surface">
                        {medicationLabel(option)}
                      </span>
                      {option.brandName && (
                        <span className="block text-[11px] text-on-surface-variant">
                          Brand: {option.brandName}
                        </span>
                      )}
                    </button>
                  ))
                )}
              </div>
            )}
            {medication && (
              <div className="mt-2 flex items-center justify-between gap-2 rounded bg-surface-container px-2 py-1.5">
                <p className="min-w-0 truncate text-xs font-semibold text-on-surface">
                  {medicationLabel(medication)}
                </p>
                <button
                  type="button"
                  onClick={() => setMedication(null)}
                  className="shrink-0 text-[11px] font-bold text-on-surface-variant hover:text-error"
                >
                  Change
                </button>
              </div>
            )}
          </div>
        )}

        {editing && medication && (
          <div className="mt-3 rounded bg-surface-container px-2 py-1.5">
            <p className="text-xs font-semibold text-on-surface">{medicationLabel(medication)}</p>
            <p className="text-[11px] text-on-surface-variant">Medication is fixed once created.</p>
          </div>
        )}

        <div className="mt-3 grid grid-cols-3 gap-2">
          <label className="block text-xs font-semibold">
            Dose
            <input
              type="number"
              min="0"
              step="any"
              value={dose}
              onChange={(event) => setDose(event.target.value)}
              className="mt-1 block h-9 w-full rounded border border-outline-variant bg-surface px-2 text-sm"
            />
          </label>
          <label className="block text-xs font-semibold">
            Unit
            <select
              value={doseUnit}
              onChange={(event) => setDoseUnit(event.target.value as DoseUnit)}
              className="mt-1 block h-9 w-full rounded border border-outline-variant bg-surface px-2 text-sm"
            >
              {doseUnitOptions.map((option) => (
                <option key={option} value={option}>
                  {option}
                </option>
              ))}
            </select>
          </label>
          <label className="block text-xs font-semibold">
            Route
            <select
              value={route}
              onChange={(event) => setRoute(event.target.value as Route)}
              className="mt-1 block h-9 w-full rounded border border-outline-variant bg-surface px-2 text-sm"
            >
              {routeOptions.map((option) => (
                <option key={option} value={option}>
                  {option}
                </option>
              ))}
            </select>
          </label>
        </div>

        <label className="mt-3 block text-xs font-semibold">
          Frequency
          <select
            value={frequency}
            onChange={(event) => setFrequency(event.target.value as MedicationFrequency)}
            className="mt-1 block h-9 w-full rounded border border-outline-variant bg-surface px-2 text-sm"
          >
            {frequencyOptions.map((option) => (
              <option key={option} value={option}>
                {frequencyLabel[option]}
              </option>
            ))}
          </select>
        </label>

        <div className="mt-3 grid grid-cols-2 gap-2">
          <label className="block text-xs font-semibold">
            Every (value) <span className="font-normal text-on-surface-variant">(optional)</span>
            <input
              type="number"
              min="1"
              value={frequencyValue}
              onChange={(event) => setFrequencyValue(event.target.value)}
              placeholder="e.g. 6"
              className="mt-1 block h-9 w-full rounded border border-outline-variant bg-surface px-2 text-sm"
            />
          </label>
          <label className="block text-xs font-semibold">
            Period
            <select
              value={frequencyUnit}
              onChange={(event) => setFrequencyUnit(event.target.value as FrequencyUnit | "")}
              className="mt-1 block h-9 w-full rounded border border-outline-variant bg-surface px-2 text-sm"
            >
              <option value="">—</option>
              {frequencyUnitOptions.map((option) => (
                <option key={option} value={option}>
                  {option.toLowerCase()}
                </option>
              ))}
            </select>
          </label>
        </div>

        <div className="mt-3 grid grid-cols-2 gap-2">
          <label className="block text-xs font-semibold">
            Duration <span className="font-normal text-on-surface-variant">(optional)</span>
            <input
              type="number"
              min="1"
              value={duration}
              onChange={(event) => setDuration(event.target.value)}
              placeholder="e.g. 30"
              className="mt-1 block h-9 w-full rounded border border-outline-variant bg-surface px-2 text-sm"
            />
          </label>
          <label className="block text-xs font-semibold">
            Duration unit
            <select
              value={durationUnit}
              onChange={(event) => setDurationUnit(event.target.value as DurationUnit | "")}
              className="mt-1 block h-9 w-full rounded border border-outline-variant bg-surface px-2 text-sm"
            >
              <option value="">—</option>
              {durationUnitOptions.map((option) => (
                <option key={option} value={option}>
                  {option.toLowerCase()}
                </option>
              ))}
            </select>
          </label>
        </div>

        <div className="mt-3 grid grid-cols-2 gap-2">
          <label className="block text-xs font-semibold">
            Quantity <span className="font-normal text-on-surface-variant">(optional)</span>
            <input
              type="number"
              min="0"
              value={quantity}
              onChange={(event) => setQuantity(event.target.value)}
              placeholder="e.g. 30"
              className="mt-1 block h-9 w-full rounded border border-outline-variant bg-surface px-2 text-sm"
            />
          </label>
          <label className="block text-xs font-semibold">
            Quantity unit
            <select
              value={quantityUnit}
              onChange={(event) => setQuantityUnit(event.target.value as QuantityUnit | "")}
              className="mt-1 block h-9 w-full rounded border border-outline-variant bg-surface px-2 text-sm"
            >
              <option value="">—</option>
              {quantityUnitOptions.map((option) => (
                <option key={option} value={option}>
                  {option.toLowerCase()}
                </option>
              ))}
            </select>
          </label>
        </div>

        <div className="mt-3 grid grid-cols-3 gap-2">
          <label className="block text-xs font-semibold">
            Refills
            <input
              type="number"
              min="0"
              value={refills}
              onChange={(event) => setRefills(event.target.value)}
              className="mt-1 block h-9 w-full rounded border border-outline-variant bg-surface px-2 text-sm"
            />
          </label>
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
          Instructions{" "}
          {frequency === "CUSTOM" ? (
            <span className="font-normal text-error">required for custom frequency</span>
          ) : (
            <span className="font-normal text-on-surface-variant">(optional)</span>
          )}
          <textarea
            value={instructions}
            onChange={(event) => setInstructions(event.target.value)}
            maxLength={5000}
            rows={2}
            placeholder="e.g. Take with food"
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
            {saving ? "Saving…" : editing ? "Save Changes" : "Add Medication"}
          </button>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
