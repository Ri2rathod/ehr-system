"use client";

import { ReactNode, useState } from "react";
import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { formatAppointmentDate } from "@/modules/appointments/utils/appointment-format";
import {
  LabResult,
  LabResultStatus,
  LabResultValue,
} from "@/modules/lab-results/types/lab-result.types";
import { LabResultDialog, LabResultDialogMode } from "@/modules/lab-results/components/lab-result-dialog";
import { useLabResults, useLabResultAction } from "@/modules/lab-results/hooks/use-lab-results";
import {
  useDeleteLabOrder,
  useDeleteLabOrderItem,
  useLabOrder,
  useLabOrderAction,
  useSpecimens,
  useSpecimenAction,
} from "../hooks/use-lab-orders";
import {
  LabOrderAction,
  LabOrderItem,
  LabOrderItemStatus,
  LabOrderPriority,
  LabOrderStatus,
  LabOrderSummary,
  Specimen,
  SpecimenAction,
  SpecimenStatus,
} from "../types/lab-order.types";
import { LabOrderItemDialog } from "./lab-order-item-dialog";
import { SpecimenDialog, SpecimenRejectDialog } from "./specimen-dialog";

const orderStatusStyles: Record<LabOrderStatus, string> = {
  DRAFT: "bg-surface-container-high text-on-surface-variant",
  ORDERED: "bg-blue-100 text-blue-900",
  IN_PROGRESS: "bg-amber-100 text-amber-900",
  COMPLETED: "bg-emerald-100 text-emerald-900",
  CANCELLED: "bg-red-100 text-red-900",
};

const priorityStyles: Record<LabOrderPriority, string> = {
  ROUTINE: "bg-surface-container-high text-on-surface-variant",
  URGENT: "bg-amber-100 text-amber-900",
  STAT: "bg-red-100 text-red-900",
};

const itemStatusStyles: Record<LabOrderItemStatus, string> = {
  ORDERED: "bg-surface-container-high text-on-surface-variant",
  IN_PROGRESS: "bg-amber-100 text-amber-900",
  COMPLETED: "bg-emerald-100 text-emerald-900",
  CANCELLED: "bg-red-100 text-red-900",
};

const specimenStatusStyles: Record<SpecimenStatus, string> = {
  PENDING_COLLECTION: "bg-amber-100 text-amber-900",
  COLLECTED: "bg-blue-100 text-blue-900",
  RECEIVED: "bg-emerald-100 text-emerald-900",
  REJECTED: "bg-red-100 text-red-900",
  CANCELLED: "bg-surface-container-high text-on-surface-variant",
};

const resultStatusStyles: Record<LabResultStatus, string> = {
  PRELIMINARY: "bg-amber-100 text-amber-900",
  FINAL: "bg-emerald-100 text-emerald-900",
  CORRECTED: "bg-blue-100 text-blue-900",
  CANCELLED: "bg-red-100 text-red-900",
};

function Badge({ className, children }: { className: string; children: ReactNode }) {
  return (
    <span className={`rounded-full px-2 py-0.5 text-[10px] font-bold tracking-wide ${className}`}>
      {children}
    </span>
  );
}

const smallButton =
  "rounded border border-outline-variant px-2 py-1 text-[10px] font-bold text-primary hover:bg-primary/5 disabled:opacity-50";

function describeError(caught: unknown, fallback: string): string {
  const response = (caught as { response?: { status?: number; data?: { message?: string } } })
    ?.response;
  const message = String(response?.data?.message || "");
  if (response?.status === 403) return "You do not have permission to perform this action.";
  if (message) return message;
  return fallback;
}

function valueLabel(value: LabResultValue): string {
  if (value.valueNumeric != null) {
    return `${value.valueNumeric}${value.unit ? ` ${value.unit}` : ""}`;
  }
  if (value.valueText) return value.valueText;
  if (value.valueCode) return value.valueCode;
  return "—";
}

function flagBadgeClass(flag?: string | null): string {
  if (!flag) return "bg-surface-container-high text-on-surface-variant";
  if (flag.startsWith("CRITICAL")) return "bg-red-100 text-red-900";
  if (flag === "NORMAL") return "bg-emerald-100 text-emerald-900";
  return "bg-amber-100 text-amber-900";
}

interface LabOrderCardProps {
  encounterUuid: string;
  summary: LabOrderSummary;
  editable: boolean;
  onEdit: (order: LabOrderSummary) => void;
}

interface ConfirmState {
  title: string;
  description: string;
  confirmLabel: string;
  run: () => Promise<unknown>;
}

export function LabOrderCard({ encounterUuid, summary, editable, onEdit }: LabOrderCardProps) {
  const detail = useLabOrder(encounterUuid, summary.uuid);
  const specimens = useSpecimens(summary.uuid);
  const results = useLabResults(summary.uuid);

  const orderAction = useLabOrderAction(encounterUuid, summary.uuid);
  const deleteOrder = useDeleteLabOrder(encounterUuid, summary.uuid);
  const deleteItem = useDeleteLabOrderItem(encounterUuid, summary.uuid);
  const specimenAction = useSpecimenAction(summary.uuid);
  const resultAction = useLabResultAction(summary.uuid);

  const [itemDialogOpen, setItemDialogOpen] = useState(false);
  const [editingItem, setEditingItem] = useState<LabOrderItem | null>(null);
  const [specimenDialogOpen, setSpecimenDialogOpen] = useState(false);
  const [rejectTarget, setRejectTarget] = useState<Specimen | null>(null);
  const [resultDialogOpen, setResultDialogOpen] = useState(false);
  const [resultDialogMode, setResultDialogMode] = useState<LabResultDialogMode>("create");
  const [editingResult, setEditingResult] = useState<LabResult | null>(null);
  const [confirm, setConfirm] = useState<ConfirmState | null>(null);
  const [actionError, setActionError] = useState("");
  const [busy, setBusy] = useState(false);

  const status = summary.status;
  const terminal = status === "COMPLETED" || status === "CANCELLED";
  const canAddItem = editable && status === "DRAFT";
  const canEditOrder = editable && !terminal;
  const canDeleteOrder = editable && !terminal;
  const canPlace = status === "DRAFT";
  const canStart = status === "ORDERED";
  const canComplete = status === "ORDERED" || status === "IN_PROGRESS";
  const canCancelOrder = !terminal;
  const canAddSpecimen = status === "ORDERED" || status === "IN_PROGRESS";
  const canRecordResult =
    status === "ORDERED" || status === "IN_PROGRESS" || status === "COMPLETED";

  const items = detail.data?.items ?? [];
  const specimenList = specimens.data ?? [];
  const resultList = results.data ?? [];
  const receivedSpecimens = specimenList.filter((entry) => entry.status === "RECEIVED");

  const runOrderAction = async (action: LabOrderAction) => {
    setActionError("");
    try {
      await orderAction.mutateAsync(action);
    } catch (caught: unknown) {
      setActionError(describeError(caught, "The lab order action failed."));
    }
  };

  const runSpecimenAction = async (specimen: Specimen, action: SpecimenAction) => {
    setActionError("");
    try {
      await specimenAction.mutateAsync({ specimenUuid: specimen.uuid, action });
    } catch (caught: unknown) {
      setActionError(describeError(caught, "The specimen action failed."));
    }
  };

  const runConfirm = async () => {
    if (!confirm) return;
    setBusy(true);
    setActionError("");
    try {
      await confirm.run();
      setConfirm(null);
    } catch (caught: unknown) {
      setActionError(describeError(caught, "The action failed."));
    } finally {
      setBusy(false);
    }
  };

  const confirmAction = (action: LabOrderAction, title: string, description: string) => {
    setConfirm({
      title,
      description,
      confirmLabel: title,
      run: async () => {
        await orderAction.mutateAsync(action);
      },
    });
  };

  return (
    <article className="rounded-lg border border-outline-variant bg-surface-container-lowest p-3">
      <div className="flex flex-wrap items-start justify-between gap-2">
        <div className="flex flex-wrap items-center gap-2">
          <h3 className="text-sm font-bold text-on-surface">{summary.orderNumber}</h3>
          <Badge className={orderStatusStyles[status]}>{status.replace(/_/g, " ")}</Badge>
          <Badge className={priorityStyles[summary.priority]}>{summary.priority}</Badge>
        </div>
        <div className="flex flex-wrap gap-1.5">
          {canPlace && (
            <button
              type="button"
              disabled={orderAction.isPending || summary.itemCount === 0}
              title={summary.itemCount === 0 ? "Add at least one test first" : undefined}
              onClick={() => runOrderAction("order")}
              className={smallButton}
            >
              Place Order
            </button>
          )}
          {canStart && (
            <button
              type="button"
              disabled={orderAction.isPending}
              onClick={() => runOrderAction("start")}
              className={smallButton}
            >
              Start
            </button>
          )}
          {canComplete && (
            <button
              type="button"
              disabled={orderAction.isPending}
              onClick={() =>
                confirmAction("complete", "Complete Order", "Mark this lab order as completed?")
              }
              className={smallButton}
            >
              Complete
            </button>
          )}
          {canCancelOrder && (
            <button
              type="button"
              disabled={orderAction.isPending}
              onClick={() =>
                confirmAction("cancel", "Cancel Order", "Cancel this lab order and its active tests?")
              }
              className={smallButton}
            >
              Cancel Order
            </button>
          )}
        </div>
      </div>

      <p className="mt-1 text-xs text-on-surface-variant">
        {summary.orderedAt ? `Ordered ${formatAppointmentDate(summary.orderedAt)} · ` : "Not placed · "}
        {summary.itemCount} test{summary.itemCount === 1 ? "" : "s"} · {summary.resultCount} result
        {summary.resultCount === 1 ? "" : "s"} ({summary.finalizedResultCount} final)
      </p>
      {summary.instructions && (
        <p className="mt-1 text-xs italic text-on-surface-variant">{summary.instructions}</p>
      )}

      {/* Tests */}
      <div className="mt-3">
        <div className="mb-1 flex items-center justify-between">
          <h4 className="text-[11px] font-bold uppercase tracking-wider text-on-surface-variant">
            Tests
          </h4>
          {canAddItem && (
            <button
              type="button"
              onClick={() => {
                setEditingItem(null);
                setItemDialogOpen(true);
              }}
              className={smallButton}
            >
              + Add Test
            </button>
          )}
        </div>
        {detail.isLoading ? (
          <div className="h-10 animate-pulse rounded bg-surface-container" />
        ) : items.length === 0 ? (
          <p className="text-xs text-on-surface-variant">No tests on this order yet.</p>
        ) : (
          <ul className="space-y-1">
            {items.map((item) => (
              <li
                key={item.uuid}
                className="flex flex-wrap items-center justify-between gap-2 rounded border border-outline-variant px-2 py-1.5 text-xs"
              >
                <span className="text-on-surface">
                  <span className="font-semibold">{item.labTestCode}</span> · {item.labTestName}
                </span>
                <span className="flex items-center gap-1.5">
                  <Badge className={itemStatusStyles[item.status]}>
                    {item.status.replace(/_/g, " ")}
                  </Badge>
                  {canAddItem && (
                    <>
                      <button
                        type="button"
                        onClick={() => {
                          setEditingItem(item);
                          setItemDialogOpen(true);
                        }}
                        className={smallButton}
                      >
                        Edit
                      </button>
                      <button
                        type="button"
                        disabled={deleteItem.isPending}
                        onClick={() =>
                          setConfirm({
                            title: "Remove Test",
                            description: `Remove ${item.labTestName} from this order?`,
                            confirmLabel: "Remove",
                            run: async () => {
                              await deleteItem.mutateAsync(item.uuid);
                            },
                          })
                        }
                        className={smallButton}
                      >
                        Remove
                      </button>
                    </>
                  )}
                </span>
              </li>
            ))}
          </ul>
        )}
      </div>

      {/* Specimens */}
      <div className="mt-3">
        <div className="mb-1 flex items-center justify-between">
          <h4 className="text-[11px] font-bold uppercase tracking-wider text-on-surface-variant">
            Specimens
          </h4>
          {canAddSpecimen && (
            <button
              type="button"
              onClick={() => setSpecimenDialogOpen(true)}
              className={smallButton}
            >
              + Register Specimen
            </button>
          )}
        </div>
        {specimens.isLoading ? (
          <div className="h-10 animate-pulse rounded bg-surface-container" />
        ) : specimenList.length === 0 ? (
          <p className="text-xs text-on-surface-variant">No specimens registered.</p>
        ) : (
          <ul className="space-y-1">
            {specimenList.map((specimen) => (
              <li
                key={specimen.uuid}
                className="flex flex-wrap items-center justify-between gap-2 rounded border border-outline-variant px-2 py-1.5 text-xs"
              >
                <span className="text-on-surface">
                  <span className="font-semibold">{specimen.specimenType}</span>
                  {specimen.specimenIdentifier ? ` · ${specimen.specimenIdentifier}` : ""}
                  {specimen.rejectionReason ? (
                    <span className="text-on-surface-variant"> — {specimen.rejectionReason}</span>
                  ) : null}
                </span>
                <span className="flex items-center gap-1.5">
                  <Badge className={specimenStatusStyles[specimen.status]}>
                    {specimen.status.replace(/_/g, " ")}
                  </Badge>
                  {specimen.status === "PENDING_COLLECTION" && (
                    <button
                      type="button"
                      disabled={specimenAction.isPending}
                      onClick={() => runSpecimenAction(specimen, "collect")}
                      className={smallButton}
                    >
                      Collect
                    </button>
                  )}
                  {specimen.status === "COLLECTED" && (
                    <button
                      type="button"
                      disabled={specimenAction.isPending}
                      onClick={() => runSpecimenAction(specimen, "receive")}
                      className={smallButton}
                    >
                      Receive
                    </button>
                  )}
                  {(specimen.status === "PENDING_COLLECTION" ||
                    specimen.status === "COLLECTED") && (
                    <>
                      <button
                        type="button"
                        onClick={() => setRejectTarget(specimen)}
                        className={smallButton}
                      >
                        Reject
                      </button>
                      <button
                        type="button"
                        disabled={specimenAction.isPending}
                        onClick={() =>
                          setConfirm({
                            title: "Cancel Specimen",
                            description: "Cancel this specimen? It can no longer be collected.",
                            confirmLabel: "Cancel Specimen",
                            run: async () => {
                              await specimenAction.mutateAsync({
                                specimenUuid: specimen.uuid,
                                action: "cancel",
                              });
                            },
                          })
                        }
                        className={smallButton}
                      >
                        Cancel
                      </button>
                    </>
                  )}
                </span>
              </li>
            ))}
          </ul>
        )}
      </div>

      {/* Results */}
      <div className="mt-3">
        <div className="mb-1 flex items-center justify-between">
          <h4 className="text-[11px] font-bold uppercase tracking-wider text-on-surface-variant">
            Results
          </h4>
          {canRecordResult && (
            <button
              type="button"
              disabled={items.length === 0 || receivedSpecimens.length === 0}
              title={
                receivedSpecimens.length === 0
                  ? "Receive a specimen before entering results"
                  : items.length === 0
                    ? "Add a test first"
                    : undefined
              }
              onClick={() => {
                setResultDialogMode("create");
                setEditingResult(null);
                setResultDialogOpen(true);
              }}
              className={smallButton}
            >
              + Record Result
            </button>
          )}
        </div>
        {results.isLoading ? (
          <div className="h-10 animate-pulse rounded bg-surface-container" />
        ) : resultList.length === 0 ? (
          <p className="text-xs text-on-surface-variant">No results recorded.</p>
        ) : (
          <ul className="space-y-1">
            {resultList.map((result) => (
              <li
                key={result.uuid}
                className="flex flex-wrap items-center justify-between gap-2 rounded border border-outline-variant px-2 py-1.5 text-xs"
              >
                <span className="text-on-surface">
                  <span className="font-semibold">{result.labTestName}</span>
                  {result.values.map((value) => (
                    <span key={value.uuid} className="ml-2 text-on-surface-variant">
                      {valueLabel(value)}
                      {value.abnormalFlag ? (
                        <Badge className={`ml-1.5 ${flagBadgeClass(value.abnormalFlag)}`}>
                          {value.abnormalFlag.replace(/_/g, " ")}
                        </Badge>
                      ) : null}
                    </span>
                  ))}
                  {result.correctionReason ? (
                    <span className="block text-[10px] text-on-surface-variant">
                      Corrected: {result.correctionReason}
                    </span>
                  ) : null}
                </span>
                <span className="flex items-center gap-1.5">
                  <Badge className={resultStatusStyles[result.status]}>{result.status}</Badge>
                  {result.status === "PRELIMINARY" && (
                    <>
                      <button
                        type="button"
                        onClick={() => {
                          setResultDialogMode("edit");
                          setEditingResult(result);
                          setResultDialogOpen(true);
                        }}
                        className={smallButton}
                      >
                        Edit
                      </button>
                      <button
                        type="button"
                        onClick={() =>
                          setConfirm({
                            title: "Finalize Result",
                            description: `Finalize the result for ${result.labTestName}? Finalized results can only be corrected.`,
                            confirmLabel: "Finalize",
                            run: async () => {
                              await resultAction.mutateAsync({
                                resultUuid: result.uuid,
                                action: "finalize",
                              });
                            },
                          })
                        }
                        className={smallButton}
                      >
                        Finalize
                      </button>
                      <button
                        type="button"
                        onClick={() =>
                          setConfirm({
                            title: "Cancel Result",
                            description: `Cancel the preliminary result for ${result.labTestName}?`,
                            confirmLabel: "Cancel Result",
                            run: async () => {
                              await resultAction.mutateAsync({
                                resultUuid: result.uuid,
                                action: "cancel",
                              });
                            },
                          })
                        }
                        className={smallButton}
                      >
                        Cancel
                      </button>
                    </>
                  )}
                  {result.status === "FINAL" && (
                    <button
                      type="button"
                      onClick={() => {
                        setResultDialogMode("correct");
                        setEditingResult(result);
                        setResultDialogOpen(true);
                      }}
                      className={smallButton}
                    >
                      Correct
                    </button>
                  )}
                </span>
              </li>
            ))}
          </ul>
        )}
      </div>

      {actionError && (
        <p role="alert" className="mt-2 text-xs font-semibold text-error">
          {actionError}
        </p>
      )}

      {(canEditOrder || canDeleteOrder) && (
        <div className="mt-3 flex flex-wrap gap-1.5 border-t border-outline-variant pt-2">
          {canEditOrder && (
            <button type="button" onClick={() => onEdit(summary)} className={smallButton}>
              Edit Order
            </button>
          )}
          {canDeleteOrder && (
            <button
              type="button"
              onClick={() =>
                setConfirm({
                  title: "Delete Lab Order",
                  description:
                    "Delete this draft lab order? Its tests, specimens, and non-final results are removed with it.",
                  confirmLabel: "Delete",
                  run: async () => {
                    await deleteOrder.mutateAsync();
                  },
                })
              }
              className={smallButton}
            >
              Delete Order
            </button>
          )}
        </div>
      )}

      <LabOrderItemDialog
        open={itemDialogOpen}
        encounterUuid={encounterUuid}
        labOrderUuid={summary.uuid}
        item={editingItem}
        onClose={() => setItemDialogOpen(false)}
      />
      <SpecimenDialog
        open={specimenDialogOpen}
        labOrderUuid={summary.uuid}
        onClose={() => setSpecimenDialogOpen(false)}
      />
      <SpecimenRejectDialog
        open={Boolean(rejectTarget)}
        labOrderUuid={summary.uuid}
        specimen={rejectTarget}
        onClose={() => setRejectTarget(null)}
      />
      <LabResultDialog
        open={resultDialogOpen}
        labOrderUuid={summary.uuid}
        mode={resultDialogMode}
        result={editingResult}
        items={items}
        specimens={specimenList}
        onClose={() => setResultDialogOpen(false)}
      />
      <AlertDialog open={Boolean(confirm)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>{confirm?.title}</AlertDialogTitle>
            <AlertDialogDescription>{confirm?.description}</AlertDialogDescription>
          </AlertDialogHeader>
          {actionError && (
            <p role="alert" className="mt-2 text-xs font-semibold text-error">
              {actionError}
            </p>
          )}
          <AlertDialogFooter>
            <button
              onClick={() => {
                setConfirm(null);
                setActionError("");
              }}
              className="h-8 rounded-md border border-outline-variant px-3 text-xs font-semibold"
            >
              Keep
            </button>
            <button
              disabled={busy}
              onClick={runConfirm}
              className="h-8 rounded-md bg-error px-3 text-xs font-bold text-on-error disabled:opacity-50"
            >
              {busy ? "Working…" : confirm?.confirmLabel}
            </button>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </article>
  );
}
