package com.example.ehrsystem.modules.doctor.service;

import com.example.ehrsystem.modules.doctor.entity.DoctorSequence;
import com.example.ehrsystem.modules.doctor.repository.DoctorSequenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;

@Service
@RequiredArgsConstructor
public class DoctorCodeService {

    private static final String DOCTOR_CODE_PREFIX = "DOC-%d-%06d";
    private final DoctorSequenceRepository doctorSequenceRepository;

    @Transactional
    public String generateDoctorCode() {
        int currentYear = Year.now().getValue();

        DoctorSequence sequence = doctorSequenceRepository.findByYearWithLock(currentYear)
                .orElseGet(() -> {
                    DoctorSequence newSequence = DoctorSequence.builder()
                            .sequenceYear(currentYear)
                            .lastNumber(0L)
                            .build();
                    return doctorSequenceRepository.save(newSequence);
                });

        long nextNumber = sequence.getLastNumber() + 1;
        sequence.setLastNumber(nextNumber);
        doctorSequenceRepository.save(sequence);

        return String.format(DOCTOR_CODE_PREFIX, currentYear, nextNumber);
    }
}