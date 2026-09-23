import AppointmentDetailsPage from "@/modules/appointments/views/appointment-details-page";
export default async function Page({ params }: { params: Promise<{ uuid: string }> }) { const { uuid } = await params; return <AppointmentDetailsPage uuid={uuid} />; }
