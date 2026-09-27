package com.example.ehrsystem.modules.prescription.service;

import com.example.ehrsystem.modules.prescription.entity.PrescriptionSequence;
import com.example.ehrsystem.modules.prescription.repository.PrescriptionSequenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;

/**
 * Concurrency-safe prescription number generation: PRES-YYYY-NNNNNN
 * (row-locked yearly sequence table, same architecture as MRN/DOC/APP/ENC).
 */
@Service
@RequiredArgsConstructor
public class PrescriptionNumberService {

    private static final String PRESCRIPTION_NUMBER_FORMAT = "PRES-%d-%06d";

    private final PrescriptionSequenceRepository prescriptionSequenceRepository;

    @Transactional
    public String generatePrescriptionNumber() {
        int currentYear = Year.now().getValue();

        PrescriptionSequence sequence = prescriptionSequenceRepository.findByYearWithLock(currentYear)
                .orElseGet(() -> {
                    PrescriptionSequence newSequence = PrescriptionSequence.builder()
                            .sequenceYear(currentYear)
                            .lastNumber(0L)
                            .build();
                    return prescriptionSequenceRepository.save(newSequence);
                });

        long nextNumber = sequence.getLastNumber() + 1;
        sequence.setLastNumber(nextNumber);
        prescriptionSequenceRepository.save(sequence);

        return String.format(PRESCRIPTION_NUMBER_FORMAT, currentYear, nextNumber);
    }
}
