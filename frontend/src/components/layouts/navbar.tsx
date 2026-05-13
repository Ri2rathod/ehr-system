'use client';

import React from 'react';
import { Bell, Search, ChevronDown } from 'lucide-react';

export function Navbar() {
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
        <button className="flex items-center gap-2 pl-2 pr-1 py-1 hover:bg-surface-container rounded-lg transition-all group">
          <div className="h-8 w-8 rounded-full bg-primary-container flex items-center justify-center text-primary font-bold text-xs border border-primary/10">
            SC
          </div>
          <ChevronDown className="h-4 w-4 text-outline group-hover:text-on-surface" />
        </button>
      </div>
    </header>
  );
}
