import { PatientChartShell } from "@/modules/patients/views/patient-chart-shell";

export default async function PatientAppointmentsPage({ params }: { params: Promise<{ uuid: string }> }) {
  const { uuid } = await params;
  return <PatientChartShell uuid={uuid} active="appointments">Appointments workspace</PatientChartShell>;
}
