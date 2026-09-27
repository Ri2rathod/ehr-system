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
  useDeleteTreatmentItem,
  useDeleteTreatmentPlan,
  useTreatmentItemAction,
  useTreatmentPlanAction,
  useTreatmentPlanItems,
  useTreatmentPlans,
  TreatmentItemAction,
  TreatmentPlanAction,
} from "../hooks/use-treatment-plans";
import {
  DurationUnit,
  TreatmentItemStatus,
  TreatmentPlan,
  TreatmentPlanItem,
  TreatmentPlanStatus,
  TreatmentPriority,
  TreatmentType,
} from "../types/treatment-plan.types";
import { TreatmentItemDialog } from "./treatment-item-dialog";
import { TreatmentPlanDialog } from "./treatment-plan-dialog";

interface TreatmentPlanSectionProps {
  encounterUuid: string;
  editable: boolean;
}

const planStatusStyles: Record<TreatmentPlanStatus, string> = {
  DRAFT: "bg-surface-container-high text-on-surface-variant",
  ACTIVE: "bg-emerald-100 text-emerald-900",
  COMPLETED: "bg-sky-100 text-sky-900",
  CANCELLED: "bg-amber-100 text-amber-900",
};

const planStatusLabel: Record<TreatmentPlanStatus, string> = {
  DRAFT: "Draft",
  ACTIVE: "Active",
  COMPLETED: "Completed",
  CANCELLED: "Cancelled",
};

const itemStatusStyles: Record<TreatmentItemStatus, string> = {
  PLANNED: "bg-surface-container-high text-on-surface-variant",
  IN_PROGRESS: "bg-emerald-100 text-emerald-900",
  COMPLETED: "bg-sky-100 text-sky-900",
  CANCELLED: "bg-amber-100 text-amber-900",
};

const itemStatusLabel: Record<TreatmentItemStatus, string> = {
  PLANNED: "Planned",
  IN_PROGRESS: "In progress",
  COMPLETED: "Completed",
  CANCELLED: "Cancelled",
};

const priorityStyles: Record<TreatmentPriority, string> = {
  LOW: "bg-surface-container text-on-surface-variant",
  MEDIUM: "bg-surface-container-high text-on-surface-variant",
  HIGH: "bg-error/10 text-error",
};

const treatmentTypeLabel: Record<TreatmentType, string> = {
  LIFESTYLE: "Lifestyle",
  DIET: "Diet",
  EXERCISE: "Exercise",
  PHYSIOTHERAPY: "Physiotherapy",
  BEHAVIORAL: "Behavioral",
  EDUCATION: "Education",
  MONITORING: "Monitoring",
  FOLLOW_UP: "Follow-up",
  PROCEDURE: "Procedure",
  OTHER: "Other",
};

const durationUnitLabel: Record<DurationUnit, string> = {
  DAYS: "days",
  WEEKS: "weeks",
  MONTHS: "months",
};

function describeError(caught: unknown, fallback: string): string {
  const response = (caught as { response?: { status?: number; data?: { message?: string } } })?.response;
  const message = String(response?.data?.message || "");
  if (response?.status === 403) return "You do not have permission to perform this action.";
  if (message) return message;
  return fallback;
}

function formatDateRange(start?: string | null, end?: string | null): string | null {
  if (!start && !end) return null;
  if (start && end) return `${formatAppointmentDate(start)} – ${formatAppointmentDate(end)}`;
  if (start) return `From ${formatAppointmentDate(start)}`;
  return `Until ${formatAppointmentDate(end!)}`;
}

export function TreatmentPlanSection({ encounterUuid, editable }: TreatmentPlanSectionProps) {
  const { data: plans, isLoading, isError, refetch } = useTreatmentPlans(encounterUuid);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<TreatmentPlan | null>(null);

  const openCreate = () => {
    setEditing(null);
    setDialogOpen(true);
  };

  return (
    <section className="rounded-lg border border-outline-variant bg-surface-container-lowest p-4">
      <div className="mb-1 flex flex-wrap items-center justify-between gap-2">
        <div className="flex items-center gap-2">
          <h2 className="text-xs font-bold uppercase tracking-wider text-on-surface-variant">
            Treatment &amp; Care Plan
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
            + Add Treatment Plan
          </button>
        )}
      </div>
      <p className="mb-3 text-xs text-on-surface-variant">
        Plan and instructions for ongoing patient care.
      </p>

      {isLoading ? (
        <div className="space-y-2">
          {[0, 1, 2].map((index) => (
            <div key={index} className="h-16 animate-pulse rounded bg-surface-container" />
          ))}
        </div>
      ) : isError ? (
        <div>
          <p className="text-sm font-bold text-on-surface">Unable to load treatment plan</p>
          <button
            onClick={() => refetch()}
            className="mt-2 h-8 rounded border border-outline-variant px-3 text-xs font-bold text-primary"
          >
            Try again
          </button>
        </div>
      ) : !plans?.length ? (
        <div>
          <p className="text-sm font-bold text-on-surface">No treatment plan recorded</p>
          <p className="mt-1 text-xs text-on-surface-variant">
            Add a plan with treatment items for this encounter.
          </p>
          {editable && (
            <button
              onClick={openCreate}
              className="mt-3 h-8 rounded-md bg-primary px-3 text-xs font-bold text-on-primary"
            >
              + Add Treatment Plan
            </button>
          )}
        </div>
      ) : (
        <div className="space-y-4">
          {plans.map((plan) => (
            <PlanCard
              key={plan.uuid}
              encounterUuid={encounterUuid}
              plan={plan}
              editable={editable}
              onEditPlan={(target) => {
                setEditing(target);
                setDialogOpen(true);
              }}
            />
          ))}
        </div>
      )}

      <TreatmentPlanDialog
        open={dialogOpen}
        encounterUuid={encounterUuid}
        plan={editing}
        onClose={() => setDialogOpen(false)}
      />
    </section>
  );
}

interface PlanCardProps {
  encounterUuid: string;
  plan: TreatmentPlan;
  editable: boolean;
  onEditPlan: (plan: TreatmentPlan) => void;
}

function PlanCard({ encounterUuid, plan, editable, onEditPlan }: PlanCardProps) {
  const { data: items, isLoading: itemsLoading } = useTreatmentPlanItems(
    encounterUuid,
    plan.uuid,
  );
  const planAction = useTreatmentPlanAction(encounterUuid);
  const deletePlan = useDeleteTreatmentPlan(encounterUuid);
  const itemAction = useTreatmentItemAction(encounterUuid);
  const deleteItem = useDeleteTreatmentItem(encounterUuid);

  const [actionError, setActionError] = useState("");
  const [cancelPlanOpen, setCancelPlanOpen] = useState(false);
  const [cancelPlanError, setCancelPlanError] = useState("");
  const [deletePlanOpen, setDeletePlanOpen] = useState(false);
  const [deletePlanError, setDeletePlanError] = useState("");
  const [cancelItemTarget, setCancelItemTarget] = useState<TreatmentPlanItem | null>(null);
  const [cancelItemError, setCancelItemError] = useState("");
  const [deleteItemTarget, setDeleteItemTarget] = useState<TreatmentPlanItem | null>(null);
  const [deleteItemError, setDeleteItemError] = useState("");
  const [itemDialogOpen, setItemDialogOpen] = useState(false);
  const [editingItem, setEditingItem] = useState<TreatmentPlanItem | null>(null);

  const planOpen = plan.status === "DRAFT" || plan.status === "ACTIVE";
  const canEdit = editable && planOpen;

  const runPlanAction = async (action: TreatmentPlanAction) => {
    setActionError("");
    try {
      await planAction.mutateAsync({ planUuid: plan.uuid, action });
    } catch (caught) {
      setActionError(describeError(caught, "Unable to update the treatment plan."));
    }
  };

  const runItemAction = async (item: TreatmentPlanItem, action: TreatmentItemAction) => {
    setActionError("");
    try {
      await itemAction.mutateAsync({ planUuid: plan.uuid, itemUuid: item.uuid, action });
    } catch (caught) {
      setActionError(describeError(caught, "Unable to update the treatment."));
    }
  };

  const handleCancelPlan = async () => {
    setCancelPlanError("");
    try {
      await planAction.mutateAsync({ planUuid: plan.uuid, action: "cancel" });
      setCancelPlanOpen(false);
    } catch (caught) {
      setCancelPlanError(describeError(caught, "Unable to cancel the treatment plan."));
    }
  };

  const handleDeletePlan = async () => {
    setDeletePlanError("");
    try {
      await deletePlan.mutateAsync(plan.uuid);
      setDeletePlanOpen(false);
    } catch (caught) {
      setDeletePlanError(describeError(caught, "Unable to delete the treatment plan."));
    }
  };

  const handleCancelItem = async () => {
    if (!cancelItemTarget) return;
    setCancelItemError("");
    try {
      await itemAction.mutateAsync({
        planUuid: plan.uuid,
        itemUuid: cancelItemTarget.uuid,
        action: "cancel",
      });
      setCancelItemTarget(null);
    } catch (caught) {
      setCancelItemError(describeError(caught, "Unable to cancel the treatment."));
    }
  };

  const handleDeleteItem = async () => {
    if (!deleteItemTarget) return;
    setDeleteItemError("");
    try {
      await deleteItem.mutateAsync({
        planUuid: plan.uuid,
        itemUuid: deleteItemTarget.uuid,
      });
      setDeleteItemTarget(null);
    } catch (caught) {
      setDeleteItemError(describeError(caught, "Unable to delete the treatment."));
    }
  };

  const period = formatDateRange(plan.startDate, plan.endDate);

  return (
    <div className="rounded-md border border-outline-variant bg-surface p-3">
      <div className="flex flex-wrap items-start justify-between gap-2">
        <div className="min-w-0 space-y-1">
          <div className="flex flex-wrap items-center gap-2">
            <span
              className={`rounded-full px-2 py-0.5 text-[10px] font-bold tracking-wide ${planStatusStyles[plan.status]}`}
            >
              {planStatusLabel[plan.status]}
            </span>
            <p className="text-sm font-semibold text-on-surface">{plan.title}</p>
          </div>
          {plan.goals && (
            <p className="text-xs text-on-surface-variant">
              <span className="font-semibold text-on-surface">Goals:</span> {plan.goals}
            </p>
          )}
          {period && <p className="text-[11px] text-on-surface-variant">Plan period: {period}</p>}
          {plan.followUpInstructions && (
            <p className="text-[11px] text-on-surface-variant">
              Follow-up: {plan.followUpInstructions}
            </p>
          )}
        </div>
        {editable && (
          <div className="flex shrink-0 flex-wrap gap-1">
            {plan.status === "DRAFT" && (
              <button
                disabled={planAction.isPending}
                onClick={() => runPlanAction("activate")}
                className="rounded border border-outline-variant px-2 py-1 text-[10px] font-bold text-primary hover:bg-primary/5 disabled:opacity-50"
              >
                Activate
              </button>
            )}
            {plan.status === "ACTIVE" && (
              <button
                disabled={planAction.isPending}
                onClick={() => runPlanAction("complete")}
                className="rounded border border-outline-variant px-2 py-1 text-[10px] font-bold text-primary hover:bg-primary/5 disabled:opacity-50"
              >
                Complete
              </button>
            )}
            {plan.status !== "COMPLETED" && plan.status !== "CANCELLED" && (
              <>
                <button
                  onClick={() => onEditPlan(plan)}
                  className="rounded border border-outline-variant px-2 py-1 text-[10px] font-bold text-primary hover:bg-primary/5"
                >
                  Edit
                </button>
                <button
                  onClick={() => {
                    setCancelPlanError("");
                    setCancelPlanOpen(true);
                  }}
                  className="rounded border border-outline-variant px-2 py-1 text-[10px] font-bold text-on-surface-variant hover:bg-surface-container"
                >
                  Cancel
                </button>
                <button
                  onClick={() => {
                    setDeletePlanError("");
                    setDeletePlanOpen(true);
                  }}
                  className="rounded border border-outline-variant px-2 py-1 text-[10px] font-bold text-error hover:bg-error/5"
                >
                  Delete
                </button>
              </>
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
            Treatments
          </p>
          {canEdit && (
            <button
              onClick={() => {
                setEditingItem(null);
                setItemDialogOpen(true);
              }}
              className="rounded border border-outline-variant px-2 py-1 text-[10px] font-bold text-primary hover:bg-primary/5"
            >
              + Add Treatment
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
          <p className="text-xs text-on-surface-variant">No treatments in this plan yet.</p>
        ) : (
          <div>
            {items.map((item) => (
              <div
                key={item.uuid}
                className="flex items-start justify-between gap-3 border-b border-outline-variant py-2 last:border-b-0"
              >
                <div className="min-w-0 space-y-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <span
                      className={`rounded-full px-2 py-0.5 text-[10px] font-bold tracking-wide ${priorityStyles[item.priority]}`}
                    >
                      {item.priority}
                    </span>
                    <p className="text-sm font-semibold text-on-surface">{item.name}</p>
                  </div>
                  <p className="text-[11px] text-on-surface-variant">
                    {treatmentTypeLabel[item.treatmentType]}
                    {item.frequency && ` · ${item.frequency}`}
                    {item.duration != null && item.durationUnit
                      ? ` · ${item.duration} ${durationUnitLabel[item.durationUnit]}`
                      : ""}
                  </p>
                  {(item.diagnosisUuid || item.diagnosisName) && (
                    <p className="text-[11px] text-on-surface-variant">
                      For:{" "}
                      {item.diagnosisCode && (
                        <span className="font-mono font-bold text-on-surface">
                          {item.diagnosisCode}
                        </span>
                      )}
                      {item.diagnosisCode && item.diagnosisName && " · "}
                      {item.diagnosisName}
                    </p>
                  )}
                  <p className="flex flex-wrap items-center gap-2 text-[11px] text-on-surface-variant">
                    <span
                      className={`rounded-full px-2 py-0.5 text-[10px] font-bold ${itemStatusStyles[item.status]}`}
                    >
                      {itemStatusLabel[item.status]}
                    </span>
                    {formatDateRange(item.startDate, item.endDate) && (
                      <span>{formatDateRange(item.startDate, item.endDate)}</span>
                    )}
                  </p>
                </div>
                {canEdit && (
                  <div className="flex shrink-0 flex-wrap gap-1">
                    {item.status === "PLANNED" && (
                      <button
                        disabled={itemAction.isPending}
                        onClick={() => runItemAction(item, "start")}
                        className="rounded border border-outline-variant px-2 py-1 text-[10px] font-bold text-primary hover:bg-primary/5 disabled:opacity-50"
                      >
                        Start
                      </button>
                    )}
                    {item.status === "IN_PROGRESS" && (
                      <button
                        disabled={itemAction.isPending}
                        onClick={() => runItemAction(item, "complete")}
                        className="rounded border border-outline-variant px-2 py-1 text-[10px] font-bold text-primary hover:bg-primary/5 disabled:opacity-50"
                      >
                        Complete
                      </button>
                    )}
                    {item.status !== "COMPLETED" && item.status !== "CANCELLED" && (
                      <>
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
                      </>
                    )}
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>

      <TreatmentItemDialog
        open={itemDialogOpen}
        encounterUuid={encounterUuid}
        planUuid={plan.uuid}
        item={editingItem}
        onClose={() => setItemDialogOpen(false)}
      />

      <AlertDialog open={cancelPlanOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Cancel treatment plan?</AlertDialogTitle>
            <AlertDialogDescription>
              This plan will be marked cancelled. Its history remains available for review.
            </AlertDialogDescription>
          </AlertDialogHeader>
          {cancelPlanError && (
            <p role="alert" className="mt-3 text-xs font-semibold text-error">
              {cancelPlanError}
            </p>
          )}
          <AlertDialogFooter>
            <button
              onClick={() => setCancelPlanOpen(false)}
              className="h-8 rounded-md border border-outline-variant px-3 text-xs font-semibold"
            >
              Keep Plan
            </button>
            <button
              disabled={planAction.isPending}
              onClick={handleCancelPlan}
              className="h-8 rounded-md bg-error px-3 text-xs font-bold text-on-error disabled:opacity-50"
            >
              {planAction.isPending ? "Cancelling…" : "Cancel Plan"}
            </button>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      <AlertDialog open={deletePlanOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Delete treatment plan?</AlertDialogTitle>
            <AlertDialogDescription>
              The plan and its treatments are removed from this encounter. Completed plans cannot
              be deleted.
            </AlertDialogDescription>
          </AlertDialogHeader>
          {deletePlanError && (
            <p role="alert" className="mt-3 text-xs font-semibold text-error">
              {deletePlanError}
            </p>
          )}
          <AlertDialogFooter>
            <button
              onClick={() => setDeletePlanOpen(false)}
              className="h-8 rounded-md border border-outline-variant px-3 text-xs font-semibold"
            >
              Cancel
            </button>
            <button
              disabled={deletePlan.isPending}
              onClick={handleDeletePlan}
              className="h-8 rounded-md bg-error px-3 text-xs font-bold text-on-error disabled:opacity-50"
            >
              {deletePlan.isPending ? "Deleting…" : "Delete"}
            </button>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      <AlertDialog open={Boolean(cancelItemTarget)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Cancel treatment?</AlertDialogTitle>
            <AlertDialogDescription>
              {cancelItemTarget?.name} will be marked cancelled.
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
              {itemAction.isPending ? "Cancelling…" : "Cancel Treatment"}
            </button>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      <AlertDialog open={Boolean(deleteItemTarget)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Delete treatment?</AlertDialogTitle>
            <AlertDialogDescription>
              {deleteItemTarget?.name} will be removed from this plan. Completed treatments cannot
              be deleted.
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
