"use client";

import React, { useEffect } from "react";
import { useAuthStore } from "@/modules/auth/store/auth.store";
import { authApi } from "@/modules/auth/api/auth.api";
import { usePathname, useRouter } from "next/navigation";
import { Loader2 } from "lucide-react";

const publicRoutes = ["/login", "/register", "/forgot-password"];

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const { user, setUser, setLoading, isLoading, logout } = useAuthStore();
  const pathname = usePathname();
  const router = useRouter();

  useEffect(() => {
    const initAuth = async () => {
      try {
        setLoading(true);
        const userData = await authApi.me();
        setUser(userData);
      } catch (error) {
        logout();
        // Redirect to login if on a protected route
        if (!publicRoutes.includes(pathname)) {
          router.push("/login");
        }
      } finally {
        setLoading(false);
      }
    };

    initAuth();
  }, [setUser, setLoading, logout, pathname, router]);

  if (isLoading) {
    return (
      <div className="flex h-screen w-full items-center justify-center bg-surface">
        <div className="flex flex-col items-center gap-4">
          <Loader2 className="h-8 w-8 animate-spin text-primary" />
          <p className="text-sm font-bold text-on-surface-variant tracking-wider uppercase">
            Validating Clinical Session...
          </p>
        </div>
      </div>
    );
  }

  return <>{children}</>;
}
