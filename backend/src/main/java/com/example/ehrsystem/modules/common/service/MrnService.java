package com.example.ehrsystem.modules.common.service;

import com.example.ehrsystem.modules.common.entity.MrnSequence;
import com.example.ehrsystem.modules.common.repository.MrnSequenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;

@Service
@RequiredArgsConstructor
public class MrnService {

    private static final String MRN_PREFIX = "MRN-%d-%06d";
    private final MrnSequenceRepository mrnSequenceRepository;

    @Transactional
    public String generateMrn() {
        int currentYear = Year.now().getValue();

        MrnSequence sequence = mrnSequenceRepository.findByYearWithLock(currentYear)
                .orElseGet(() -> {
                    MrnSequence newSequence = MrnSequence.builder()
                            .sequenceYear(currentYear)
                            .lastNumber(0L)
                            .build();
                    return mrnSequenceRepository.save(newSequence);
                });

        long nextNumber = sequence.getLastNumber() + 1;
        sequence.setLastNumber(nextNumber);
        mrnSequenceRepository.save(sequence);

        return String.format(MRN_PREFIX, currentYear, nextNumber);
    }
}