"use client";

import React from "react";

interface PatientToolbarProps {
  search: string;
  status: string;
  gender: string;
  bloodGroup: string;
  onSearchChange: (value: string) => void;
  onStatusChange: (value: string) => void;
  onGenderChange: (value: string) => void;
  onBloodGroupChange: (value: string) => void;
}

export function PatientToolbar(props: PatientToolbarProps) {
  const {
    search,
    status,
    gender,
    bloodGroup,
    onSearchChange,
    onStatusChange,
    onGenderChange,
    onBloodGroupChange,
  } = props;

  return (
    <section className="rounded-lg border border-outline-variant bg-surface-container-lowest p-3">
      <div className="flex flex-wrap items-center gap-2">
        <input
          value={search}
          onChange={(e) => onSearchChange(e.target.value)}
          placeholder="Search by MRN, name, phone, or email"
          className="h-9 w-full min-w-[260px] flex-1 rounded-md border border-outline-variant bg-surface px-3 text-sm outline-none focus:border-primary"
        />

        <select
          value={status}
          onChange={(e) => onStatusChange(e.target.value)}
          className="h-9 rounded-md border border-outline-variant bg-surface px-2 text-xs font-medium"
        >
          <option value="">All Status</option>
          <option value="ACTIVE">Active</option>
          <option value="INACTIVE">Inactive</option>
          <option value="ARCHIVED">Archived</option>
          <option value="BLOCKED">Blocked</option>
          <option value="DECEASED">Deceased</option>
        </select>

        <select
          value={gender}
          onChange={(e) => onGenderChange(e.target.value)}
          className="h-9 rounded-md border border-outline-variant bg-surface px-2 text-xs font-medium"
        >
          <option value="">All Gender</option>
          <option value="MALE">Male</option>
          <option value="FEMALE">Female</option>
          <option value="OTHER">Other</option>
          <option value="UNKNOWN">Unknown</option>
        </select>

        <select
          value={bloodGroup}
          onChange={(e) => onBloodGroupChange(e.target.value)}
          className="h-9 rounded-md border border-outline-variant bg-surface px-2 text-xs font-medium"
        >
          <option value="">All Blood Groups</option>
          <option value="A_POSITIVE">A+</option>
          <option value="A_NEGATIVE">A-</option>
          <option value="B_POSITIVE">B+</option>
          <option value="B_NEGATIVE">B-</option>
          <option value="AB_POSITIVE">AB+</option>
          <option value="AB_NEGATIVE">AB-</option>
          <option value="O_POSITIVE">O+</option>
          <option value="O_NEGATIVE">O-</option>
        </select>
      </div>
    </section>
  );
}
