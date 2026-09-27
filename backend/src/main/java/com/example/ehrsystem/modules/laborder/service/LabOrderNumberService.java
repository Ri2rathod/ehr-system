package com.example.ehrsystem.modules.laborder.service;

import com.example.ehrsystem.modules.laborder.entity.LabOrderSequence;
import com.example.ehrsystem.modules.laborder.repository.LabOrderSequenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;

/**
 * Concurrency-safe lab order number generation: LAB-YYYY-NNNNNN
 * (row-locked yearly sequence table, same architecture as MRN/DOC/APP/ENC/PRES).
 */
@Service
@RequiredArgsConstructor
public class LabOrderNumberService {

    private static final String LAB_ORDER_NUMBER_FORMAT = "LAB-%d-%06d";

    private final LabOrderSequenceRepository labOrderSequenceRepository;

    @Transactional
    public String generateLabOrderNumber() {
        int currentYear = Year.now().getValue();

        LabOrderSequence sequence = labOrderSequenceRepository.findByYearWithLock(currentYear)
                .orElseGet(() -> {
                    LabOrderSequence newSequence = LabOrderSequence.builder()
                            .sequenceYear(currentYear)
                            .lastNumber(0L)
                            .build();
                    return labOrderSequenceRepository.save(newSequence);
                });

        long nextNumber = sequence.getLastNumber() + 1;
        sequence.setLastNumber(nextNumber);
        labOrderSequenceRepository.save(sequence);

        return String.format(LAB_ORDER_NUMBER_FORMAT, currentYear, nextNumber);
    }
}
