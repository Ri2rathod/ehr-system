import { PatientChartShell } from "@/modules/patients/views/patient-chart-shell";

export default async function PatientAuditPage({ params }: { params: Promise<{ uuid: string }> }) {
  const { uuid } = await params;
  return <PatientChartShell uuid={uuid} active="audit">Audit log workspace</PatientChartShell>;
}
