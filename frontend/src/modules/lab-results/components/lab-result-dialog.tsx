"use client";

import { useEffect, useMemo, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { useLabTest } from "@/modules/lab-tests/hooks/use-lab-tests";
import { LabTest } from "@/modules/lab-tests/types/lab-test.types";
import { LabOrderItem, Specimen } from "@/modules/lab-orders/types/lab-order.types";
import { useCorrectLabResult, useCreateLabResult, useUpdateLabResult } from "../hooks/use-lab-results";
import {
  AbnormalFlag,
  LabResult,
  LabResultValuePayload,
} from "../types/lab-result.types";

export type LabResultDialogMode = "create" | "edit" | "correct";

const ABNORMAL_FLAGS: AbnormalFlag[] = [
  "NORMAL",
  "LOW",
  "HIGH",
  "CRITICAL_LOW",
  "CRITICAL_HIGH",
  "ABNORMAL",
  "POSITIVE",
  "NEGATIVE",
];

interface LabResultDialogProps {
  open: boolean;
  labOrderUuid: string;
  mode: LabResultDialogMode;
  /** required for edit/correct */
  result?: LabResult | null;
  items: LabOrderItem[];
  specimens: Specimen[];
  onClose: () => void;
}

function describeError(caught: unknown, fallback: string): string {
  const response = (caught as { response?: { status?: number; data?: { message?: string } } })
    ?.response;
  const message = String(response?.data?.message || "");
  if (response?.status === 403) return "You do not have permission to perform this action.";
  if (message) return message;
  return fallback;
}

function referenceLabel(test: LabTest): string {
  if (test.defaultReferenceText) return test.defaultReferenceText;
  if (test.defaultReferenceLow != null && test.defaultReferenceHigh != null) {
    return `${test.defaultReferenceLow} – ${test.defaultReferenceHigh}`;
  }
  return "Not set";
}

export function LabResultDialog({
  open,
  labOrderUuid,
  mode,
  result,
  items,
  specimens,
  onClose,
}: LabResultDialogProps) {
  const queryClient = useQueryClient();
  const createResult = useCreateLabResult(labOrderUuid);
  const editResult = useUpdateLabResult(labOrderUuid, result?.uuid);
  const correctResult = useCorrectLabResult(labOrderUuid, result?.uuid);

  const recordableItems = useMemo(
    () => items.filter((item) => item.status !== "CANCELLED"),
    [items],
  );
  const receivedSpecimens = useMemo(
    () => specimens.filter((specimen) => specimen.status === "RECEIVED"),
    [specimens],
  );

  const [selectedItemUuid, setSelectedItemUuid] = useState("");
  const [selectedSpecimenUuid, setSelectedSpecimenUuid] = useState("");
  const [numericText, setNumericText] = useState("");
  const [valueText, setValueText] = useState("");
  const [valueCode, setValueCode] = useState("");
  const [abnormalFlag, setAbnormalFlag] = useState<string>("");
  const [comments, setComments] = useState("");
  const [correctionReason, setCorrectionReason] = useState("");
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  const storedValue = mode === "create" ? null : (result?.values?.[0] ?? null);
  const testUuid =
    mode === "create"
      ? recordableItems.find((item) => item.uuid === selectedItemUuid)?.labTestUuid
      : storedValue?.labTestUuid;
  const test = useLabTest(testUuid);

  useEffect(() => {
    if (!open) return;
    setError("");
    setComments(result?.comments ?? "");
    setCorrectionReason("");
    setSelectedItemUuid(recordableItems[0]?.uuid ?? "");
    setSelectedSpecimenUuid(receivedSpecimens[0]?.uuid ?? "");
    setNumericText(storedValue?.valueNumeric != null ? String(storedValue.valueNumeric) : "");
    setValueText(storedValue?.valueText ?? "");
    setValueCode(storedValue?.valueCode ?? "");
    setAbnormalFlag(storedValue?.abnormalFlag ?? "");
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, result]);

  const resultType = test.data?.resultType;

  const buildValuePayload = (): LabResultValuePayload | null => {
    if (!testUuid || !resultType) return null;
    const payload: LabResultValuePayload = { labTestUuid: testUuid };
    if (resultType === "NUMERIC") {
      const parsed = Number(numericText);
      if (!numericText.trim() || Number.isNaN(parsed)) {
        setError("Enter a valid numeric result.");
        return null;
      }
      payload.valueNumeric = parsed;
    } else if (resultType === "TEXT") {
      if (!valueText.trim()) {
        setError("Enter the text result.");
        return null;
      }
      payload.valueText = valueText.trim();
    } else if (resultType === "QUALITATIVE") {
      const hasText = Boolean(valueText.trim());
      const hasCode = Boolean(valueCode.trim());
      if (!hasText && !hasCode) {
        setError("Enter a text value or a code for this qualitative test.");
        return null;
      }
      if (hasText && hasCode) {
        setError("A qualitative result can include a text value or a code, not both.");
        return null;
      }
      if (hasText) payload.valueText = valueText.trim();
      if (hasCode) payload.valueCode = valueCode.trim();
    } else if (resultType === "CODED") {
      if (!valueCode.trim()) {
        setError("Enter the coded value.");
        return null;
      }
      payload.valueCode = valueCode.trim();
    }
    if (abnormalFlag) {
      payload.abnormalFlag = abnormalFlag as AbnormalFlag;
    }
    if (storedValue) {
      payload.referenceLow = storedValue.referenceLow ?? undefined;
      payload.referenceHigh = storedValue.referenceHigh ?? undefined;
      payload.referenceText = storedValue.referenceText ?? undefined;
      payload.unit = storedValue.unit ?? undefined;
    }
    return payload;
  };

  const submit = async () => {
    setError("");
    const valuePayload = buildValuePayload();
    if (!valuePayload) return;

    if (mode === "create") {
      if (!selectedItemUuid || !selectedSpecimenUuid) {
        setError("Choose an ordered test and a received specimen.");
        return;
      }
    }
    if (mode === "correct" && !correctionReason.trim()) {
      setError("A correction reason is required.");
      return;
    }

    setSaving(true);
    try {
      if (mode === "create") {
        await createResult.mutateAsync({
          labOrderItemUuid: selectedItemUuid,
          specimenUuid: selectedSpecimenUuid,
          comments: comments.trim() || undefined,
          values: [valuePayload],
        });
      } else if (mode === "edit") {
        await editResult.mutateAsync({
          comments: comments.trim() || undefined,
          values: [valuePayload],
          version: result?.version,
        });
      } else {
        await correctResult.mutateAsync({
          correctionReason: correctionReason.trim(),
          comments: comments.trim() || undefined,
          values: [valuePayload],
          version: result?.version,
        });
      }
      onClose();
    } catch (caught: unknown) {
      const response = (caught as { response?: { status?: number } })?.response;
      setError(describeError(caught, "Unable to save the lab result."));
      if (response?.status === 409) {
        queryClient.invalidateQueries({ queryKey: ["lab-order-results"] });
        queryClient.invalidateQueries({ queryKey: ["encounter-lab-orders"] });
        queryClient.invalidateQueries({ queryKey: ["lab-order"] });
      }
    } finally {
      setSaving(false);
    }
  };

  const title =
    mode === "create" ? "Record Result" : mode === "edit" ? "Edit Result" : "Correct Result";
  const submitLabel =
    mode === "create" ? "Record Result" : mode === "edit" ? "Save Changes" : "Submit Correction";

  return (
    <AlertDialog open={open}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>{title}</AlertDialogTitle>
          <AlertDialogDescription>
            {mode === "correct"
              ? "Corrections replace the recorded values; the previous values are kept in history."
              : "Values must match the test's result type."}
          </AlertDialogDescription>
        </AlertDialogHeader>

        <div className="space-y-3">
          {mode === "create" && (
            <>
              <div>
                <label
                  className="text-xs font-bold text-on-surface-variant"
                  htmlFor="result-item"
                >
                  Ordered test
                </label>
                <select
                  id="result-item"
                  value={selectedItemUuid}
                  onChange={(event) => setSelectedItemUuid(event.target.value)}
                  className="mt-1 block h-9 w-full rounded border border-outline-variant bg-surface px-2 text-sm"
                >
                  {recordableItems.length === 0 && <option value="">No ordered tests</option>}
                  {recordableItems.map((item) => (
                    <option key={item.uuid} value={item.uuid}>
                      {item.labTestCode} · {item.labTestName}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label
                  className="text-xs font-bold text-on-surface-variant"
                  htmlFor="result-specimen"
                >
                  Specimen (received)
                </label>
                <select
                  id="result-specimen"
                  value={selectedSpecimenUuid}
                  onChange={(event) => setSelectedSpecimenUuid(event.target.value)}
                  className="mt-1 block h-9 w-full rounded border border-outline-variant bg-surface px-2 text-sm"
                >
                  {receivedSpecimens.length === 0 && (
                    <option value="">No received specimens yet</option>
                  )}
                  {receivedSpecimens.map((specimen) => (
                    <option key={specimen.uuid} value={specimen.uuid}>
                      {specimen.specimenType}
                      {specimen.specimenIdentifier ? ` · ${specimen.specimenIdentifier}` : ""}
                    </option>
                  ))}
                </select>
                {receivedSpecimens.length === 0 && (
                  <p className="mt-1 text-xs text-on-surface-variant">
                    Collect and receive a specimen before entering results.
                  </p>
                )}
              </div>
            </>
          )}

          {mode === "correct" && (
            <div>
              <label
                className="text-xs font-bold text-on-surface-variant"
                htmlFor="correction-reason"
              >
                Correction reason
              </label>
              <input
                id="correction-reason"
                value={correctionReason}
                onChange={(event) => setCorrectionReason(event.target.value)}
                maxLength={300}
                placeholder="Transcription error in the original entry"
                className="mt-1 block h-9 w-full rounded border border-outline-variant bg-surface px-2 text-sm"
              />
            </div>
          )}

          {mode !== "create" && result && (
            <p className="text-xs text-on-surface-variant">
              {result.labTestCode} · {result.labTestName} · {result.status}
            </p>
          )}

          {testUuid && !test.data && !test.isError && (
            <p className="text-xs text-on-surface-variant">Loading test definition…</p>
          )}

          {resultType === "NUMERIC" && (
            <div>
              <label className="text-xs font-bold text-on-surface-variant" htmlFor="value-numeric">
                Result
                {test.data?.unit ? ` (${test.data.unit})` : ""}
              </label>
              <input
                id="value-numeric"
                type="number"
                step="any"
                value={numericText}
                onChange={(event) => setNumericText(event.target.value)}
                className="mt-1 block h-9 w-full rounded border border-outline-variant bg-surface px-2 text-sm"
              />
            </div>
          )}

          {resultType === "TEXT" && (
            <div>
              <label className="text-xs font-bold text-on-surface-variant" htmlFor="value-text">
                Result
              </label>
              <input
                id="value-text"
                value={valueText}
                onChange={(event) => setValueText(event.target.value)}
                maxLength={300}
                className="mt-1 block h-9 w-full rounded border border-outline-variant bg-surface px-2 text-sm"
              />
            </div>
          )}

          {resultType === "QUALITATIVE" && (
            <div className="grid gap-2 sm:grid-cols-2">
              <div>
                <label className="text-xs font-bold text-on-surface-variant" htmlFor="value-qtext">
                  Text value
                </label>
                <input
                  id="value-qtext"
                  value={valueText}
                  onChange={(event) => setValueText(event.target.value)}
                  maxLength={300}
                  placeholder="Negative"
                  className="mt-1 block h-9 w-full rounded border border-outline-variant bg-surface px-2 text-sm"
                />
              </div>
              <div>
                <label className="text-xs font-bold text-on-surface-variant" htmlFor="value-qcode">
                  Code value
                </label>
                <input
                  id="value-qcode"
                  value={valueCode}
                  onChange={(event) => setValueCode(event.target.value)}
                  maxLength={100}
                  placeholder="NEG"
                  className="mt-1 block h-9 w-full rounded border border-outline-variant bg-surface px-2 text-sm"
                />
              </div>
            </div>
          )}

          {resultType === "CODED" && (
            <div>
              <label className="text-xs font-bold text-on-surface-variant" htmlFor="value-code">
                Coded value
              </label>
              <input
                id="value-code"
                value={valueCode}
                onChange={(event) => setValueCode(event.target.value)}
                maxLength={100}
                placeholder="DETECTED"
                className="mt-1 block h-9 w-full rounded border border-outline-variant bg-surface px-2 text-sm"
              />
            </div>
          )}

          {test.data && (
            <p className="text-xs text-on-surface-variant">
              Reference: {referenceLabel(test.data)}
              {test.data.unit ? ` ${test.data.unit}` : ""}
            </p>
          )}

          <div>
            <label className="text-xs font-bold text-on-surface-variant" htmlFor="abnormal-flag">
              Abnormal flag
            </label>
            <select
              id="abnormal-flag"
              value={abnormalFlag}
              onChange={(event) => setAbnormalFlag(event.target.value)}
              className="mt-1 block h-9 w-full rounded border border-outline-variant bg-surface px-2 text-sm"
            >
              <option value="">Auto (from reference range)</option>
              {ABNORMAL_FLAGS.map((flag) => (
                <option key={flag} value={flag}>
                  {flag.replace(/_/g, " ")}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="text-xs font-bold text-on-surface-variant" htmlFor="result-comments">
              Comments (optional)
            </label>
            <textarea
              id="result-comments"
              value={comments}
              onChange={(event) => setComments(event.target.value)}
              maxLength={1000}
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
            disabled={saving || !resultType}
            onClick={submit}
            className="h-8 rounded-md bg-primary px-3 text-xs font-bold text-on-primary disabled:opacity-50"
          >
            {saving ? "Saving…" : submitLabel}
          </button>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
