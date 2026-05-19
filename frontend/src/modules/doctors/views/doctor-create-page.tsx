import { DoctorForm } from "../forms/doctor-form";

export default function DoctorCreatePage() {
  return (
    <div className="mx-auto w-full max-w-[1400px] space-y-4 p-5">
      <h1 className="text-xl font-bold">Create Doctor</h1>
      <DoctorForm />
    </div>
  );
}
