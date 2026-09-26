package com.example.ehrsystem.modules.encounter.service;

import com.example.ehrsystem.modules.encounter.entity.EncounterSequence;
import com.example.ehrsystem.modules.encounter.repository.EncounterSequenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;

@Service
@RequiredArgsConstructor
public class EncounterNumberService {

    private static final String ENCOUNTER_NUMBER_FORMAT = "ENC-%d-%06d";
    private final EncounterSequenceRepository encounterSequenceRepository;

    @Transactional
    public String generateEncounterNumber() {
        int currentYear = Year.now().getValue();

        EncounterSequence sequence = encounterSequenceRepository.findByYearWithLock(currentYear)
                .orElseGet(() -> {
                    EncounterSequence newSequence = EncounterSequence.builder()
                            .sequenceYear(currentYear)
                            .lastNumber(0L)
                            .build();
                    return encounterSequenceRepository.save(newSequence);
                });

        long nextNumber = sequence.getLastNumber() + 1;
        sequence.setLastNumber(nextNumber);
        encounterSequenceRepository.save(sequence);

        return String.format(ENCOUNTER_NUMBER_FORMAT, currentYear, nextNumber);
    }
}
