'use client';

import React from 'react';
import { Mail, ChevronRight, Activity, ShieldCheck, CreditCard, Building } from 'lucide-react';
import Link from 'next/link';

export default function RegisterPage() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-surface p-4">
      <div className="flex w-full max-w-5xl overflow-hidden rounded-xl border border-outline-variant bg-surface-container-lowest shadow-xl">
        {/* Left Side - Info */}
        <div className="hidden w-1/2 bg-tertiary p-12 text-on-tertiary lg:flex lg:flex-col lg:justify-between">
          <div>
            <div className="mb-8 flex items-center gap-2">
              <Activity className="h-8 w-8" />
              <span className="text-2xl font-bold tracking-tight text-white">ClinicalOS</span>
            </div>
            <h1 className="display-lg mb-6 text-4xl font-bold leading-tight text-white">
              Precision clinical workflows.
            </h1>
            <p className="body-base mb-8 text-lg opacity-90">
              Join the secure network designed for high-density patient data management and authoritative care coordination.
            </p>
          </div>
          <div className="space-y-4">
            <div className="flex items-start gap-4 rounded-lg bg-white/5 p-4 border border-white/10">
              <ShieldCheck className="h-6 w-6 text-white mt-1 shrink-0" />
              <div className="text-sm">
                <p className="font-bold text-white">Identity Verification</p>
                <p className="opacity-80">Your professional identity is verified securely against the national registry.</p>
              </div>
            </div>
            <div className="flex items-start gap-4 rounded-lg bg-white/5 p-4 border border-white/10">
              <Building className="h-6 w-6 text-white mt-1 shrink-0" />
              <div className="text-sm">
                <p className="font-bold text-white">EHR Compliance</p>
                <p className="opacity-80">All data is handled with strict HIPAA and healthcare security standards.</p>
              </div>
            </div>
          </div>
        </div>

        {/* Right Side - Register Form */}
        <div className="flex w-full flex-col justify-center p-8 lg:w-1/2 lg:p-16">
          <div className="mb-10 lg:hidden">
            <div className="mb-4 flex items-center gap-2 text-primary">
              <Activity className="h-6 w-6" />
              <span className="text-xl font-bold tracking-tight">ClinicalOS</span>
            </div>
          </div>

          <div className="mb-8">
            <h2 className="headline-md mb-2 text-2xl font-bold text-on-surface">Provider Registration</h2>
            <p className="body-base text-on-surface-variant">
              Enter your credentials to establish your secure clinical profile.
            </p>
          </div>

          <form className="space-y-5" onSubmit={(e) => e.preventDefault()}>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1">
                <label className="label-caps block text-[10px] font-bold uppercase tracking-wider text-on-surface-variant">
                  First Name
                </label>
                <input
                  type="text"
                  placeholder="John"
                  className="w-full rounded-md border border-outline-variant bg-surface-container-low px-3 py-2.5 outline-none transition-all focus:border-primary focus:ring-2 focus:ring-primary/20"
                />
              </div>
              <div className="space-y-1">
                <label className="label-caps block text-[10px] font-bold uppercase tracking-wider text-on-surface-variant">
                  Last Name
                </label>
                <input
                  type="text"
                  placeholder="Doe"
                  className="w-full rounded-md border border-outline-variant bg-surface-container-low px-3 py-2.5 outline-none transition-all focus:border-primary focus:ring-2 focus:ring-primary/20"
                />
              </div>
            </div>

            <div className="space-y-1">
              <label className="label-caps block text-[10px] font-bold uppercase tracking-wider text-on-surface-variant">
                Professional Email
              </label>
              <div className="relative">
                <Mail className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-outline" />
                <input
                  type="email"
                  placeholder="j.doe@medical-center.org"
                  className="w-full rounded-md border border-outline-variant bg-surface-container-low py-2.5 pl-10 pr-4 outline-none transition-all focus:border-primary focus:ring-2 focus:ring-primary/20"
                />
              </div>
            </div>

            <div className="space-y-1">
              <label className="label-caps block text-[10px] font-bold uppercase tracking-wider text-on-surface-variant">
                NPI Number (National Provider Identifier)
              </label>
              <div className="relative">
                <CreditCard className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-outline" />
                <input
                  type="text"
                  placeholder="10-digit numeric identifier"
                  className="w-full rounded-md border border-outline-variant bg-surface-container-low py-2.5 pl-10 pr-4 outline-none transition-all focus:border-primary focus:ring-2 focus:ring-primary/20"
                />
              </div>
            </div>

            <div className="space-y-1">
              <label className="label-caps block text-[10px] font-bold uppercase tracking-wider text-on-surface-variant">
                Password
              </label>
              <input
                type="password"
                placeholder="Minimum 12 characters"
                className="w-full rounded-md border border-outline-variant bg-surface-container-low px-3 py-2.5 outline-none transition-all focus:border-primary focus:ring-2 focus:ring-primary/20"
              />
            </div>

            <div className="pt-2">
              <button
                type="submit"
                className="flex w-full items-center justify-center gap-2 rounded-md bg-primary py-3 font-bold text-on-primary transition-all hover:bg-primary/90 shadow-lg shadow-primary/20"
              >
                Create Clinical Profile
                <ChevronRight className="h-5 w-5" />
              </button>
            </div>
          </form>

          <div className="mt-8 text-center text-sm">
            <p className="text-on-surface-variant">
              Already registered?{' '}
              <Link href="/login" className="font-bold text-primary hover:underline">
                Sign in to workspace
              </Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
