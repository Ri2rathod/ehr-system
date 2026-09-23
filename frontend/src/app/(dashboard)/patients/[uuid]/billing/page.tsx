import { PatientChartShell } from "@/modules/patients/views/patient-chart-shell";

export default async function PatientBillingPage({ params }: { params: Promise<{ uuid: string }> }) {
  const { uuid } = await params;
  return <PatientChartShell uuid={uuid} active="billing">Billing workspace</PatientChartShell>;
}
