package com.incident.commander.service;

import com.incident.commander.dto.PolicyDTO;
import com.incident.commander.domain.Policy;
import com.incident.commander.repository.PolicyRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PolicyService {

    private final PolicyRepository policyRepository;

    public PolicyService(PolicyRepository policyRepository) {
        this.policyRepository = policyRepository;
    }

    public Optional<PolicyDTO> findByPhoneNumber(long phoneNumber) {
        return policyRepository.findByPhoneNumber(phoneNumber)
                .map(p -> PolicyDTO.builder()
                        .name(p.getName())
                        .policyNumber(p.getPolicyNumber())
                        .coverage(p.getCoverage())
                        .insured(p.isInsured())
                        .build());
    }

    public boolean hasCoverage(long phoneNumber, String coverageType) {
        return findByPhoneNumber(phoneNumber)
                .map(p -> p.getCoverage().contains(coverageType))
                .orElse(false);
    }
}
