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
import { useCreateLabOrderItem, useUpdateLabOrderItem } from "../hooks/use-lab-orders";
import { LabOrderItem } from "../types/lab-order.types";

interface LabOrderItemDialogProps {
  open: boolean;
  encounterUuid: string;
  labOrderUuid: string;
  /** null = add mode */
  item: LabOrderItem | null;
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

export function LabOrderItemDialog({
  open,
  encounterUuid,
  labOrderUuid,
  item,
  onClose,
}: LabOrderItemDialogProps) {
  const queryClient = useQueryClient();
  const editing = item !== null;
  const createItem = useCreateLabOrderItem(encounterUuid, labOrderUuid);
  const updateItem = useUpdateLabOrderItem(encounterUuid, labOrderUuid);

  const [selectedTest, setSelectedTest] = useState<LabTestSummary | null>(null);
  const [instructions, setInstructions] = useState("");
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
    setInstructions(item?.instructions ?? "");
    setSelectedTest(
      item
        ? {
            uuid: item.labTestUuid,
            code: item.labTestCode,
            name: item.labTestName,
            resultType: "NUMERIC",
            isActive: true,
          }
        : null,
    );
  }, [open, item]);

  const submit = async () => {
    setError("");
    if (!selectedTest) {
      setError("Choose a lab test.");
      return;
    }
    setSaving(true);
    try {
      if (editing) {
        await updateItem.mutateAsync({
          itemUuid: item!.uuid,
          payload: {
            labTestUuid: selectedTest.uuid,
            instructions: instructions.trim() || undefined,
            version: item?.version,
          },
        });
      } else {
        await createItem.mutateAsync({
          labTestUuid: selectedTest.uuid,
          instructions: instructions.trim() || undefined,
        });
      }
      onClose();
    } catch (caught: unknown) {
      const response = (caught as { response?: { status?: number } })?.response;
      setError(describeError(caught, "Unable to save the lab test."));
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
          <AlertDialogTitle>{editing ? "Edit Ordered Test" : "Add Test"}</AlertDialogTitle>
          <AlertDialogDescription>
            {editing
              ? "Swap the test or update the instructions for this line."
              : "Search the active lab test catalog."}
          </AlertDialogDescription>
        </AlertDialogHeader>

        <div className="space-y-3">
          <div>
            <label className="text-xs font-bold text-on-surface-variant" htmlFor="item-test-search">
              Lab test
            </label>
            <input
              id="item-test-search"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Search by name, code, or category…"
              autoComplete="off"
              disabled={false}
              className="mt-1 block h-9 w-full rounded border border-outline-variant bg-surface px-2 text-sm"
            />

            {selectedTest && !debouncedQuery.trim() && (
              <div className="mt-2 flex items-center justify-between rounded border border-primary/40 bg-primary/5 px-2 py-1.5 text-xs">
                <span className="text-on-surface">
                  <span className="font-semibold">{selectedTest.code}</span> · {selectedTest.name}
                </span>
                <button
                  type="button"
                  onClick={() => setSelectedTest(null)}
                  className="ml-2 font-bold text-error hover:underline"
                >
                  ×
                </button>
              </div>
            )}

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
                      onClick={() => {
                        setSelectedTest(test);
                        setQuery("");
                        setDebouncedQuery("");
                      }}
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
          </div>

          <div>
            <label className="text-xs font-bold text-on-surface-variant" htmlFor="item-instructions">
              Line instructions
            </label>
            <textarea
              id="item-instructions"
              value={instructions}
              onChange={(event) => setInstructions(event.target.value)}
              maxLength={500}
              placeholder="Optional note for this test…"
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
            {saving ? "Saving…" : editing ? "Save Changes" : "Add Test"}
          </button>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
