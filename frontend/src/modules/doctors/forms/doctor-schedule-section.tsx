import { UseFormRegister } from "react-hook-form";
import { DoctorFormValues } from "../schemas/doctor.schema";

const inputCls = "h-9 w-full rounded-md border border-outline-variant bg-surface px-3 text-sm outline-none focus:border-primary";

export function DoctorScheduleSection({ register }: { register: UseFormRegister<DoctorFormValues> }) {
  return (
    <section className="rounded-lg border border-outline-variant bg-surface-container-lowest p-4">
      <h2 className="mb-3 text-sm font-bold">Schedule</h2>
      <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
        <input className={inputCls} placeholder="Available Days (comma separated)" {...register("consultationHours")} />
        <input className={inputCls} placeholder="Consultation Hours" {...register("consultationHours")} />
        <input className={inputCls} placeholder="Break Times" {...register("breakTimes")} />
        <label className="flex items-center gap-2 text-sm"><input type="checkbox" {...register("onlineAvailable")} /> Online Available</label>
      </div>
    </section>
  );
}
