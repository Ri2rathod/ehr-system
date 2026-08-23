"use client";

import { useState } from "react";
import { Plus, Trash2, Edit2, CheckCircle2, Clock, AlertCircle, CalendarDays } from "lucide-react";
import {
  AvailabilityExceptionResponse,
  CreateExceptionRequest,
  UpdateExceptionRequest,
  ExceptionType,
} from "../../types/scheduling.types";
import {
  useExceptions,
  useCreateException,
  useUpdateException,
  useDeleteException,
} from "../../hooks/use-scheduling";

const EXCEPTION_TYPES: { value: ExceptionType; label: string }[] = [
  { value: "VACATION", label: "Vacation" },
  { value: "HOLIDAY", label: "Holiday" },
  { value: "EMERGENCY_LEAVE", label: "Emergency Leave" },
  { value: "PERSONAL_LEAVE", label: "Personal Leave" },
  { value: "SICK_LEAVE", label: "Sick Leave" },
  { value: "TRAINING", label: "Training" },
  { value: "OTHER", label: "Other" },
];

const EXCEPTION_COLORS: Record<ExceptionType, string> = {
  VACATION: "bg-tertiary-fixed text-on-tertiary-fixed",
  HOLIDAY: "bg-primary-fixed text-on-primary-fixed",
  EMERGENCY_LEAVE: "bg-error-container text-on-error-container",
  PERSONAL_LEAVE: "bg-secondary-fixed text-on-secondary-fixed",
  SICK_LEAVE: "bg-error-container text-on-error-container",
  TRAINING: "bg-surface-container-high text-on-surface-variant border border-outline-variant/50",
  OTHER: "bg-surface-container text-on-surface",
};

const EMPTY_FORM: CreateExceptionRequest = {
  exceptionDate: "",
  exceptionType: "PERSONAL_LEAVE",
  isFullDay: true,
  reason: "",
};

interface Props {
  doctorUuid: string;
  from?: string;
  to?: string;
}

export function ExceptionsTab({ doctorUuid, from, to }: Props) {
  const { data: exceptions = [], isLoading } = useExceptions(doctorUuid, from, to);
  const createMut = useCreateException(doctorUuid);
  const updateMut = useUpdateException(doctorUuid);
  const deleteMut = useDeleteException(doctorUuid);

  const [isAddOpen, setIsAddOpen] = useState(false);
  const [editTarget, setEditTarget] = useState<AvailabilityExceptionResponse | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<string | null>(null);
  const [form, setForm] = useState<CreateExceptionRequest>(EMPTY_FORM);
  const [rangeFrom, setRangeFrom] = useState(from ?? "");
  const [rangeTo, setRangeTo] = useState(to ?? "");

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (editTarget) {
      const payload: UpdateExceptionRequest = { ...form };
      if (form.isFullDay) { delete payload.startTime; delete payload.endTime; }
      await updateMut.mutateAsync({ exceptionUuid: editTarget.uuid, payload });
      setEditTarget(null);
    } else {
      const payload: CreateExceptionRequest = { ...form };
      if (form.isFullDay) { delete payload.startTime; delete payload.endTime; }
      await createMut.mutateAsync(payload);
      setIsAddOpen(false);
    }
    setForm(EMPTY_FORM);
  };

  const openEdit = (ex: AvailabilityExceptionResponse) => {
    setEditTarget(ex);
    setForm({
      exceptionDate: ex.exceptionDate,
      exceptionType: ex.exceptionType,
      isFullDay: ex.isFullDay,
      startTime: ex.startTime,
      endTime: ex.endTime,
      reason: ex.reason ?? "",
    });
  };

  if (isLoading) {
    return (
      <div className="space-y-2">
        {[...Array(4)].map((_, i) => (
          <div key={i} className="h-16 rounded-lg bg-surface-container animate-pulse" />
        ))}
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {/* Filters + Toolbar */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3">
        <div className="flex items-center gap-2">
          <div>
            <label className="block text-[10px] font-bold text-on-surface-variant mb-0.5">From</label>
            <input type="date" value={rangeFrom} onChange={(e) => setRangeFrom(e.target.value)}
              className="rounded-md border border-outline-variant bg-surface p-1.5 text-xs text-on-surface" />
          </div>
          <div>
            <label className="block text-[10px] font-bold text-on-surface-variant mb-0.5">To</label>
            <input type="date" value={rangeTo} onChange={(e) => setRangeTo(e.target.value)}
              className="rounded-md border border-outline-variant bg-surface p-1.5 text-xs text-on-surface" />
          </div>
        </div>
        <button
          onClick={() => { setForm(EMPTY_FORM); setIsAddOpen(true); }}
          className="px-4 py-2 bg-primary text-on-primary text-xs font-semibold rounded-lg flex items-center gap-1.5 hover:opacity-90 shadow-xs"
        >
          <Plus className="w-3.5 h-3.5" /> Add Exception
        </button>
      </div>

      {/* List */}
      <div className="bg-surface-container-lowest rounded-xl border border-outline-variant overflow-hidden shadow-xs">
        <div className="p-4 border-b border-outline-variant flex items-center justify-between">
          <h3 className="font-bold text-sm text-on-surface">Schedule Exceptions &amp; Leaves</h3>
          <span className="text-xs text-on-surface-variant">{exceptions.length} exception{exceptions.length !== 1 ? "s" : ""}</span>
        </div>
        <div className="divide-y divide-outline-variant">
          {exceptions.length === 0 ? (
            <div className="p-8 text-center">
              <CalendarDays className="w-10 h-10 text-outline mx-auto mb-2" />
              <p className="text-sm text-on-surface-variant font-medium">No exceptions found</p>
            </div>
          ) : (
            exceptions.map((ex) => (
              <div key={ex.uuid} className="p-4 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 hover:bg-surface-container-low/30">
                <div className="space-y-1">
                  <div className="flex items-center gap-2">
                    <span className="font-bold text-sm text-on-surface">{ex.exceptionDate}</span>
                    <span className={`text-[10px] font-bold px-2 py-0.5 rounded uppercase ${EXCEPTION_COLORS[ex.exceptionType]}`}>
                      {ex.exceptionType.replace(/_/g, " ")}
                    </span>
                    {ex.isFullDay ? (
                      <span className="text-[10px] font-bold bg-surface-container text-outline px-2 py-0.5 rounded">Full Day</span>
                    ) : (
                      <span className="text-xs text-outline flex items-center gap-1">
                        <Clock className="w-3 h-3" /> {ex.startTime} – {ex.endTime}
                      </span>
                    )}
                  </div>
                  {ex.reason && <p className="text-xs text-on-surface-variant">{ex.reason}</p>}
                </div>
                <div className="flex items-center gap-2">
                  <button onClick={() => openEdit(ex)}
                    className="p-1.5 rounded hover:bg-surface-container text-outline hover:text-on-surface transition-colors">
                    <Edit2 className="w-4 h-4" />
                  </button>
                  <button onClick={() => setDeleteTarget(ex.uuid)}
                    className="p-1.5 rounded hover:bg-error-container/20 text-outline hover:text-error transition-colors">
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>
            ))
          )}
        </div>
      </div>

      {/* Add / Edit Modal */}
      {(isAddOpen || editTarget) && (
        <div className="fixed inset-0 z-50 bg-inverse-surface/40 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-surface-container-lowest border border-outline-variant rounded-xl w-full max-w-md p-6 shadow-lg space-y-4">
            <h3 className="font-bold text-base text-on-surface flex items-center gap-2">
              <AlertCircle className="w-5 h-5 text-primary" />
              {editTarget ? "Edit Exception" : "Add Exception"}
            </h3>
            <form onSubmit={handleSubmit} className="space-y-4 text-sm">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-on-surface-variant mb-1">Date *</label>
                  <input type="date" required value={form.exceptionDate}
                    onChange={(e) => setForm({ ...form, exceptionDate: e.target.value })}
                    className="w-full rounded-md border border-outline-variant bg-surface p-2 text-on-surface" />
                </div>
                <div>
                  <label className="block text-xs font-bold text-on-surface-variant mb-1">Type *</label>
                  <select required value={form.exceptionType}
                    onChange={(e) => setForm({ ...form, exceptionType: e.target.value as ExceptionType })}
                    className="w-full rounded-md border border-outline-variant bg-surface p-2 text-on-surface">
                    {EXCEPTION_TYPES.map((t) => <option key={t.value} value={t.value}>{t.label}</option>)}
                  </select>
                </div>
              </div>
              <label className="flex items-center gap-2 text-xs font-medium text-on-surface cursor-pointer">
                <input type="checkbox" checked={form.isFullDay ?? true}
                  onChange={(e) => setForm({ ...form, isFullDay: e.target.checked })}
                  className="rounded border-outline-variant text-primary" />
                Full Day
              </label>
              {!form.isFullDay && (
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs font-bold text-on-surface-variant mb-1">Start Time</label>
                    <input type="time" value={form.startTime ?? ""}
                      onChange={(e) => setForm({ ...form, startTime: e.target.value })}
                      className="w-full rounded-md border border-outline-variant bg-surface p-2 text-on-surface" />
                  </div>
                  <div>
                    <label className="block text-xs font-bold text-on-surface-variant mb-1">End Time</label>
                    <input type="time" value={form.endTime ?? ""}
                      onChange={(e) => setForm({ ...form, endTime: e.target.value })}
                      className="w-full rounded-md border border-outline-variant bg-surface p-2 text-on-surface" />
                  </div>
                </div>
              )}
              <div>
                <label className="block text-xs font-bold text-on-surface-variant mb-1">Reason</label>
                <textarea rows={3} value={form.reason ?? ""}
                  onChange={(e) => setForm({ ...form, reason: e.target.value })}
                  placeholder="Reason for exception..."
                  className="w-full rounded-md border border-outline-variant bg-surface p-2 text-on-surface resize-none" />
              </div>
              <div className="flex justify-end gap-2 pt-2 border-t border-outline-variant">
                <button type="button" onClick={() => { setIsAddOpen(false); setEditTarget(null); setForm(EMPTY_FORM); }}
                  className="px-4 py-2 bg-surface-container text-on-surface text-xs font-medium rounded-lg">Cancel</button>
                <button type="submit" disabled={createMut.isPending || updateMut.isPending}
                  className="px-4 py-2 bg-primary text-on-primary text-xs font-bold rounded-lg hover:opacity-90 disabled:opacity-60">
                  {createMut.isPending || updateMut.isPending ? "Saving…" : editTarget ? "Update" : "Save Exception"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Delete confirm */}
      {deleteTarget && (
        <div className="fixed inset-0 z-50 bg-inverse-surface/40 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-surface-container-lowest border border-outline-variant rounded-xl w-full max-w-sm p-6 shadow-lg space-y-4">
            <h3 className="font-bold text-base text-on-surface">Delete Exception?</h3>
            <p className="text-sm text-on-surface-variant">This will permanently remove this exception record.</p>
            <div className="flex justify-end gap-2">
              <button onClick={() => setDeleteTarget(null)} className="px-4 py-2 bg-surface-container text-on-surface text-xs font-medium rounded-lg">Cancel</button>
              <button onClick={async () => { await deleteMut.mutateAsync(deleteTarget); setDeleteTarget(null); }}
                disabled={deleteMut.isPending}
                className="px-4 py-2 bg-error text-on-error text-xs font-bold rounded-lg disabled:opacity-60">
                {deleteMut.isPending ? "Deleting…" : "Delete"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
