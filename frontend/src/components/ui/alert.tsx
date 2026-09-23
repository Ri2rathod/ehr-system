import * as React from "react";

type AlertVariant = "default" | "destructive";

export function Alert({
  className = "",
  variant = "default",
  children,
}: {
  className?: string;
  variant?: AlertVariant;
  children: React.ReactNode;
}) {
  const variantClass =
    variant === "destructive"
      ? "border-error/30 bg-error-container/30 text-on-error-container"
      : "border-outline-variant bg-surface-container-low text-on-surface";

  return <div className={`rounded-md border p-3 text-sm ${variantClass} ${className}`}>{children}</div>;
}

export function AlertTitle({ className = "", children }: { className?: string; children: React.ReactNode }) {
  return <h5 className={`font-bold ${className}`}>{children}</h5>;
}

export function AlertDescription({ className = "", children }: { className?: string; children: React.ReactNode }) {
  return <div className={`mt-1 text-xs ${className}`}>{children}</div>;
}
