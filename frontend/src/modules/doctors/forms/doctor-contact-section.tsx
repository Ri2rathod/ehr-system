import { UseFormRegister } from "react-hook-form";
import { DoctorFormValues } from "../schemas/doctor.schema";

const inputCls = "h-9 w-full rounded-md border border-outline-variant bg-surface px-3 text-sm outline-none focus:border-primary";

export function DoctorContactSection({ register }: { register: UseFormRegister<DoctorFormValues> }) {
  return (
    <section className="rounded-lg border border-outline-variant bg-surface-container-lowest p-4">
      <h2 className="mb-3 text-sm font-bold">Contact Information</h2>
      <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
        <input className={inputCls} placeholder="Email" {...register("email")} />
        <input className={inputCls} placeholder="Phone" {...register("phone")} />
        <input className={inputCls} placeholder="Address" {...register("address")} />
      </div>
    </section>
  );
}
