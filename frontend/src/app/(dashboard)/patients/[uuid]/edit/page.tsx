export default async function PatientEditPage({
  params,
}: {
  params: Promise<{ uuid: string }>;
}) {
  const { uuid } = await params;
  return (
    <div className="space-y-2 p-5">
      <h1 className="text-xl font-bold text-on-surface">Edit Patient</h1>
      <p className="text-sm text-on-surface-variant">Patient UUID: {uuid}</p>
    </div>
  );
}
