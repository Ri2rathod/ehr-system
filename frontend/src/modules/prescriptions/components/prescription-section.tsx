"use client";

import { useState } from "react";
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
  useDeletePrescription,
  useDeletePrescriptionItem,
  usePrescriptionAction,
  usePrescriptionItemAction,
  usePrescriptionItems,
  usePrescriptions,
  PrescriptionAction,
  PrescriptionItemAction,
} from "../hooks/use-prescriptions";
import {
  DoseUnit,
  DurationUnit,
  FrequencyUnit,
  MedicationFrequency,
  Prescription,
  PrescriptionItem,
  PrescriptionItemStatus,
  PrescriptionStatus,
  QuantityUnit,
  Route,
} from "../types/prescription.types";
import { PrescriptionDialog } from "./prescription-dialog";
import { PrescriptionItemDialog } from "./prescription-item-dialog";

interface PrescriptionSectionProps {
  encounterUuid: string;
  editable: boolean;
}

const rxStatusStyles: Record<PrescriptionStatus, string> = {
  DRAFT: "bg-surface-container-high text-on-surface-variant",
  ACTIVE: "bg-emerald-100 text-emerald-900",
  COMPLETED: "bg-sky-100 text-sky-900",
  CANCELLED: "bg-amber-100 text-amber-900",
  VOID: "bg-error/10 text-error",
};

const rxStatusLabel: Record<PrescriptionStatus, string> = {
  DRAFT: "Draft",
  ACTIVE: "Active",
  COMPLETED: "Completed",
  CANCELLED: "Cancelled",
  VOID: "Void",
};

const itemStatusStyles: Record<PrescriptionItemStatus, string> = {
  ACTIVE: "bg-emerald-100 text-emerald-900",
  COMPLETED: "bg-sky-100 text-sky-900",
  CANCELLED: "bg-amber-100 text-amber-900",
  DISCONTINUED: "bg-surface-container-high text-on-surface-variant",
};

const itemStatusLabel: Record<PrescriptionItemStatus, string> = {
  ACTIVE: "Active",
  COMPLETED: "Completed",
  CANCELLED: "Cancelled",
  DISCONTINUED: "Discontinued",
};

const frequencyLabel: Record<MedicationFrequency, string> = {
  ONCE_DAILY: "Once daily",
  TWICE_DAILY: "Twice daily",
  THREE_TIMES_DAILY: "Three times daily",
  FOUR_TIMES_DAILY: "Four times daily",
  EVERY_MORNING: "Every morning",
  EVERY_EVENING: "Every evening",
  AT_BEDTIME: "At bedtime",
  WEEKLY: "Weekly",
  AS_NEEDED: "As needed",
  CUSTOM: "Custom",
};

const routeLabel: Record<Route, string> = {
  ORAL: "Oral",
  TOPICAL: "Topical",
  INTRAVENOUS: "IV",
  INTRAMUSCULAR: "IM",
  SUBCUTANEOUS: "Subcutaneous",
  INHALATION: "Inhalation",
  OPHTHALMIC: "Ophthalmic",
  OTIC: "Otic",
  NASAL: "Nasal",
  RECTAL: "Rectal",
  OTHER: "Other",
};

const doseUnitLabel: Record<DoseUnit, string> = {
  MG: "mg",
  MCG: "mcg",
  G: "g",
  ML: "mL",
  IU: "IU",
  MEQ: "mEq",
  PERCENT: "%",
  PUFF: "puff",
  DROP: "drop",
  UNIT: "unit",
  OTHER: "other",
};

const frequencyUnitLabel: Record<FrequencyUnit, string> = {
  HOURS: "hours",
  DAYS: "days",
};

const durationUnitLabel: Record<DurationUnit, string> = {
  DAYS: "days",
  WEEKS: "weeks",
  MONTHS: "months",
};

const quantityUnitLabel: Record<QuantityUnit, string> = {
  TABLETS: "tablets",
  CAPSULES: "capsules",
  ML: "mL",
  G: "g",
  PUFFS: "puffs",
  DROPS: "drops",
  UNITS: "units",
  OTHER: "other",
};

function describeError(caught: unknown, fallback: string): string {
  const response = (caught as { response?: { status?: number; data?: { message?: string } } })
    ?.response;
  const message = String(response?.data?.message || "");
  if (response?.status === 403) return "You do not have permission to perform this action.";
  if (message) return message;
  return fallback;
}

function describeDose(item: PrescriptionItem): string {
  return `${item.dose} ${doseUnitLabel[item.doseUnit] || item.doseUnit} · ${
    routeLabel[item.route] || item.route
  } · ${frequencyLabel[item.frequency] || item.frequency}`;
}

function describeExtras(item: PrescriptionItem): string[] {
  const parts: string[] = [];
  if (item.frequencyValue && item.frequencyUnit) {
    parts.push(`Every ${item.frequencyValue} ${frequencyUnitLabel[item.frequencyUnit]}`);
  }
  if (item.duration && item.durationUnit) {
    parts.push(`For ${item.duration} ${durationUnitLabel[item.durationUnit]}`);
  }
  if (item.quantity && item.quantityUnit) {
    parts.push(`Qty ${item.quantity} ${quantityUnitLabel[item.quantityUnit]}`);
  }
  if (item.refills) parts.push(`${item.refills} refill${item.refills > 1 ? "s" : ""}`);
  if (item.startDate || item.endDate) {
    if (item.startDate && item.endDate) {
      parts.push(`${formatAppointmentDate(item.startDate)} – ${formatAppointmentDate(item.endDate)}`);
    } else if (item.startDate) {
      parts.push(`From ${formatAppointmentDate(item.startDate)}`);
    } else if (item.endDate) {
      parts.push(`Until ${formatAppointmentDate(item.endDate)}`);
    }
  }
  return parts;
}

export function PrescriptionSection({ encounterUuid, editable }: PrescriptionSectionProps) {
  const { data: prescriptions, isLoading, isError, refetch } = usePrescriptions(encounterUuid);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<Prescription | null>(null);

  const openCreate = () => {
    setEditing(null);
    setDialogOpen(true);
  };

  return (
    <section className="rounded-lg border border-outline-variant bg-surface-container-lowest p-4">
      <div className="mb-1 flex flex-wrap items-center justify-between gap-2">
        <div className="flex items-center gap-2">
          <h2 className="text-xs font-bold uppercase tracking-wider text-on-surface-variant">
            Prescriptions
          </h2>
          {!editable && (
            <span className="rounded-full bg-surface-container-high px-2 py-0.5 text-[10px] font-bold text-on-surface-variant">
              Read only
            </span>
          )}
        </div>
        {editable && !isLoading && !isError && (
          <button
            onClick={openCreate}
            className="h-8 rounded bg-primary px-3 text-xs font-bold text-on-primary"
          >
            + New Prescription
          </button>
        )}
      </div>
      <p className="mb-3 text-xs text-on-surface-variant">
        Medications prescribed for this visit. Activate a prescription after adding medications.
      </p>

      {isLoading ? (
        <div className="space-y-2">
          {[0, 1, 2].map((index) => (
            <div key={index} className="h-16 animate-pulse rounded bg-surface-container" />
          ))}
        </div>
      ) : isError ? (
        <div>
          <p className="text-sm font-bold text-on-surface">Unable to load prescriptions</p>
          <button
            onClick={() => refetch()}
            className="mt-2 h-8 rounded border border-outline-variant px-3 text-xs font-bold text-primary"
          >
            Try again
          </button>
        </div>
      ) : !prescriptions?.length ? (
        <div>
          <p className="text-sm font-bold text-on-surface">No prescriptions for this encounter</p>
          <p className="mt-1 text-xs text-on-surface-variant">
            Create a prescription, add medications, then activate it.
          </p>
          {editable && (
            <button
              onClick={openCreate}
              className="mt-3 h-8 rounded-md bg-primary px-3 text-xs font-bold text-on-primary"
            >
              + New Prescription
            </button>
          )}
        </div>
      ) : (
        <div className="space-y-4">
          {prescriptions.map((prescription) => (
            <PrescriptionCard
              key={prescription.uuid}
              encounterUuid={encounterUuid}
              prescription={prescription}
              editable={editable}
              onEditPrescription={(target) => {
                setEditing(target);
                setDialogOpen(true);
              }}
            />
          ))}
        </div>
      )}

      <PrescriptionDialog
        open={dialogOpen}
        encounterUuid={encounterUuid}
        prescription={editing}
        onClose={() => setDialogOpen(false)}
      />
    </section>
  );
}

interface PrescriptionCardProps {
  encounterUuid: string;
  prescription: Prescription;
  editable: boolean;
  onEditPrescription: (prescription: Prescription) => void;
}

function PrescriptionCard({
  encounterUuid,
  prescription,
  editable,
  onEditPrescription,
}: PrescriptionCardProps) {
  const { data: items, isLoading: itemsLoading } = usePrescriptionItems(
    encounterUuid,
    prescription.uuid,
  );
  const rxAction = usePrescriptionAction(encounterUuid);
  const deletePrescription = useDeletePrescription(encounterUuid);
  const itemAction = usePrescriptionItemAction(encounterUuid);
  const deleteItem = useDeletePrescriptionItem(encounterUuid);

  const [actionError, setActionError] = useState("");
  const [cancelRxOpen, setCancelRxOpen] = useState(false);
  const [cancelRxError, setCancelRxError] = useState("");
  const [deleteRxOpen, setDeleteRxOpen] = useState(false);
  const [deleteRxError, setDeleteRxError] = useState("");
  const [discontinueTarget, setDiscontinueTarget] = useState<PrescriptionItem | null>(null);
  const [discontinueError, setDiscontinueError] = useState("");
  const [cancelItemTarget, setCancelItemTarget] = useState<PrescriptionItem | null>(null);
  const [cancelItemError, setCancelItemError] = useState("");
  const [deleteItemTarget, setDeleteItemTarget] = useState<PrescriptionItem | null>(null);
  const [deleteItemError, setDeleteItemError] = useState("");
  const [itemDialogOpen, setItemDialogOpen] = useState(false);
  const [editingItem, setEditingItem] = useState<PrescriptionItem | null>(null);

  const rxOpen = prescription.status === "DRAFT" || prescription.status === "ACTIVE";
  const canEdit = editable && rxOpen;
  const canDelete = editable && prescription.status !== "COMPLETED" && prescription.status !== "ACTIVE";

  const runRxAction = async (action: PrescriptionAction) => {
    setActionError("");
    try {
      await rxAction.mutateAsync({ prescriptionUuid: prescription.uuid, action });
    } catch (caught) {
      setActionError(describeError(caught, "Unable to update the prescription."));
    }
  };

  const runItemAction = async (item: PrescriptionItem, action: PrescriptionItemAction) => {
    setActionError("");
    try {
      await itemAction.mutateAsync({
        prescriptionUuid: prescription.uuid,
        itemUuid: item.uuid,
        action,
      });
    } catch (caught) {
      setActionError(describeError(caught, "Unable to update the medication."));
    }
  };

  const handleCancelRx = async () => {
    setCancelRxError("");
    try {
      await rxAction.mutateAsync({ prescriptionUuid: prescription.uuid, action: "cancel" });
      setCancelRxOpen(false);
    } catch (caught) {
      setCancelRxError(describeError(caught, "Unable to cancel the prescription."));
    }
  };

  const handleDeleteRx = async () => {
    setDeleteRxError("");
    try {
      await deletePrescription.mutateAsync(prescription.uuid);
      setDeleteRxOpen(false);
    } catch (caught) {
      setDeleteRxError(describeError(caught, "Unable to delete the prescription."));
    }
  };

  const handleDiscontinueItem = async () => {
    if (!discontinueTarget) return;
    setDiscontinueError("");
    try {
      await itemAction.mutateAsync({
        prescriptionUuid: prescription.uuid,
        itemUuid: discontinueTarget.uuid,
        action: "discontinue",
      });
      setDiscontinueTarget(null);
    } catch (caught) {
      setDiscontinueError(describeError(caught, "Unable to discontinue the medication."));
    }
  };

  const handleCancelItem = async () => {
    if (!cancelItemTarget) return;
    setCancelItemError("");
    try {
      await itemAction.mutateAsync({
        prescriptionUuid: prescription.uuid,
        itemUuid: cancelItemTarget.uuid,
        action: "cancel",
      });
      setCancelItemTarget(null);
    } catch (caught) {
      setCancelItemError(describeError(caught, "Unable to cancel the medication."));
    }
  };

  const handleDeleteItem = async () => {
    if (!deleteItemTarget) return;
    setDeleteItemError("");
    try {
      await deleteItem.mutateAsync({
        prescriptionUuid: prescription.uuid,
        itemUuid: deleteItemTarget.uuid,
      });
      setDeleteItemTarget(null);
    } catch (caught) {
      setDeleteItemError(describeError(caught, "Unable to delete the medication."));
    }
  };

  return (
    <div className="rounded-md border border-outline-variant bg-surface p-3">
      <div className="flex flex-wrap items-start justify-between gap-2">
        <div className="min-w-0 space-y-1">
          <div className="flex flex-wrap items-center gap-2">
            <span
              className={`rounded-full px-2 py-0.5 text-[10px] font-bold tracking-wide ${rxStatusStyles[prescription.status]}`}
            >
              {rxStatusLabel[prescription.status]}
            </span>
            <span className="font-mono text-sm font-bold text-on-surface">
              {prescription.prescriptionNumber}
            </span>
            {prescription.prescribedAt && (
              <span className="text-[11px] text-on-surface-variant">
                {formatAppointmentDate(prescription.prescribedAt)}
              </span>
            )}
          </div>
          {prescription.notes && (
            <p className="text-xs text-on-surface-variant">{prescription.notes}</p>
          )}
        </div>
        {editable && (
          <div className="flex shrink-0 flex-wrap gap-1">
            {prescription.status === "DRAFT" && (
              <button
                disabled={rxAction.isPending}
                onClick={() => runRxAction("activate")}
                className="rounded border border-outline-variant px-2 py-1 text-[10px] font-bold text-primary hover:bg-primary/5 disabled:opacity-50"
              >
                Activate
              </button>
            )}
            {prescription.status === "ACTIVE" && (
              <>
                <button
                  disabled={rxAction.isPending}
                  onClick={() => runRxAction("complete")}
                  className="rounded border border-outline-variant px-2 py-1 text-[10px] font-bold text-primary hover:bg-primary/5 disabled:opacity-50"
                >
                  Complete
                </button>
                <button
                  disabled={rxAction.isPending}
                  onClick={() => runRxAction("void")}
                  className="rounded border border-outline-variant px-2 py-1 text-[10px] font-bold text-error hover:bg-error/5 disabled:opacity-50"
                >
                  Void
                </button>
              </>
            )}
            {rxOpen && (
              <>
                <button
                  onClick={() => onEditPrescription(prescription)}
                  className="rounded border border-outline-variant px-2 py-1 text-[10px] font-bold text-primary hover:bg-primary/5"
                >
                  Edit
                </button>
                <button
                  onClick={() => {
                    setCancelRxError("");
                    setCancelRxOpen(true);
                  }}
                  className="rounded border border-outline-variant px-2 py-1 text-[10px] font-bold text-on-surface-variant hover:bg-surface-container"
                >
                  Cancel
                </button>
              </>
            )}
            {canDelete && (
              <button
                onClick={() => {
                  setDeleteRxError("");
                  setDeleteRxOpen(true);
                }}
                className="rounded border border-outline-variant px-2 py-1 text-[10px] font-bold text-error hover:bg-error/5"
              >
                Delete
              </button>
            )}
          </div>
        )}
      </div>

      {actionError && (
        <p role="alert" className="mt-2 text-xs font-semibold text-error">
          {actionError}
        </p>
      )}

      <div className="mt-3 border-t border-outline-variant pt-3">
        <div className="mb-2 flex flex-wrap items-center justify-between gap-2">
          <p className="text-[11px] font-bold uppercase tracking-wider text-on-surface-variant">
            Medications
          </p>
          {canEdit && (
            <button
              onClick={() => {
                setEditingItem(null);
                setItemDialogOpen(true);
              }}
              className="rounded border border-outline-variant px-2 py-1 text-[10px] font-bold text-primary hover:bg-primary/5"
            >
              + Add Medication
            </button>
          )}
        </div>

        {itemsLoading ? (
          <div className="space-y-2">
            {[0, 1].map((index) => (
              <div key={index} className="h-10 animate-pulse rounded bg-surface-container" />
            ))}
          </div>
        ) : !items?.length ? (
          <p className="text-xs text-on-surface-variant">
            {prescription.status === "DRAFT"
              ? "No medications yet. Add at least one before activating."
              : "No medications in this prescription."}
          </p>
        ) : (
          <div>
            {items.map((item) => {
              const extras = describeExtras(item);
              return (
                <div
                  key={item.uuid}
                  className="flex items-start justify-between gap-3 border-b border-outline-variant py-2 last:border-b-0"
                >
                  <div className="min-w-0 space-y-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <span
                        className={`rounded-full px-2 py-0.5 text-[10px] font-bold tracking-wide ${itemStatusStyles[item.status]}`}
                      >
                        {itemStatusLabel[item.status]}
                      </span>
                      <p className="text-sm font-semibold text-on-surface">
                        {item.medicationGenericName}
                        {item.medicationBrandName ? ` (${item.medicationBrandName})` : ""}
                      </p>
                      {item.medicationCode && (
                        <span className="font-mono text-[10px] text-on-surface-variant">
                          {item.medicationCode}
                        </span>
                      )}
                    </div>
                    <p className="text-[11px] text-on-surface-variant">{describeDose(item)}</p>
                    {extras.length > 0 && (
                      <p className="text-[11px] text-on-surface-variant">{extras.join(" · ")}</p>
                    )}
                    {item.instructions && (
                      <p className="text-[11px] italic text-on-surface-variant">
                        {item.instructions}
                      </p>
                    )}
                  </div>
                  {canEdit && item.status === "ACTIVE" && (
                    <div className="flex shrink-0 flex-wrap gap-1">
                      <button
                        onClick={() => {
                          setEditingItem(item);
                          setItemDialogOpen(true);
                        }}
                        className="rounded border border-outline-variant px-2 py-1 text-[10px] font-bold text-primary hover:bg-primary/5"
                      >
                        Edit
                      </button>
                      <button
                        disabled={itemAction.isPending}
                        onClick={() => runItemAction(item, "complete")}
                        className="rounded border border-outline-variant px-2 py-1 text-[10px] font-bold text-primary hover:bg-primary/5 disabled:opacity-50"
                      >
                        Complete
                      </button>
                      <button
                        onClick={() => {
                          setDiscontinueError("");
                          setDiscontinueTarget(item);
                        }}
                        className="rounded border border-outline-variant px-2 py-1 text-[10px] font-bold text-on-surface-variant hover:bg-surface-container"
                      >
                        Discontinue
                      </button>
                      <button
                        onClick={() => {
                          setCancelItemError("");
                          setCancelItemTarget(item);
                        }}
                        className="rounded border border-outline-variant px-2 py-1 text-[10px] font-bold text-on-surface-variant hover:bg-surface-container"
                      >
                        Cancel
                      </button>
                      <button
                        onClick={() => {
                          setDeleteItemError("");
                          setDeleteItemTarget(item);
                        }}
                        className="rounded border border-outline-variant px-2 py-1 text-[10px] font-bold text-error hover:bg-error/5"
                      >
                        Delete
                      </button>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </div>

      <PrescriptionItemDialog
        open={itemDialogOpen}
        encounterUuid={encounterUuid}
        prescriptionUuid={prescription.uuid}
        item={editingItem}
        onClose={() => setItemDialogOpen(false)}
      />

      <AlertDialog open={cancelRxOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Cancel prescription?</AlertDialogTitle>
            <AlertDialogDescription>
              {prescription.prescriptionNumber} will be marked cancelled. Its history remains
              available for review.
            </AlertDialogDescription>
          </AlertDialogHeader>
          {cancelRxError && (
            <p role="alert" className="mt-3 text-xs font-semibold text-error">
              {cancelRxError}
            </p>
          )}
          <AlertDialogFooter>
            <button
              onClick={() => setCancelRxOpen(false)}
              className="h-8 rounded-md border border-outline-variant px-3 text-xs font-semibold"
            >
              Keep Prescription
            </button>
            <button
              disabled={rxAction.isPending}
              onClick={handleCancelRx}
              className="h-8 rounded-md bg-error px-3 text-xs font-bold text-on-error disabled:opacity-50"
            >
              {rxAction.isPending ? "Cancelling…" : "Cancel Prescription"}
            </button>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      <AlertDialog open={deleteRxOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Delete prescription?</AlertDialogTitle>
            <AlertDialogDescription>
              {prescription.prescriptionNumber} and its medications are removed from this
              encounter. Completed prescriptions cannot be deleted.
            </AlertDialogDescription>
          </AlertDialogHeader>
          {deleteRxError && (
            <p role="alert" className="mt-3 text-xs font-semibold text-error">
              {deleteRxError}
            </p>
          )}
          <AlertDialogFooter>
            <button
              onClick={() => setDeleteRxOpen(false)}
              className="h-8 rounded-md border border-outline-variant px-3 text-xs font-semibold"
            >
              Cancel
            </button>
            <button
              disabled={deletePrescription.isPending}
              onClick={handleDeleteRx}
              className="h-8 rounded-md bg-error px-3 text-xs font-bold text-on-error disabled:opacity-50"
            >
              {deletePrescription.isPending ? "Deleting…" : "Delete"}
            </button>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      <AlertDialog open={Boolean(discontinueTarget)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Discontinue medication?</AlertDialogTitle>
            <AlertDialogDescription>
              {discontinueTarget?.medicationGenericName} will be marked discontinued. The rest of
              this prescription is not affected.
            </AlertDialogDescription>
          </AlertDialogHeader>
          {discontinueError && (
            <p role="alert" className="mt-3 text-xs font-semibold text-error">
              {discontinueError}
            </p>
          )}
          <AlertDialogFooter>
            <button
              onClick={() => setDiscontinueTarget(null)}
              className="h-8 rounded-md border border-outline-variant px-3 text-xs font-semibold"
            >
              Keep
            </button>
            <button
              disabled={itemAction.isPending}
              onClick={handleDiscontinueItem}
              className="h-8 rounded-md bg-error px-3 text-xs font-bold text-on-error disabled:opacity-50"
            >
              {itemAction.isPending ? "Discontinuing…" : "Discontinue"}
            </button>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      <AlertDialog open={Boolean(cancelItemTarget)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Cancel medication?</AlertDialogTitle>
            <AlertDialogDescription>
              {cancelItemTarget?.medicationGenericName} will be marked cancelled.
            </AlertDialogDescription>
          </AlertDialogHeader>
          {cancelItemError && (
            <p role="alert" className="mt-3 text-xs font-semibold text-error">
              {cancelItemError}
            </p>
          )}
          <AlertDialogFooter>
            <button
              onClick={() => setCancelItemTarget(null)}
              className="h-8 rounded-md border border-outline-variant px-3 text-xs font-semibold"
            >
              Keep
            </button>
            <button
              disabled={itemAction.isPending}
              onClick={handleCancelItem}
              className="h-8 rounded-md bg-error px-3 text-xs font-bold text-on-error disabled:opacity-50"
            >
              {itemAction.isPending ? "Cancelling…" : "Cancel Medication"}
            </button>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      <AlertDialog open={Boolean(deleteItemTarget)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Delete medication?</AlertDialogTitle>
            <AlertDialogDescription>
              {deleteItemTarget?.medicationGenericName} will be removed from this prescription.
              Completed medications cannot be deleted.
            </AlertDialogDescription>
          </AlertDialogHeader>
          {deleteItemError && (
            <p role="alert" className="mt-3 text-xs font-semibold text-error">
              {deleteItemError}
            </p>
          )}
          <AlertDialogFooter>
            <button
              onClick={() => setDeleteItemTarget(null)}
              className="h-8 rounded-md border border-outline-variant px-3 text-xs font-semibold"
            >
              Cancel
            </button>
            <button
              disabled={deleteItem.isPending}
              onClick={handleDeleteItem}
              className="h-8 rounded-md bg-error px-3 text-xs font-bold text-on-error disabled:opacity-50"
            >
              {deleteItem.isPending ? "Deleting…" : "Delete"}
            </button>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
