package com.incident.commander.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class PolicyDTO {
    private String name;
    private String policyNumber;
    private List<String> coverage;
    private boolean insured;
}
