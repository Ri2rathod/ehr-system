import DoctorEditPage from "@/modules/doctors/views/doctor-edit-page";

export default async function Page({ params }: { params: Promise<{ uuid: string }> }) {
  const { uuid } = await params;
  return <DoctorEditPage uuid={uuid} />;
}
