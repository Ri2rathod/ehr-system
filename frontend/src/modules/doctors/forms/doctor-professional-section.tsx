import { UseFormRegister } from "react-hook-form";
import { DoctorFormValues } from "../schemas/doctor.schema";

const inputCls = "h-9 w-full rounded-md border border-outline-variant bg-surface px-3 text-sm outline-none focus:border-primary";

export function DoctorProfessionalSection({ register }: { register: UseFormRegister<DoctorFormValues> }) {
  return (
    <section className="rounded-lg border border-outline-variant bg-surface-container-lowest p-4">
      <h2 className="mb-3 text-sm font-bold">Professional Information</h2>
      <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
        <input className={inputCls} placeholder="Specialization" {...register("specialization")} />
        <input className={inputCls} placeholder="Department" {...register("department")} />
        <input className={inputCls} placeholder="License Number" {...register("licenseNumber")} />
        <input className={inputCls} type="number" placeholder="Experience (Years)" {...register("experienceYears", { valueAsNumber: true })} />
        <input className={inputCls} placeholder="Qualification" {...register("qualification")} />
        <input className={inputCls} type="number" placeholder="Consultation Fee" {...register("consultationFee", { valueAsNumber: true })} />
      </div>
    </section>
  );
}
