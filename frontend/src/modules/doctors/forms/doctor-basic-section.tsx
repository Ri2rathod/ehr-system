import { UseFormRegister, FieldErrors } from "react-hook-form";
import { DoctorFormValues } from "../schemas/doctor.schema";

const inputCls = "h-9 w-full rounded-md border border-outline-variant bg-surface px-3 text-sm outline-none focus:border-primary";

export function DoctorBasicSection({ register, errors }: { register: UseFormRegister<DoctorFormValues>; errors: FieldErrors<DoctorFormValues> }) {
  return (
    <section className="rounded-lg border border-outline-variant bg-surface-container-lowest p-4">
      <h2 className="mb-3 text-sm font-bold">Basic Information</h2>
      <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
        <input className={inputCls} placeholder="First Name" {...register("firstName")} />
        <input className={inputCls} placeholder="Last Name" {...register("lastName")} />
        <input className={inputCls} placeholder="Display Name" {...register("displayName")} />
        <select className={inputCls} {...register("gender")}>
          <option value="">Gender</option>
          <option value="MALE">Male</option><option value="FEMALE">Female</option><option value="OTHER">Other</option>
        </select>
      </div>
      {errors.firstName?.message ? <p className="mt-2 text-xs text-error">{errors.firstName.message}</p> : null}
    </section>
  );
}
