import { EncounterWorkspace } from "@/modules/encounters/views/encounter-workspace";

export default async function Page({ params }: { params: Promise<{ uuid: string }> }) {
  const { uuid } = await params;
  return <EncounterWorkspace uuid={uuid} />;
}
