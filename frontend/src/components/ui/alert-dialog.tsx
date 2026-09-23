"use client";

import * as React from "react";

export function AlertDialog({ open, children }: { open: boolean; children: React.ReactNode }) {
  if (!open) return null;
  return <>{children}</>;
}

export function AlertDialogContent({ children }: { children: React.ReactNode }) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <div className="w-full max-w-md rounded-lg border border-outline-variant bg-surface-container-lowest p-4 shadow-xl">{children}</div>
    </div>
  );
}

export function AlertDialogHeader({ children }: { children: React.ReactNode }) {
  return <div>{children}</div>;
}

export function AlertDialogTitle({ children }: { children: React.ReactNode }) {
  return <h3 className="text-sm font-bold text-on-surface">{children}</h3>;
}

export function AlertDialogDescription({ children }: { children: React.ReactNode }) {
  return <p className="mt-1 text-xs text-on-surface-variant">{children}</p>;
}

export function AlertDialogFooter({ children }: { children: React.ReactNode }) {
  return <div className="mt-4 flex justify-end gap-2">{children}</div>;
}

export function AlertDialogAction({
  onClick,
  children,
}: {
  onClick?: () => void;
  children: React.ReactNode;
}) {
  return (
    <button onClick={onClick} className="h-8 rounded-md bg-primary px-3 text-xs font-bold text-on-primary">
      {children}
    </button>
  );
}
