import { PatientChartShell } from "@/modules/patients/views/patient-chart-shell";

export default async function PatientPrescriptionsPage({ params }: { params: Promise<{ uuid: string }> }) {
  const { uuid } = await params;
  return <PatientChartShell uuid={uuid} active="prescriptions">Prescriptions workspace</PatientChartShell>;
}
