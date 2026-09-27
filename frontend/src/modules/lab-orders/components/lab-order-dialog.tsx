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
import { useLabTestSearch } from "@/modules/lab-tests/hooks/use-lab-tests";
import { LabTestSummary } from "@/modules/lab-tests/types/lab-test.types";
import { useCreateLabOrder, useUpdateLabOrder } from "../hooks/use-lab-orders";
import { LabOrderPriority, LabOrderSummary } from "../types/lab-order.types";

interface LabOrderDialogProps {
  open: boolean;
  encounterUuid: string;
  /** null = create mode */
  order: LabOrderSummary | null;
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

export function LabOrderDialog({ open, encounterUuid, order, onClose }: LabOrderDialogProps) {
  const queryClient = useQueryClient();
  const editing = order !== null;
  const createOrder = useCreateLabOrder(encounterUuid);
  const updateOrder = useUpdateLabOrder(encounterUuid, order?.uuid);

  const [priority, setPriority] = useState<LabOrderPriority>("ROUTINE");
  const [instructions, setInstructions] = useState("");
  const [selectedTests, setSelectedTests] = useState<LabTestSummary[]>([]);
  const [query, setQuery] = useState("");
  const [debouncedQuery, setDebouncedQuery] = useState("");
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  const search = useLabTestSearch(debouncedQuery, { active: true });

  useEffect(() => {
    if (!open) return;
    const handle = window.setTimeout(() => setDebouncedQuery(query), 250);
    return () => window.clearTimeout(handle);
  }, [query, open]);

  useEffect(() => {
    if (!open) return;
    setError("");
    setQuery("");
    setDebouncedQuery("");
    setSelectedTests([]);
    setPriority(order?.priority ?? "ROUTINE");
    setInstructions(order?.instructions ?? "");
  }, [open, order]);

  const addTest = (test: LabTestSummary) => {
    setSelectedTests((current) =>
      current.some((entry) => entry.uuid === test.uuid) ? current : [...current, test],
    );
    setQuery("");
    setDebouncedQuery("");
  };

  const removeTest = (testUuid: string) => {
    setSelectedTests((current) => current.filter((entry) => entry.uuid !== testUuid));
  };

  const submit = async () => {
    setError("");
    if (!editing && selectedTests.length === 0) {
      setError("Select at least one lab test.");
      return;
    }
    setSaving(true);
    try {
      if (editing) {
        await updateOrder.mutateAsync({
          priority,
          instructions: instructions.trim() || undefined,
          version: order?.version,
        });
      } else {
        await createOrder.mutateAsync({
          priority,
          instructions: instructions.trim() || undefined,
          items: selectedTests.map((test) => ({ labTestUuid: test.uuid })),
        });
      }
      onClose();
    } catch (caught: unknown) {
      const response = (caught as { response?: { status?: number } })?.response;
      setError(describeError(caught, "Unable to save the lab order."));
      if (response?.status === 409) {
        queryClient.invalidateQueries({ queryKey: ["encounter-lab-orders"] });
        queryClient.invalidateQueries({ queryKey: ["lab-order"] });
      }
    } finally {
      setSaving(false);
    }
  };

  const results = search.data?.content ?? [];

  return (
    <AlertDialog open={open}>
      <AlertDialogContent>
      <AlertDialogHeader>
        <AlertDialogTitle>{editing ? "Edit Lab Order" : "New Lab Order"}</AlertDialogTitle>
        <AlertDialogDescription>
          {editing
            ? "Update the priority and instructions. Tests can only be changed while the order is a draft."
            : "Pick the diagnostics to order for this encounter."}
        </AlertDialogDescription>
      </AlertDialogHeader>

      <div className="space-y-3">
        <div>
          <label className="text-xs font-bold text-on-surface-variant" htmlFor="lab-order-priority">
            Priority
          </label>
          <select
            id="lab-order-priority"
            value={priority}
            onChange={(event) => setPriority(event.target.value as LabOrderPriority)}
            className="mt-1 block h-9 w-full rounded border border-outline-variant bg-surface px-2 text-sm"
          >
            <option value="ROUTINE">Routine</option>
            <option value="URGENT">Urgent</option>
            <option value="STAT">STAT</option>
          </select>
        </div>

        <div>
          <label className="text-xs font-bold text-on-surface-variant" htmlFor="lab-order-instructions">
            Instructions
          </label>
          <textarea
            id="lab-order-instructions"
            value={instructions}
            onChange={(event) => setInstructions(event.target.value)}
            maxLength={1000}
            placeholder="Fasting sample, preferred draw time…"
            className="mt-1 block w-full rounded border border-outline-variant bg-surface p-2 text-sm"
          />
        </div>

        {!editing && (
          <div>
            <label className="text-xs font-bold text-on-surface-variant" htmlFor="lab-test-search">
              Lab tests
            </label>
            <input
              id="lab-test-search"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Search by name, code, or category…"
              autoComplete="off"
              className="mt-1 block h-9 w-full rounded border border-outline-variant bg-surface px-2 text-sm"
            />

            {debouncedQuery.trim() && (
              <div className="mt-2 max-h-36 overflow-y-auto rounded border border-outline-variant">
                {search.isLoading ? (
                  <p className="p-2 text-xs text-on-surface-variant">Searching…</p>
                ) : search.isError ? (
                  <p className="p-2 text-xs font-semibold text-error">
                    Unable to search lab tests.
                  </p>
                ) : results.length === 0 ? (
                  <p className="p-2 text-xs text-on-surface-variant">No lab tests found.</p>
                ) : (
                  results.map((test) => (
                    <button
                      key={test.uuid}
                      type="button"
                      onClick={() => addTest(test)}
                      className="block w-full border-b border-outline-variant px-2 py-1.5 text-left text-xs last:border-b-0 hover:bg-surface-container"
                    >
                      <span className="font-semibold text-on-surface">{test.name}</span>
                      <span className="ml-2 text-on-surface-variant">{test.code}</span>
                      <span className="ml-2 text-on-surface-variant">· {test.resultType}</span>
                    </button>
                  ))
                )}
              </div>
            )}

            {selectedTests.length > 0 && (
              <div className="mt-2 space-y-1">
                {selectedTests.map((test) => (
                  <div
                    key={test.uuid}
                    className="flex items-center justify-between rounded border border-outline-variant px-2 py-1 text-xs"
                  >
                    <span className="text-on-surface">
                      <span className="font-semibold">{test.code}</span> · {test.name}
                    </span>
                    <button
                      type="button"
                      onClick={() => removeTest(test.uuid)}
                      className="ml-2 font-bold text-error hover:underline"
                    >
                      ×
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
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
          {saving ? "Saving…" : editing ? "Save Changes" : "Create Lab Order"}
        </button>
      </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
