package com.example.ehrsystem.modules.appointment.service;

import com.example.ehrsystem.modules.appointment.entity.AppointmentSequence;
import com.example.ehrsystem.modules.appointment.repository.AppointmentSequenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;

@Service
@RequiredArgsConstructor
public class AppointmentNumberService {

    private static final String APPOINTMENT_NUMBER_FORMAT = "APT-%d-%06d";
    private final AppointmentSequenceRepository appointmentSequenceRepository;

    @Transactional
    public String generateAppointmentNumber() {
        int currentYear = Year.now().getValue();

        AppointmentSequence sequence = appointmentSequenceRepository.findByYearWithLock(currentYear)
                .orElseGet(() -> {
                    AppointmentSequence newSequence = AppointmentSequence.builder()
                            .sequenceYear(currentYear)
                            .lastNumber(0L)
                            .build();
                    return appointmentSequenceRepository.save(newSequence);
                });

        long nextNumber = sequence.getLastNumber() + 1;
        sequence.setLastNumber(nextNumber);
        appointmentSequenceRepository.save(sequence);

        return String.format(APPOINTMENT_NUMBER_FORMAT, currentYear, nextNumber);
    }
}
