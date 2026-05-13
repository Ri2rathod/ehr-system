'use client';

import React from 'react';
import { 
  Activity, 
  LayoutDashboard, 
  Users, 
  Calendar, 
  Microscope, 
  Pill, 
  Settings, 
  HelpCircle,
  ShieldCheck
} from 'lucide-react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';

const sidebarItems = [
  { icon: LayoutDashboard, label: 'Dashboard', href: '/dashboard' },
  { icon: Users, label: 'Patient Records', href: '/patients' },
  { icon: Calendar, label: 'Schedule', href: '/appointments' },
  { icon: Microscope, label: 'Labs & Imaging', href: '/encounters' },
  { icon: Pill, label: 'Pharmacy', href: '/billing' }, // Placeholder links
  { icon: Settings, label: 'Settings', href: '#' },
  { icon: HelpCircle, label: 'Support', href: '#' },
];

export function Sidebar() {
  const pathname = usePathname();

  return (
    <aside className="w-64 border-r border-outline-variant bg-surface-container-low flex flex-col h-full">
      <div className="p-6 border-b border-outline-variant">
        <Link href="/dashboard" className="flex items-center gap-2 text-primary">
          <Activity className="h-6 w-6" />
          <span className="text-xl font-bold tracking-tight">ClinicalOS</span>
        </Link>
      </div>
      
      <nav className="flex-1 p-4 space-y-1 overflow-y-auto">
        {sidebarItems.map((item) => {
          const isActive = pathname === item.href;
          return (
            <Link
              key={item.label}
              href={item.href}
              className={`flex items-center gap-3 px-4 py-3 rounded-lg transition-all ${
                isActive 
                  ? 'bg-primary text-on-primary font-bold shadow-md shadow-primary/20' 
                  : 'text-on-surface-variant hover:bg-surface-container hover:text-on-surface'
              }`}
            >
              <item.icon className={`h-5 w-5 ${isActive ? 'text-white' : 'text-outline'}`} />
              <span className="text-sm">{item.label}</span>
            </Link>
          );
        })}
      </nav>

      <div className="p-4 mt-auto">
        <div className="bg-tertiary-container text-on-tertiary-container p-4 rounded-xl border border-tertiary/10">
          <div className="flex items-center gap-3 mb-2">
            <ShieldCheck className="h-5 w-5 text-tertiary" />
            <span className="text-xs font-bold uppercase tracking-wider">Clinical Auth</span>
          </div>
          <p className="text-[10px] leading-relaxed opacity-80">
            Dr. Sarah Connor<br/>
            General Internal Medicine<br/>
            Session expires in 4h 12m
          </p>
        </div>
      </div>
    </aside>
  );
}
