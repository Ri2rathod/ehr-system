"use client";

import { useState } from "react";
import { useEncounterVitals, useRecordVitals } from "../hooks/use-encounters";
import { RecordVitalsPayload } from "../types/encounter.types";
import { formatEncounterDateTime } from "../utils/encounter-format";

interface VitalsSectionProps {
  encounterUuid: string;
  editable: boolean;
}

interface VitalsForm {
  temperature: string;
  heartRate: string;
  respiratoryRate: string;
  systolicBp: string;
  diastolicBp: string;
  oxygenSaturation: string;
  weight: string;
  height: string;
}

const emptyForm: VitalsForm = {
  temperature: "",
  heartRate: "",
  respiratoryRate: "",
  systolicBp: "",
  diastolicBp: "",
  oxygenSaturation: "",
  weight: "",
  height: "",
};

const fields: { key: keyof VitalsForm; label: string; unit: string; step?: string }[] = [
  { key: "temperature", label: "Temperature", unit: "°C", step: "0.1" },
  { key: "heartRate", label: "Heart rate", unit: "bpm" },
  { key: "respiratoryRate", label: "Resp. rate", unit: "/min" },
  { key: "systolicBp", label: "Systolic BP", unit: "mmHg" },
  { key: "diastolicBp", label: "Diastolic BP", unit: "mmHg" },
  { key: "oxygenSaturation", label: "SpO₂", unit: "%", step: "0.1" },
  { key: "weight", label: "Weight", unit: "kg", step: "0.1" },
  { key: "height", label: "Height", unit: "cm", step: "0.1" },
];

function toNumber(value: string): number | undefined {
  if (value.trim() === "") return undefined;
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : undefined;
}

export function VitalsSection({ encounterUuid, editable }: VitalsSectionProps) {
  const { data: vitals, isLoading } = useEncounterVitals(encounterUuid);
  const recordVitals = useRecordVitals();
  const [form, setForm] = useState<VitalsForm>(emptyForm);
  const [showForm, setShowForm] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const latest = vitals?.[0];

  const setField = (key: keyof VitalsForm, value: string) => {
    setForm((prev) => ({ ...prev, [key]: value }));
    setNotice("");
  };

  const handleSave = async () => {
    setError("");
    setNotice("");
    const payload: RecordVitalsPayload = {};
    const temperature = toNumber(form.temperature);
    const heartRate = toNumber(form.heartRate);
    const respiratoryRate = toNumber(form.respiratoryRate);
    const systolicBp = toNumber(form.systolicBp);
    const diastolicBp = toNumber(form.diastolicBp);
    const oxygenSaturation = toNumber(form.oxygenSaturation);
    const weight = toNumber(form.weight);
    const height = toNumber(form.height);
    if (temperature !== undefined) payload.temperature = temperature;
    if (heartRate !== undefined) payload.heartRate = heartRate;
    if (respiratoryRate !== undefined) payload.respiratoryRate = respiratoryRate;
    if (systolicBp !== undefined) payload.systolicBp = systolicBp;
    if (diastolicBp !== undefined) payload.diastolicBp = diastolicBp;
    if (oxygenSaturation !== undefined) payload.oxygenSaturation = oxygenSaturation;
    if (weight !== undefined) payload.weight = weight;
    if (height !== undefined) payload.height = height;

    if (Object.keys(payload).length === 0) {
      setError("Enter at least one vital value.");
      return;
    }
    if ((systolicBp === undefined) !== (diastolicBp === undefined)) {
      setError("Both systolic and diastolic blood pressure are required together.");
      return;
    }
    if (systolicBp !== undefined && diastolicBp !== undefined && systolicBp < diastolicBp) {
      setError("Systolic blood pressure must be greater than or equal to diastolic.");
      return;
    }

    try {
      await recordVitals.mutateAsync({ uuid: encounterUuid, payload });
      setForm(emptyForm);
      setShowForm(false);
      setNotice("Vitals recorded.");
    } catch (caught: unknown) {
      const response = (caught as { response?: { status?: number; data?: { message?: string } } })?.response;
      if (response?.status === 403) setError("You do not have permission to record vitals.");
      else setError(String(response?.data?.message || "Unable to record vitals."));
    }
  };

  const tiles = latest
    ? [
        ["Temperature", latest.temperature !== undefined && latest.temperature !== null ? `${latest.temperature} °C` : "—"],
        ["Heart rate", latest.heartRate != null ? `${latest.heartRate} bpm` : "—"],
        ["Resp. rate", latest.respiratoryRate != null ? `${latest.respiratoryRate} /min` : "—"],
        ["Blood pressure", latest.systolicBp != null && latest.diastolicBp != null ? `${latest.systolicBp}/${latest.diastolicBp} mmHg` : "—"],
        ["SpO₂", latest.oxygenSaturation != null ? `${latest.oxygenSaturation} %` : "—"],
        ["Weight", latest.weight != null ? `${latest.weight} kg` : "—"],
        ["Height", latest.height != null ? `${latest.height} cm` : "—"],
        ["BMI", latest.bmi != null ? `${latest.bmi}` : "—"],
      ]
    : [];

  return (
    <section className="rounded-lg border border-outline-variant bg-surface-container-lowest p-4">
      <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
        <div>
          <h2 className="text-xs font-bold uppercase tracking-wider text-on-surface-variant">
            Vitals
          </h2>
          {latest && (
            <p className="mt-1 text-[10px] text-on-surface-variant">
              Latest recorded {formatEncounterDateTime(latest.recordedAt || latest.createdAt)}
            </p>
          )}
        </div>
        {editable && (
          <button
            onClick={() => {
              setShowForm((value) => !value);
              setError("");
              setNotice("");
            }}
            className="h-8 rounded border border-outline-variant px-3 text-xs font-bold text-primary"
          >
            {showForm ? "Close" : "Record vitals"}
          </button>
        )}
      </div>

      {notice && (
        <div className="mb-3 rounded-md border border-emerald-200 bg-emerald-50 p-2 text-xs text-emerald-900">
          {notice}
        </div>
      )}
      {error && (
        <div role="alert" className="mb-3 rounded-md border border-error/30 bg-error-container/30 p-2 text-xs text-on-error-container">
          {error}
        </div>
      )}

      {isLoading ? (
        <p className="text-xs text-on-surface-variant">Loading vitals…</p>
      ) : !latest ? (
        <p className="text-xs text-on-surface-variant">No vitals recorded for this encounter.</p>
      ) : (
        <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
          {tiles.map(([label, value]) => (
            <div key={String(label)} className="rounded-md border border-outline-variant bg-surface-container-low p-2">
              <p className="text-[10px] font-bold uppercase tracking-wide text-on-surface-variant">{label}</p>
              <p className="mt-1 font-mono text-sm font-bold text-on-surface">{String(value)}</p>
            </div>
          ))}
        </div>
      )}

      {editable && showForm && (
        <div className="mt-4 border-t border-outline-variant pt-4">
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
            {fields.map((field) => (
              <label key={field.key} className="text-xs text-on-surface-variant">
                {field.label}
                <input
                  type="number"
                  step={field.step || "1"}
                  inputMode="decimal"
                  value={form[field.key]}
                  onChange={(event) => setField(field.key, event.target.value)}
                  placeholder={field.unit}
                  className="mt-1 block h-9 w-full rounded border border-outline-variant bg-surface px-2 font-mono text-sm text-on-surface"
                />
              </label>
            ))}
          </div>
          <div className="mt-3 flex items-center gap-2">
            <button
              onClick={handleSave}
              disabled={recordVitals.isPending}
              className="h-9 rounded-md bg-primary px-3 text-xs font-bold text-on-primary disabled:opacity-50"
            >
              {recordVitals.isPending ? "Saving…" : "Save vitals"}
            </button>
            <span className="text-[10px] text-on-surface-variant">
              Temperature in °C, weight in kg, height in cm. BMI is calculated automatically.
            </span>
          </div>
        </div>
      )}
    </section>
  );
}
