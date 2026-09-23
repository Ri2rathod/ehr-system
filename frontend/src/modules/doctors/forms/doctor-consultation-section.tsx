import { UseFormRegister } from "react-hook-form";
import { DoctorFormValues } from "../schemas/doctor.schema";

const inputCls = "h-9 w-full rounded-md border border-outline-variant bg-surface px-3 text-sm outline-none focus:border-primary";

export function DoctorConsultationSection({ register }: { register: UseFormRegister<DoctorFormValues> }) {
  return (
    <section className="rounded-lg border border-outline-variant bg-surface-container-lowest p-4">
      <h2 className="mb-3 text-sm font-bold">Status</h2>
      <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
        <select className={inputCls} {...register("status")}>
          <option value="ACTIVE">ACTIVE</option>
          <option value="ON_LEAVE">ON_LEAVE</option>
          <option value="INACTIVE">INACTIVE</option>
          <option value="SUSPENDED">SUSPENDED</option>
        </select>
      </div>
    </section>
  );
}
