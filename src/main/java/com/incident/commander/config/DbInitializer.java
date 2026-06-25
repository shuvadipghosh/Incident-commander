package com.incident.commander.config;

import com.incident.commander.domain.Policy;
import com.incident.commander.repository.PolicyRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DbInitializer implements CommandLineRunner {

    private final PolicyRepository policyRepository;

    public DbInitializer(PolicyRepository policyRepository) {
        this.policyRepository = policyRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (policyRepository.count() == 0) {
            policyRepository.saveAll(List.of(
                Policy.builder()
                    .phoneNumber(1234567890L)
                    .name("John Doe")
                    .policyNumber("POL-987654")
                    .coverage(List.of("FUEL_DELIVERY", "TOW_TRUCK", "FLAT_TYRE"))
                    .insured(true)
                    .build(),
                Policy.builder()
                    .phoneNumber(9876543210L)
                    .name("Jane Smith")
                    .policyNumber("POL-123456")
                    .coverage(List.of("FUEL_DELIVERY"))
                    .insured(true)
                    .build(),
                Policy.builder()
                    .phoneNumber(5551234567L)
                    .name("Bob Johnson")
                    .policyNumber("POL-555000")
                    .coverage(List.of("FUEL_DELIVERY", "TOW_TRUCK", "FLAT_TYRE", "DEAD_BATTERY", "MEDICAL_EMERGENCY"))
                    .insured(true)
                    .build()
            ));
        }
    }
}
