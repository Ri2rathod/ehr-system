import { PatientChartShell } from "@/modules/patients/views/patient-chart-shell";

export default async function PatientOverviewPage({
  params,
}: {
  params: Promise<{ uuid: string }>;
}) {
  const { uuid } = await params;

  return (
    <PatientChartShell uuid={uuid} active="overview">
      <div className="grid grid-cols-1 gap-3 lg:grid-cols-2">
        <section className="rounded-md border border-outline-variant p-3">
          <h2 className="text-xs font-bold uppercase tracking-wider text-on-surface-variant">Demographics</h2>
          <p className="mt-2 text-sm text-on-surface">Load patient demographic summary here.</p>
        </section>
        <section className="rounded-md border border-outline-variant p-3">
          <h2 className="text-xs font-bold uppercase tracking-wider text-on-surface-variant">Allergies</h2>
          <p className="mt-2 text-sm text-on-surface">Load critical allergy flags here.</p>
        </section>
      </div>
    </PatientChartShell>
  );
}
