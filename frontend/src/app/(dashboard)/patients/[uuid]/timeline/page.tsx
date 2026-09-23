import { PatientChartShell } from "@/modules/patients/views/patient-chart-shell";

export default async function PatientTimelinePage({ params }: { params: Promise<{ uuid: string }> }) {
  const { uuid } = await params;
  return <PatientChartShell uuid={uuid} active="timeline">Clinical timeline workspace</PatientChartShell>;
}
