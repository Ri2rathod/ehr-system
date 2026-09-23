"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { doctorSchema, DoctorFormValues } from "../schemas/doctor.schema";
import { DoctorBasicSection } from "./doctor-basic-section";
import { DoctorContactSection } from "./doctor-contact-section";
import { DoctorProfessionalSection } from "./doctor-professional-section";
import { DoctorScheduleSection } from "./doctor-schedule-section";
import { DoctorConsultationSection } from "./doctor-consultation-section";
import { useCreateDoctor } from "../hooks/use-create-doctor";
import { useUpdateDoctor } from "../hooks/use-update-doctor";

export function DoctorForm({ initial, uuid }: { initial?: Partial<DoctorFormValues>; uuid?: string }) {
  const router = useRouter();
  const createDoctor = useCreateDoctor();
  const updateDoctor = useUpdateDoctor();

  const { register, handleSubmit, formState: { errors, isDirty } } = useForm<DoctorFormValues>({
    resolver: zodResolver(doctorSchema),
    defaultValues: {
      status: "ACTIVE",
      consultationFee: 0,
      ...initial,
    },
  });

  const onSubmit = async (values: DoctorFormValues) => {
    if (uuid) {
      await updateDoctor.mutateAsync({ uuid, payload: values });
      return;
    }
    const created = await createDoctor.mutateAsync(values);
    router.push(`/doctors/${created?.uuid || created?.id || ""}`);
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4 pb-20">
      <DoctorBasicSection register={register} errors={errors} />
      <DoctorContactSection register={register} />
      <DoctorProfessionalSection register={register} />
      <DoctorScheduleSection register={register} />
      <DoctorConsultationSection register={register} />

      <div className="fixed bottom-0 left-0 right-0 z-20 border-t border-outline-variant bg-surface-container-lowest/95 p-3">
        <div className="mx-auto flex max-w-[1400px] justify-between">
          <button type="button" onClick={() => router.back()} className="h-9 rounded-md border border-outline-variant px-3 text-xs font-semibold">Cancel</button>
          <button type="submit" disabled={!isDirty || createDoctor.isPending || updateDoctor.isPending} className="h-9 rounded-md bg-primary px-3 text-xs font-bold text-on-primary disabled:opacity-50">
            {uuid ? "Save Changes" : "Create Doctor"}
          </button>
        </div>
      </div>
    </form>
  );
}
