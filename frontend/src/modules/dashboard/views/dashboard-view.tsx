'use client';

import React from 'react';
import { 
  Activity, 
  Users, 
  Calendar, 
  Microscope, 
  Pill, 
  Settings, 
  HelpCircle, 
  Bell, 
  Search, 
  ChevronRight,
  ClipboardList,
  AlertCircle,
  Clock,
  User as UserIcon,
  ChevronDown,
  LayoutDashboard,
  ShieldCheck
} from 'lucide-react';
import Link from 'next/link';

export default function DashboardPage() {
  const recentPatients = [
    { name: 'Sarah Miller', id: 'P-9821', age: 42, condition: 'Post-Op Recovery', room: '402-A', status: 'Stable' },
    { name: 'Robert Chen', id: 'P-7732', age: 65, condition: 'Chronic COPD', room: '405-B', status: 'Observation' },
    { name: 'Elena Rodriguez', id: 'P-1104', age: 29, condition: 'Acute Appendicitis', room: '412', status: 'Urgent' },
    { name: 'James Wilson', id: 'P-5521', age: 58, condition: 'Hypertensive Crisis', room: '408-A', status: 'Stable' },
  ];

  return (
    <div className="p-8 space-y-8">
      <div className="flex items-end justify-between">
        <div>
          <p className="text-xs font-bold uppercase tracking-widest text-primary mb-1">Unit Overview</p>
          <h1 className="text-3xl font-bold text-on-surface tracking-tight">Internal Medicine Ward 4B</h1>
          <p className="text-on-surface-variant text-sm mt-1">14 Active Patients • 2 Scheduled Discharges • Unit Full</p>
        </div>
        <div className="flex gap-3">
          <button className="px-4 py-2 bg-surface-container border border-outline-variant rounded-lg text-sm font-bold text-on-surface hover:bg-surface-container-high transition-all">
            Unit Reports
          </button>
          <button className="px-4 py-2 bg-primary text-on-primary rounded-lg text-sm font-bold hover:bg-primary/90 transition-all shadow-md shadow-primary/10 flex items-center gap-2">
            <Users className="h-4 w-4" />
            Admit Patient
          </button>
        </div>
      </div>

          <div className="grid grid-cols-12 gap-8">
            {/* Left Column - Patient Queue */}
            <div className="col-span-8 space-y-8">
              <section className="bg-surface-container-lowest rounded-xl border border-outline-variant overflow-hidden shadow-sm">
                <div className="px-6 py-4 border-b border-outline-variant flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <ClipboardList className="h-5 w-5 text-primary" />
                    <h2 className="font-bold text-on-surface">Active Patient Queue</h2>
                  </div>
                  <button className="text-xs font-bold text-primary hover:underline">View All Patients</button>
                </div>
                <div className="overflow-x-auto">
                  <table className="w-full text-left border-collapse">
                    <thead className="bg-surface-container-low">
                      <tr>
                        <th className="px-6 py-3 text-[10px] font-bold uppercase tracking-wider text-on-surface-variant">Patient</th>
                        <th className="px-6 py-3 text-[10px] font-bold uppercase tracking-wider text-on-surface-variant">Room</th>
                        <th className="px-6 py-3 text-[10px] font-bold uppercase tracking-wider text-on-surface-variant">Condition</th>
                        <th className="px-6 py-3 text-[10px] font-bold uppercase tracking-wider text-on-surface-variant">Status</th>
                        <th className="px-6 py-3 text-[10px] font-bold uppercase tracking-wider text-on-surface-variant">Action</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-outline-variant">
                      {recentPatients.map((patient) => (
                        <tr key={patient.id} className="hover:bg-surface-container-lowest group transition-all">
                          <td className="px-6 py-4">
                            <div className="flex items-center gap-3">
                              <div className="h-8 w-8 rounded-full bg-surface-container-high flex items-center justify-center text-on-surface-variant">
                                <UserIcon className="h-4 w-4" />
                              </div>
                              <div>
                                <p className="text-sm font-bold text-on-surface">{patient.name}</p>
                                <p className="text-[10px] text-outline">{patient.id} • {patient.age}y</p>
                              </div>
                            </div>
                          </td>
                          <td className="px-6 py-4">
                            <span className="text-xs font-medium text-on-surface-variant">{patient.room}</span>
                          </td>
                          <td className="px-6 py-4">
                            <span className="text-xs text-on-surface-variant">{patient.condition}</span>
                          </td>
                          <td className="px-6 py-4">
                            <span className={`inline-flex items-center px-2 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider ${
                              patient.status === 'Urgent' ? 'bg-error-container text-on-error-container' : 
                              patient.status === 'Observation' ? 'bg-tertiary-container text-on-tertiary-container' : 
                              'bg-surface-container-high text-on-surface-variant'
                            }`}>
                              {patient.status}
                            </span>
                          </td>
                          <td className="px-6 py-4">
                            <button className="p-1.5 hover:bg-primary-container hover:text-primary rounded-md transition-all text-outline">
                              <ChevronRight className="h-4 w-4" />
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </section>

              <div className="grid grid-cols-2 gap-8">
                 <section className="bg-surface-container-lowest rounded-xl border border-outline-variant overflow-hidden shadow-sm">
                    <div className="px-6 py-4 border-b border-outline-variant flex items-center gap-2">
                      <Microscope className="h-5 w-5 text-primary" />
                      <h2 className="font-bold text-on-surface">Recent Labs</h2>
                    </div>
                    <div className="p-6 space-y-4">
                      {[1, 2, 3].map((i) => (
                        <div key={i} className="flex items-center justify-between p-3 rounded-lg border border-outline-variant hover:bg-surface-container-low transition-all cursor-pointer">
                          <div className="flex items-center gap-3">
                            <div className="p-2 bg-tertiary-container rounded-md">
                              <Activity className="h-4 w-4 text-tertiary" />
                            </div>
                            <div>
                              <p className="text-xs font-bold text-on-surface">Metabolic Panel {i === 1 ? '(CMP)' : '(CBC)'}</p>
                              <p className="text-[10px] text-outline">Patient: {recentPatients[i-1].name}</p>
                            </div>
                          </div>
                          <span className={`text-[10px] font-bold ${i === 1 ? 'text-error' : 'text-primary'}`}>
                            {i === 1 ? 'Abnormal' : 'Normal'}
                          </span>
                        </div>
                      ))}
                    </div>
                 </section>

                 <section className="bg-surface-container-lowest rounded-xl border border-outline-variant overflow-hidden shadow-sm">
                    <div className="px-6 py-4 border-b border-outline-variant flex items-center gap-2">
                      <Clock className="h-5 w-5 text-primary" />
                      <h2 className="font-bold text-on-surface">Upcoming Tasks</h2>
                    </div>
                    <div className="p-6 space-y-4">
                      {[
                        { time: '14:30', task: 'Medication Admin (Bed 402)', priority: 'High' },
                        { time: '15:00', task: 'Wound Dressing (Bed 405)', priority: 'Medium' },
                        { time: '16:00', task: 'Discharge Prep (Bed 412)', priority: 'Low' },
                      ].map((task, i) => (
                        <div key={i} className="flex items-center gap-4">
                          <span className="text-xs font-bold text-primary w-10">{task.time}</span>
                          <div className="flex-1 p-3 rounded-lg bg-surface-container-low border-l-4 border-primary">
                            <p className="text-xs font-bold text-on-surface">{task.task}</p>
                          </div>
                        </div>
                      ))}
                    </div>
                 </section>
              </div>
            </div>

            {/* Right Column - Status & Notes */}
            <div className="col-span-4 space-y-8">
              <section className="bg-primary text-on-primary rounded-xl p-6 shadow-lg shadow-primary/20 relative overflow-hidden">
                <Activity className="absolute -right-4 -bottom-4 h-32 w-32 opacity-10" />
                <h3 className="text-xs font-bold uppercase tracking-widest opacity-80 mb-4">Unit Health</h3>
                <div className="space-y-6">
                  <div>
                    <div className="flex justify-between text-xs mb-2">
                      <span className="opacity-80">Bed Occupancy</span>
                      <span className="font-bold">100%</span>
                    </div>
                    <div className="h-1.5 w-full bg-white/20 rounded-full overflow-hidden">
                      <div className="h-full bg-white w-full"></div>
                    </div>
                  </div>
                  <div>
                    <div className="flex justify-between text-xs mb-2">
                      <span className="opacity-80">Nursing Ratio</span>
                      <span className="font-bold text-on-primary">1:4</span>
                    </div>
                    <div className="h-1.5 w-full bg-white/20 rounded-full overflow-hidden">
                      <div className="h-full bg-white w-3/4"></div>
                    </div>
                  </div>
                </div>
              </section>

              <section className="bg-surface-container-lowest rounded-xl border border-outline-variant overflow-hidden shadow-sm">
                <div className="px-6 py-4 border-b border-outline-variant flex items-center gap-2 bg-surface-container-high/30">
                  <AlertCircle className="h-5 w-5 text-error" />
                  <h2 className="font-bold text-on-surface">Shift Notes</h2>
                </div>
                <div className="p-6 space-y-4">
                  <div className="p-4 rounded-lg bg-error-container/20 border border-error/10">
                    <p className="text-xs font-bold text-error mb-1">Equipment Alert</p>
                    <p className="text-xs text-on-surface-variant leading-relaxed">
                      CT Scanner 2 is down for maintenance until 14:00. Route stats to Scanner 1.
                    </p>
                  </div>
                  <div className="p-4 rounded-lg bg-surface-container-low border border-outline-variant">
                    <p className="text-xs font-bold text-on-surface mb-1">Coverage Update</p>
                    <p className="text-xs text-on-surface-variant leading-relaxed">
                      Dr. Chen covering beds 402-408 this afternoon.
                    </p>
                  </div>
                  <button className="w-full py-2 text-xs font-bold text-primary border border-primary/20 rounded-lg hover:bg-primary/5 transition-all">
                    Add Shift Note
                  </button>
                </div>
              </section>
            </div>
          </div>
        </div>
  );
}
