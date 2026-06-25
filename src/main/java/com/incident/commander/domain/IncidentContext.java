package com.incident.commander.domain;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class IncidentContext {
    private IncidentType incidentType;
    private String description;
    private double latitude;
    private double longitude;
    private long phoneNumber;
    private String sessionId;
    
    private String policyNumber;
    private String policyHolder;
    private List<String> coverage;
    private boolean insured;
}
