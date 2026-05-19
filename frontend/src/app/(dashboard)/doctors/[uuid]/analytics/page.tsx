export default async function Page({ params }: { params: Promise<{ uuid: string }> }) {
  const { uuid } = await params;
  return <div className="p-5 text-sm">Doctor analytics workspace for {uuid}</div>;
}
