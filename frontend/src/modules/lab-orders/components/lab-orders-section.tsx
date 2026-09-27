"use client";

import { useState } from "react";
import { useLabOrders } from "../hooks/use-lab-orders";
import { LabOrderSummary } from "../types/lab-order.types";
import { LabOrderCard } from "./lab-order-card";
import { LabOrderDialog } from "./lab-order-dialog";

interface LabOrdersSectionProps {
  encounterUuid: string;
  editable: boolean;
}

export function LabOrdersSection({ encounterUuid, editable }: LabOrdersSectionProps) {
  const { data: orders, isLoading, isError, refetch } = useLabOrders(encounterUuid);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<LabOrderSummary | null>(null);

  const openCreate = () => {
    setEditing(null);
    setDialogOpen(true);
  };

  return (
    <section className="rounded-lg border border-outline-variant bg-surface-container-lowest p-4">
      <div className="mb-1 flex flex-wrap items-center justify-between gap-2">
        <div className="flex items-center gap-2">
          <h2 className="text-xs font-bold uppercase tracking-wider text-on-surface-variant">
            Lab Orders &amp; Results
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
            + New Lab Order
          </button>
        )}
      </div>
      <p className="mb-3 text-xs text-on-surface-variant">
        Tests ordered for this visit. Specimen collection, lab processing, and results can continue
        after the encounter is completed.
      </p>

      {isLoading ? (
        <div className="space-y-2">
          {[0, 1, 2].map((index) => (
            <div key={index} className="h-24 animate-pulse rounded bg-surface-container" />
          ))}
        </div>
      ) : isError ? (
        <div className="rounded border border-error/40 bg-error/5 p-4 text-center">
          <p className="text-sm font-bold text-on-surface">Unable to load lab orders</p>
          <button
            onClick={() => refetch()}
            className="mt-2 h-8 rounded bg-primary px-3 text-xs font-bold text-on-primary"
          >
            Retry
          </button>
        </div>
      ) : !orders || orders.length === 0 ? (
        <p className="rounded border border-dashed border-outline-variant p-4 text-center text-xs text-on-surface-variant">
          No lab orders for this encounter yet.
        </p>
      ) : (
        <div className="space-y-3">
          {orders.map((summary) => (
            <LabOrderCard
              key={summary.uuid}
              encounterUuid={encounterUuid}
              summary={summary}
              editable={editable}
              onEdit={(order) => {
                setEditing(order);
                setDialogOpen(true);
              }}
            />
          ))}
        </div>
      )}

      <LabOrderDialog
        open={dialogOpen}
        encounterUuid={encounterUuid}
        order={editing}
        onClose={() => setDialogOpen(false)}
      />
    </section>
  );
}
