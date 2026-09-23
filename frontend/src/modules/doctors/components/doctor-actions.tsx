import Link from "next/link";

export function DoctorActions({ uuid }: { uuid: string }) {
  return (
    <div className="flex flex-wrap items-center gap-1 text-[10px]">
      <Link href={`/doctors/${uuid}`} className="rounded border border-outline-variant px-2 py-1 font-bold text-primary">View Profile</Link>
      <Link href={`/doctors/${uuid}/edit`} className="rounded border border-outline-variant px-2 py-1">Edit Doctor</Link>
      <Link href={`/doctors/${uuid}/schedule`} className="rounded border border-outline-variant px-2 py-1">Schedule</Link>
    </div>
  );
}
