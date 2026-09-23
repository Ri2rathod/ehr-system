import DoctorDetailsPage from "@/modules/doctors/views/doctor-details-page";

export default async function Page({ params }: { params: Promise<{ uuid: string }> }) {
  const { uuid } = await params;
  return <DoctorDetailsPage uuid={uuid} />;
}
