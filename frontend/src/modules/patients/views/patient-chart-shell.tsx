import Link from "next/link";

const tabs = [
  { key: "overview", label: "Overview", href: "" },
  { key: "appointments", label: "Appointments", href: "/appointments" },
  { key: "encounters", label: "Encounters", href: "/encounters" },
  { key: "prescriptions", label: "Prescriptions", href: "/prescriptions" },
  { key: "documents", label: "Documents", href: "/documents" },
  { key: "billing", label: "Billing", href: "/billing" },
  { key: "timeline", label: "Timeline", href: "/timeline" },
  { key: "audit", label: "Audit", href: "/audit" },
];

export function PatientChartShell({
  uuid,
  active,
  children,
}: {
  uuid: string;
  active: string;
  children: React.ReactNode;
}) {
  return (
    <div className="space-y-4 p-5">
      <div className="rounded-lg border border-outline-variant bg-surface-container-lowest p-4">
        <p className="text-[11px] text-on-surface-variant">Patients &gt; {uuid}</p>
        <div className="mt-2 flex flex-wrap items-center justify-between gap-3">
          <div>
            <h1 className="text-xl font-bold text-on-surface">Patient Chart</h1>
            <p className="text-xs text-on-surface-variant">UUID: {uuid}</p>
          </div>
          <div className="flex flex-wrap gap-2">
            <Link href={`/encounters/new?patient=${uuid}`} className="rounded-md border border-outline-variant px-3 py-2 text-xs font-semibold">New Encounter</Link>
            <Link href={`/appointments/new?patient=${uuid}`} className="rounded-md border border-outline-variant px-3 py-2 text-xs font-semibold">New Appointment</Link>
            <Link href={`/prescriptions/new?patient=${uuid}`} className="rounded-md bg-primary px-3 py-2 text-xs font-bold text-on-primary">Prescribe</Link>
          </div>
        </div>
      </div>

      <div className="rounded-lg border border-outline-variant bg-surface-container-lowest">
        <nav className="flex flex-wrap gap-1 border-b border-outline-variant p-2">
          {tabs.map((tab) => {
            const href = `/patients/${uuid}${tab.href}`;
            const isActive = active === tab.key;
            return (
              <Link
                key={tab.key}
                href={href}
                className={`rounded px-3 py-1.5 text-xs font-semibold ${isActive ? "bg-primary text-on-primary" : "text-on-surface-variant hover:bg-surface-container-low"}`}
              >
                {tab.label}
              </Link>
            );
          })}
        </nav>
        <div className="p-4">{children}</div>
      </div>
    </div>
  );
}
