"use client";

import { useState } from "react";
import { Clock, Coffee, Plus, Trash2, Edit2, ToggleLeft, ToggleRight, AlertCircle } from "lucide-react";
import {
  AvailabilityResponse,
  CreateAvailabilityRequest,
  UpdateAvailabilityRequest,
  DayOfWeek,
  ScheduleType,
  DAY_ORDER,
  DAY_LABELS,
} from "../../types/scheduling.types";
import {
  useCreateAvailability,
  useUpdateAvailability,
  useDeleteAvailability,
  useToggleAvailabilityStatus,
} from "../../hooks/use-scheduling";

interface Props {
  doctorUuid: string;
  availability: AvailabilityResponse[];
  isLoading: boolean;
}

const SCHEDULE_COLORS: Record<ScheduleType, string> = {
  WORK: "bg-tertiary-fixed text-on-tertiary-fixed",
  BREAK: "bg-surface-container-high text-on-surface-variant border border-outline-variant/50",
};

const EMPTY_FORM: CreateAvailabilityRequest = {
  dayOfWeek: "MONDAY",
  scheduleType: "WORK",
  startTime: "09:00",
  endTime: "17:00",
  isActive: true,
};

export function WeeklyScheduleTab({ doctorUuid, availability, isLoading }: Props) {
  const [isAddOpen, setIsAddOpen] = useState(false);
  const [editRow, setEditRow] = useState<AvailabilityResponse | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<string | null>(null);
  const [form, setForm] = useState<CreateAvailabilityRequest>(EMPTY_FORM);

  const createMut = useCreateAvailability(doctorUuid);
  const updateMut = useUpdateAvailability(doctorUuid);
  const deleteMut = useDeleteAvailability(doctorUuid);
  const toggleMut = useToggleAvailabilityStatus(doctorUuid);

  const grouped = DAY_ORDER.reduce<Record<DayOfWeek, AvailabilityResponse[]>>(
    (acc, day) => {
      acc[day] = availability.filter((a) => a.dayOfWeek === day);
      return acc;
    },
    {} as Record<DayOfWeek, AvailabilityResponse[]>
  );

  const handleAdd = async (e: React.FormEvent) => {
    e.preventDefault();
    if (form.endTime <= form.startTime) return alert("End time must be after start time");
    await createMut.mutateAsync(form);
    setIsAddOpen(false);
    setForm(EMPTY_FORM);
  };

  const handleUpdate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editRow) return;
    const payload: UpdateAvailabilityRequest = {
      dayOfWeek: form.dayOfWeek,
      scheduleType: form.scheduleType,
      startTime: form.startTime,
      endTime: form.endTime,
      isActive: form.isActive,
      effectiveFrom: form.effectiveFrom,
      effectiveUntil: form.effectiveUntil,
    };
    await updateMut.mutateAsync({ availabilityUuid: editRow.uuid, payload });
    setEditRow(null);
  };

  const openEdit = (row: AvailabilityResponse) => {
    setEditRow(row);
    setForm({
      dayOfWeek: row.dayOfWeek,
      scheduleType: row.scheduleType,
      startTime: row.startTime,
      endTime: row.endTime,
      isActive: row.isActive,
      effectiveFrom: row.effectiveFrom,
      effectiveUntil: row.effectiveUntil,
    });
  };

  if (isLoading) {
    return (
      <div className="space-y-2">
        {[...Array(5)].map((_, i) => (
          <div key={i} className="h-16 rounded-lg bg-surface-container animate-pulse" />
        ))}
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {/* Toolbar */}
      <div className="flex justify-end">
        <button
          onClick={() => { setForm(EMPTY_FORM); setIsAddOpen(true); }}
          className="px-4 py-2 bg-primary text-on-primary text-xs font-semibold rounded-lg flex items-center gap-1.5 hover:opacity-90 shadow-xs"
        >
          <Plus className="w-3.5 h-3.5" /> Add Schedule
        </button>
      </div>

      {/* Table */}
      <div className="bg-surface-container-lowest rounded-xl border border-outline-variant overflow-hidden shadow-xs divide-y divide-outline-variant">
        {DAY_ORDER.map((day) => (
          <div key={day} className="flex flex-col md:flex-row">
            <div className="w-full md:w-36 p-4 bg-surface-container-highest/20 border-b md:border-b-0 md:border-r border-outline-variant flex items-center justify-between md:justify-center shrink-0">
              <span className="text-sm font-bold text-on-surface">{DAY_LABELS[day]}</span>
              {grouped[day].length === 0 && (
                <span className="text-[10px] font-bold bg-surface-container text-outline px-2 py-0.5 rounded-full md:hidden">OFF</span>
              )}
            </div>
            <div className="flex-1 p-4 flex flex-col gap-2">
              {grouped[day].length === 0 ? (
                <div className="flex items-center justify-center py-1">
                  <span className="text-xs font-medium text-outline flex items-center gap-2 bg-surface-container rounded-full px-4 py-1">
                    <AlertCircle className="w-3.5 h-3.5" /> No schedule configured
                  </span>
                </div>
              ) : (
                grouped[day]
                  .sort((a, b) => a.startTime.localeCompare(b.startTime))
                  .map((slot) => (
                    <div
                      key={slot.uuid}
                      className={`flex flex-col sm:flex-row items-start sm:items-center gap-3 p-3 rounded-lg border transition-all ${
                        slot.scheduleType === "BREAK"
                          ? "border-outline-variant/40 bg-surface-container-lowest/70"
                          : "border-outline-variant/60 bg-surface-bright shadow-2xs"
                      } ${!slot.isActive ? "opacity-50" : ""}`}
                    >
                      <div className="flex items-center gap-2 w-full sm:w-52 shrink-0 font-semibold text-sm text-on-surface">
                        {slot.scheduleType === "BREAK"
                          ? <Coffee className="w-4 h-4 text-outline" />
                          : <Clock className="w-4 h-4 text-primary" />}
                        {slot.startTime} – {slot.endTime}
                      </div>
                      <div className="flex-1 flex flex-wrap items-center gap-2">
                        <span className={`text-[10px] font-bold tracking-wider px-2 py-0.5 rounded uppercase ${SCHEDULE_COLORS[slot.scheduleType]}`}>
                          {slot.scheduleType}
                        </span>
                        {slot.effectiveFrom && (
                          <span className="text-xs text-outline">
                            Eff: {slot.effectiveFrom}{slot.effectiveUntil ? ` → ${slot.effectiveUntil}` : ""}
                          </span>
                        )}
                      </div>
                      <div className="flex items-center gap-1">
                        <button
                          onClick={() => toggleMut.mutate(slot.uuid)}
                          className="p-1.5 rounded hover:bg-surface-container text-outline hover:text-primary transition-colors"
                          title={slot.isActive ? "Deactivate" : "Activate"}
                        >
                          {slot.isActive
                            ? <ToggleRight className="w-4 h-4 text-primary" />
                            : <ToggleLeft className="w-4 h-4" />}
                        </button>
                        <button
                          onClick={() => openEdit(slot)}
                          className="p-1.5 rounded hover:bg-surface-container text-outline hover:text-on-surface transition-colors"
                        >
                          <Edit2 className="w-4 h-4" />
                        </button>
                        <button
                          onClick={() => setDeleteTarget(slot.uuid)}
                          className="p-1.5 rounded hover:bg-error-container/20 text-outline hover:text-error transition-colors"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    </div>
                  ))
              )}
            </div>
          </div>
        ))}
      </div>

      {/* Add / Edit Modal */}
      {(isAddOpen || editRow) && (
        <div className="fixed inset-0 z-50 bg-inverse-surface/40 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-surface-container-lowest border border-outline-variant rounded-xl w-full max-w-md p-6 shadow-lg space-y-4">
            <h3 className="font-bold text-base text-on-surface flex items-center gap-2">
              <Clock className="w-5 h-5 text-primary" />
              {editRow ? "Edit Schedule Slot" : "Add Schedule Slot"}
            </h3>
            <form onSubmit={editRow ? handleUpdate : handleAdd} className="space-y-4 text-sm">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-on-surface-variant mb-1">Day</label>
                  <select
                    value={form.dayOfWeek}
                    onChange={(e) => setForm({ ...form, dayOfWeek: e.target.value as DayOfWeek })}
                    className="w-full rounded-md border border-outline-variant bg-surface p-2 text-on-surface font-medium"
                  >
                    {DAY_ORDER.map((d) => <option key={d} value={d}>{DAY_LABELS[d]}</option>)}
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-bold text-on-surface-variant mb-1">Type</label>
                  <select
                    value={form.scheduleType}
                    onChange={(e) => setForm({ ...form, scheduleType: e.target.value as ScheduleType })}
                    className="w-full rounded-md border border-outline-variant bg-surface p-2 text-on-surface font-medium"
                  >
                    <option value="WORK">Work</option>
                    <option value="BREAK">Break</option>
                  </select>
                </div>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-on-surface-variant mb-1">Start Time</label>
                  <input type="time" value={form.startTime}
                    onChange={(e) => setForm({ ...form, startTime: e.target.value })}
                    required className="w-full rounded-md border border-outline-variant bg-surface p-2 text-on-surface font-medium" />
                </div>
                <div>
                  <label className="block text-xs font-bold text-on-surface-variant mb-1">End Time</label>
                  <input type="time" value={form.endTime}
                    onChange={(e) => setForm({ ...form, endTime: e.target.value })}
                    required className="w-full rounded-md border border-outline-variant bg-surface p-2 text-on-surface font-medium" />
                </div>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-on-surface-variant mb-1">Effective From</label>
                  <input type="date" value={form.effectiveFrom ?? ""}
                    onChange={(e) => setForm({ ...form, effectiveFrom: e.target.value || undefined })}
                    className="w-full rounded-md border border-outline-variant bg-surface p-2 text-on-surface font-medium" />
                </div>
                <div>
                  <label className="block text-xs font-bold text-on-surface-variant mb-1">Effective Until</label>
                  <input type="date" value={form.effectiveUntil ?? ""}
                    onChange={(e) => setForm({ ...form, effectiveUntil: e.target.value || undefined })}
                    className="w-full rounded-md border border-outline-variant bg-surface p-2 text-on-surface font-medium" />
                </div>
              </div>
              <label className="flex items-center gap-2 text-xs font-medium text-on-surface cursor-pointer">
                <input type="checkbox" checked={form.isActive ?? true}
                  onChange={(e) => setForm({ ...form, isActive: e.target.checked })}
                  className="rounded border-outline-variant text-primary" />
                Active
              </label>
              <div className="flex justify-end gap-2 pt-2 border-t border-outline-variant">
                <button type="button" onClick={() => { setIsAddOpen(false); setEditRow(null); }}
                  className="px-4 py-2 bg-surface-container text-on-surface text-xs font-medium rounded-lg hover:bg-surface-container-high">
                  Cancel
                </button>
                <button type="submit"
                  disabled={createMut.isPending || updateMut.isPending}
                  className="px-4 py-2 bg-primary text-on-primary text-xs font-bold rounded-lg hover:opacity-90 shadow-xs disabled:opacity-60">
                  {createMut.isPending || updateMut.isPending ? "Saving…" : editRow ? "Update" : "Add Slot"}
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
            <h3 className="font-bold text-base text-on-surface">Delete Schedule Slot?</h3>
            <p className="text-sm text-on-surface-variant">This will remove the slot permanently.</p>
            <div className="flex justify-end gap-2">
              <button onClick={() => setDeleteTarget(null)}
                className="px-4 py-2 bg-surface-container text-on-surface text-xs font-medium rounded-lg">Cancel</button>
              <button
                onClick={async () => { await deleteMut.mutateAsync(deleteTarget); setDeleteTarget(null); }}
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
