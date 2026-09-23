"use client";

type SessionExpiredModalProps = {
  open: boolean;
  onLoginAgain: () => void;
};

export function SessionExpiredModal({ open, onLoginAgain }: SessionExpiredModalProps) {
  if (!open) return null;

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/40 p-4">
      <div className="w-full max-w-md rounded-lg border border-outline-variant bg-surface-container-lowest p-5 shadow-xl">
        <h2 className="text-base font-bold text-on-surface">Session Expired</h2>
        <p className="mt-2 text-sm text-on-surface-variant">
          Your session has expired. Please login again to continue.
        </p>
        <div className="mt-5 flex justify-end">
          <button
            onClick={onLoginAgain}
            className="h-9 rounded-md bg-primary px-4 text-xs font-bold text-on-primary"
          >
            Login Again
          </button>
        </div>
      </div>
    </div>
  );
}
