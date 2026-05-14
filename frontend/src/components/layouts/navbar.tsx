'use client';

import React, { useEffect, useRef, useState } from 'react';
import { Bell, Search, ChevronDown } from 'lucide-react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { authApi } from '@/modules/auth/api/auth.api';
import { useAuthStore } from '@/modules/auth/store/auth.store';

export function Navbar() {
  const [isUserMenuOpen, setIsUserMenuOpen] = useState(false);
  const [isLoggingOut, setIsLoggingOut] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);
  const router = useRouter();
  const clearAuth = useAuthStore((state) => state.logout);

  useEffect(() => {
    const onClickOutside = (event: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        setIsUserMenuOpen(false);
      }
    };

    document.addEventListener('mousedown', onClickOutside);
    return () => document.removeEventListener('mousedown', onClickOutside);
  }, []);

  const onLogout = async () => {
    if (isLoggingOut) return;
    setIsLoggingOut(true);
    try {
      await authApi.logout();
    } finally {
      clearAuth();
      setIsUserMenuOpen(false);
      router.replace('/login');
      setIsLoggingOut(false);
    }
  };

  return (
    <header className="h-16 border-b border-outline-variant bg-surface-container-lowest flex items-center justify-between px-8 z-10 shrink-0">
      <div className="flex items-center gap-6 flex-1">
        <div className="relative max-w-md w-full">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-outline" />
          <input
            type="text"
            placeholder="Search patients, charts, or records (⌘+K)"
            className="w-full bg-surface-container-low border border-outline-variant rounded-full py-1.5 pl-10 pr-4 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 transition-all"
          />
        </div>
      </div>
      
      <div className="flex items-center gap-4">
        <button className="relative p-2 text-on-surface-variant hover:bg-surface-container rounded-full transition-all">
          <Bell className="h-5 w-5" />
          <span className="absolute top-1.5 right-1.5 w-2 h-2 bg-error rounded-full border-2 border-surface-container-lowest"></span>
        </button>
        <div className="h-8 w-px bg-outline-variant"></div>
        <div className="relative" ref={menuRef}>
          <button
            onClick={() => setIsUserMenuOpen((prev) => !prev)}
            className="flex items-center gap-2 pl-2 pr-1 py-1 hover:bg-surface-container rounded-lg transition-all group"
          >
            <div className="h-8 w-8 rounded-full bg-primary-container flex items-center justify-center text-primary font-bold text-xs border border-primary/10">
              SC
            </div>
            <ChevronDown className="h-4 w-4 text-outline group-hover:text-on-surface" />
          </button>

          {isUserMenuOpen && (
            <div className="absolute right-0 mt-2 w-48 overflow-hidden rounded-lg border border-outline-variant bg-surface shadow-lg">
              <div className="px-3 py-2 border-b border-outline-variant">
                <p className="text-xs font-bold text-on-surface">Dr. Sarah Connor</p>
                <p className="text-[10px] text-on-surface-variant">General Internal Medicine</p>
              </div>
              <div className="py-1 text-xs">
                <Link href="#" className="block px-3 py-2 text-on-surface-variant hover:bg-surface-container hover:text-on-surface">
                  Profile
                </Link>
                <Link href="#" className="block px-3 py-2 text-on-surface-variant hover:bg-surface-container hover:text-on-surface">
                  Settings
                </Link>
                <button
                  onClick={onLogout}
                  disabled={isLoggingOut}
                  className="w-full text-left px-3 py-2 text-error hover:bg-error-container/40 disabled:opacity-50 cursor-pointer"
                >
                  {isLoggingOut ? 'Logging out...' : 'Logout'}
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
