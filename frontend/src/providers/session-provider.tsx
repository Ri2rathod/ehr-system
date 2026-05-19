"use client";

import { useEffect, useState } from "react";
import { useAuthStore } from "@/modules/auth/store/auth.store";
import { SessionExpiredModal } from "@/components/session/session-expired-modal";

export function SessionProvider({ children }: { children: React.ReactNode }) {
  const [isOpen, setIsOpen] = useState(false);
  const logout = useAuthStore((state) => state.logout);

  useEffect(() => {
    const onSessionExpired = () => setIsOpen(true);
    window.addEventListener("session-expired", onSessionExpired);
    return () => window.removeEventListener("session-expired", onSessionExpired);
  }, []);

  const onLoginAgain = () => {
    sessionStorage.setItem("redirectAfterLogin", window.location.pathname);
    logout();
    window.location.href = "/login";
  };

  return (
    <>
      {children}
      <SessionExpiredModal open={isOpen} onLoginAgain={onLoginAgain} />
    </>
  );
}
