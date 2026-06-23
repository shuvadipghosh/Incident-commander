package com.incident.commander.service;

import com.incident.commander.dto.PolicyDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PolicyService {

    // In-memory mock policy store — replace with DB in production
    private static final Map<Long, PolicyDTO> POLICY_STORE = Map.of(
            1234567890L, PolicyDTO.builder()
                    .name("John Doe")
                    .policyNumber("POL-987654")
                    .coverage(List.of("FUEL_DELIVERY", "TOW_TRUCK", "FLAT_TYRE"))
                    .insured(true)
                    .build(),
            9876543210L, PolicyDTO.builder()
                    .name("Jane Smith")
                    .policyNumber("POL-123456")
                    .coverage(List.of("FUEL_DELIVERY"))
                    .insured(true)
                    .build(),
            5551234567L, PolicyDTO.builder()
                    .name("Bob Johnson")
                    .policyNumber("POL-555000")
                    .coverage(List.of("FUEL_DELIVERY", "TOW_TRUCK", "FLAT_TYRE", "DEAD_BATTERY", "MEDICAL_EMERGENCY"))
                    .insured(true)
                    .build()
    );

    public Optional<PolicyDTO> findByPhoneNumber(long phoneNumber) {
        return Optional.ofNullable(POLICY_STORE.get(phoneNumber));
    }

    public boolean hasCoverage(long phoneNumber, String coverageType) {
        return findByPhoneNumber(phoneNumber)
                .map(p -> p.getCoverage().contains(coverageType))
                .orElse(false);
    }
}
