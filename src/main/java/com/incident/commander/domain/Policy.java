package com.incident.commander.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "policies", indexes = {
    @Index(name = "idx_policy_phone_number", columnList = "phoneNumber", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long phoneNumber;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String policyNumber;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "policy_coverages", joinColumns = @JoinColumn(name = "policy_id"))
    @Column(name = "coverage")
    private List<String> coverage;

    @Column(nullable = false)
    private boolean insured;
}
