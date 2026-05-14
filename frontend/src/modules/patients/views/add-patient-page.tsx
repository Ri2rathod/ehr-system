"use client";

import React from "react";
import { PatientForm } from "../forms/patient-form";

export default function AddPatientPage() {
  return (
    <div className="mx-auto w-full max-w-[1400px] space-y-4 p-5">
      <header className="space-y-1">
        <h1 className="text-xl font-bold text-on-surface">Add Patient</h1>
        <p className="text-xs text-on-surface-variant">Register a new patient into the system</p>
      </header>
      <PatientForm />
    </div>
  );
}
